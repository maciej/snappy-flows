package me.maciejb.snappyflows


import org.apache.pekko.NotUsed
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.util.ByteString
import me.maciejb.snappyflows.impl.Chunks.{CompressedData, UncompressedData}
import me.maciejb.snappyflows.impl._
import org.xerial.snappy.{PureJavaCrc32C, Snappy}

import scala.concurrent.{ExecutionContext, Future}

object SnappyFlows {

  val MaxChunkSize = 65536
  val DefaultChunkSize = MaxChunkSize

  def decompress(verifyChecksums: Boolean = true): Flow[ByteString, ByteString, NotUsed] = {
    SnappyChunk.decodingFlow.map {
      case UncompressedData(data, checksum) =>
        val dataBytes = data.toArray
        if (verifyChecksums) SnappyChecksum.verifyChecksum(dataBytes, checksum)
        data
      case CompressedData(data, checksum) =>
        val uncompressedData = Snappy.uncompress(data.toArray)
        if (verifyChecksums) SnappyChecksum.verifyChecksum(uncompressedData, checksum)
        ByteString.fromArray(uncompressedData)
    }
  }

  def decompressAsync(parallelism: Int, verifyChecksums: Boolean = true)
                     (implicit ec: ExecutionContext): Flow[ByteString, ByteString, NotUsed] = {
    SnappyChunk.decodingFlow.mapAsync(parallelism) {
      case UncompressedData(data, checksum) =>
        Future {
          val dataBytes = data.toArray
          if (verifyChecksums) SnappyChecksum.verifyChecksum(dataBytes, checksum)
          data
        }
      case CompressedData(data, checksum) =>
        Future {
          val uncompressedData = Snappy.uncompress(data.toArray)
          if (verifyChecksums) SnappyChecksum.verifyChecksum(uncompressedData, checksum)
          ByteString.fromArray(uncompressedData)
        }
    }
  }

  private[this] def compressWithFlow(chunkSize: Int,
                                     compressionFlow: Flow[ByteString, ByteString, NotUsed]) = {
    require(chunkSize <= MaxChunkSize, s"Chunk size $chunkSize exceeds maximum chunk size of $MaxChunkSize.")

    val headerSource = Source.single(SnappyFramed.Header)
    val chunkingAndCompression = Flow[ByteString].via(Chunking.fixedSize(chunkSize)).via(compressionFlow)

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val concat = b.add(Concat[ByteString](2))
      val flow = b.add(Flow[ByteString])

      headerSource ~> concat.in(0)
      flow.outlet ~> chunkingAndCompression ~> concat.in(1)

      FlowShape(flow.in, concat.out)
    })
  }

  def compress(chunkSize: Int = DefaultChunkSize): Flow[ByteString, ByteString, NotUsed] = {
    compressWithFlow(chunkSize, Flow[ByteString].map(SnappyFramed.compressChunk))
  }

  def compressAsync(parallelism: Int, chunkSize: Int = DefaultChunkSize)
                   (implicit ec: ExecutionContext): Flow[ByteString, ByteString, NotUsed] = {
    compressWithFlow(chunkSize,
      Flow[ByteString].mapAsync(parallelism) { chunk => Future(SnappyFramed.compressChunk(chunk)) }
    )
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

  def verifyChecksum(data: Array[Byte], expectedChecksum: Int) = {
    val actual = checksum(data)
    if (actual != expectedChecksum) throw new InvalidChecksum(expectedChecksum, actual)
  }

}
