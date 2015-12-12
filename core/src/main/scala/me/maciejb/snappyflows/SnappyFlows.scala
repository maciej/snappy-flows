package me.maciejb.snappyflows


import akka.stream.io.ByteStringParser
import akka.stream.io.ByteStringParser.{ByteReader, ParseStep}
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import me.maciejb.snappyflows.util.{Chunking, Int24}
import org.xerial.snappy.{PureJavaCrc32C, Snappy}

import scala.concurrent.{ExecutionContext, Future}

object SnappyFlows {

  val MaxChunkSize = 65536
  val DefaultChunkSize = MaxChunkSize

  def decompress(verifyChecksums: Boolean = true): Flow[ByteString, ByteString, Unit] =
    Flow.fromGraph(new Decompressor(verifyChecksums))

  private class Decompressor(verifyChecksums: Boolean) extends ByteStringParser[ByteString] {
    override def createLogic(inheritedAttributes: Attributes) = new ParsingLogic {

      object HeaderParse extends ParseStep[ByteString] {
        override def parse(reader: ByteReader): (ByteString, ParseStep[ByteString]) = {
          val actualHeader = reader.take(SnappyFramed.Header.length)
          if (actualHeader == SnappyFramed.Header) (ByteString.empty, ChunkParser)
          else sys.error(s"Illegal header: $actualHeader.")
        }
      }

      object ChunkParser extends ParseStep[ByteString] {
        def checkChecksum(expected: Int, bytes: Array[Byte]) = if (verifyChecksums) {
          val actual = SnappyChecksum.checksum(bytes)
          if (actual != expected) throw new InvalidChecksum(expected, actual)
        }

        override def parse(reader: ByteReader) = {
          reader.readByte() match {
            case SnappyFramed.Flags.CompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val checksum = reader.readIntLE()
              val compressed = reader.take(segmentLength)
              val uncompressedBytes = Snappy.uncompress(compressed.toArray)
              val uncompressed = ByteString(uncompressedBytes)
              checkChecksum(checksum, uncompressedBytes)
              (uncompressed, ChunkParser)
            case SnappyFramed.Flags.UncompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val checksum = reader.readIntLE()
              val chunk = reader.take(segmentLength)
              checkChecksum(checksum, chunk.toArray)
              (chunk, ChunkParser)
            case flag => throw new IllegalChunkFlag(flag)
          }
        }
      }

      startWith(HeaderParse)
    }
  }

  def compress(chunkSize: Int = DefaultChunkSize): Flow[ByteString, ByteString, Unit] = {
    require(chunkSize <= MaxChunkSize, s"Chunk size $chunkSize exceeded max chunk size of $MaxChunkSize.")
    Flow.fromGraph(new Compressor(chunkSize))
  }

  def compressAsync(parallelism: Int, chunkSize: Int = DefaultChunkSize)
                   (implicit ec: ExecutionContext): Flow[ByteString, ByteString, Unit] = {
    val headerSource = Source.single(SnappyFramed.Header)
    val compressionFlow = Flow[ByteString]
      .via(Chunking.fixedSize(chunkSize))
      .mapAsync(parallelism) { chunk => Future(SnappyFramed.compressChunk(chunk)) }

    Flow
      .fromGraph(FlowGraph.create() { implicit b =>
        import FlowGraph.Implicits._
        val concat = b.add(Concat[ByteString](2))
        val flow = b.add(Flow[ByteString])

        headerSource ~> concat.in(0)
        flow.outlet ~> compressionFlow ~> concat.in(1)

        FlowShape(flow.inlet, concat.out)
      })
  }

  private class Compressor(chunkSize: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {
    private val in = Inlet[ByteString]("bytesIn")
    private val out = Outlet[ByteString]("bytesOut")

    override def shape = FlowShape(in, out)

    override def createLogic(inheritedAttributes: Attributes) = {
      new GraphStageLogic(shape) {

        var buffer = ByteString.empty

        override def preStart() = {
          emit(out, SnappyFramed.Header)
          pull(in)
        }

        setHandler(out, eagerTerminateOutput)
        setHandler(in, new InHandler {

          def tryCompressing() = {
            if (buffer.size >= chunkSize) {
              val (chunk, remaining) = buffer.splitAt(chunkSize)
              buffer = remaining
              compress(chunk)
            }
          }

          def compress(chunk: ByteString) = emit(out, SnappyFramed.compressChunk(chunk))

          override def onPush() = {
            buffer ++= grab(in)
            tryCompressing()
            pull(in)
          }

          override def onUpstreamFinish() = {
            if (buffer.nonEmpty) compress(buffer)
            completeStage()
          }

        })
      }
    }
  }

}

object SnappyFramed {
  object Flags {
    val StreamIdentifier = 0xff.toByte
    val UncompressedData = 0x01.toByte
    val CompressedData = 0x00.toByte
  }

  private val HeaderBytes =
    Array[Byte](Flags.StreamIdentifier, 0x06, 0x00, 0x00, 0x73, 0x4e, 0x61, 0x50, 0x70, 0x59)

  val Header = ByteString(HeaderBytes)

  def compressChunk(chunk: ByteString): ByteString = {
    val chunkBytes = chunk.toArray
    val compressed = Snappy.compress(chunkBytes)
    val checksum = SnappyChecksum.checksum(chunkBytes)
    val length = Int24.writeLE(compressed.length + 4)

    ByteString.newBuilder
      .putByte(SnappyFramed.Flags.CompressedData)
      .append(length)
      .putInt(checksum)
      .putBytes(compressed)
      .result()
  }

}

object SnappyChecksum {
  val MaskDelta = 0xa282ead8

  private[this] final val threadLocalCrc = new ThreadLocal[PureJavaCrc32C]() {
    override def initialValue() = new PureJavaCrc32C
  }

  def checksum(data: Array[Byte]): Int = {
    val crc32c = threadLocalCrc.get()
    crc32c.reset()
    crc32c.update(data, 0, data.length)
    val crc = crc32c.getIntegerValue
    ((crc >>> 15) | (crc << 17)) + MaskDelta
  }
}
