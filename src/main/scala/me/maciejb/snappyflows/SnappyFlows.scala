package me.maciejb.snappyflows

import java.nio.ByteOrder

import akka.stream.Attributes
import akka.stream.io.ByteStringParser
import akka.stream.io.ByteStringParser.{ByteReader, ParseStep}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.xerial.snappy.{PureJavaCrc32C, Snappy}

import scala.util.control.NoStackTrace

class InvalidChecksum(expected: Int, actual: Int) extends Exception

object SnappyFlows {

  private implicit val byteOrder = ByteOrder.LITTLE_ENDIAN

  val MaxChunkSize = 65536
  val DefaultChunkSize = MaxChunkSize

  def decompress(verifyChecksums: Boolean = false): Flow[ByteString, ByteString, Unit] =
    Flow.fromGraph(new Decompressor(verifyChecksums))

  private class Decompressor(verifyChecksums: Boolean) extends ByteStringParser[ByteString] {
    override def createLogic(inheritedAttributes: Attributes) = new ParsingLogic {

      val crc32c = new PureJavaCrc32C

      object HeaderParse extends ParseStep[ByteString] {
        override def parse(reader: ByteReader): (ByteString, ParseStep[ByteString]) = {
          val actualHeader = reader.take(SnappyFramed.Header.length)
          if (actualHeader == SnappyFramed.Header) (ByteString.empty, ChunkParser)
          else sys.error("Illegal header")
        }
      }

      object ChunkParser extends ParseStep[ByteString] {
        def withChecksumChecked(storedChecksum: Int)(chunk: => ByteString) = {
          if (verifyChecksums) {
            val actual = SnappyChecksum.checksum(chunk)
            if (actual == storedChecksum) chunk
            else throw new InvalidChecksum(storedChecksum, actual)
          } else {
            chunk
          }
        }

        override def parse(reader: ByteReader) = {
          reader.readByte() match {
            case SnappyFramed.Flags.CompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val uncompressed = withChecksumChecked(reader.readIntLE()) {
                val segment = reader.take(segmentLength)
                ByteString(Snappy.uncompress(segment.toArray))
              }
              (uncompressed, ChunkParser)
            case SnappyFramed.Flags.UncompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val chunk = withChecksumChecked(reader.readIntLE()) {reader.take(segmentLength)}
              (chunk, ChunkParser)
            case _ => sys.error("Illegal chunk flag")
          }
        }
      }

      startWith(HeaderParse)
    }
  }

  def compress(chunkSize: Int = 65536): Flow[ByteString, ByteString, Unit] = {
    require(chunkSize < MaxChunkSize, s"Chunk size $chunkSize exceeded max chunk size of $MaxChunkSize.")
    Flow.fromGraph(new Compressor(chunkSize))
  }

  private class Compressor(chunkSize: Int) extends ByteStringParser[ByteString] {
    override def createLogic(inheritedAttributes: Attributes) = new ParsingLogic {

      object HeaderWriter extends ParseStep[ByteString] {
        override def parse(reader: ByteReader) = (SnappyFramed.Header, ChunkParser)
      }

      object ChunkParser extends ParseStep[ByteString] {
        override def parse(reader: ByteReader) = {
          val chunk = try {
            reader.take(chunkSize)
          } catch {
            // :-)
            case e: Exception with NoStackTrace => reader.takeAll()
          }
          val compressed = Snappy.compress(chunk.toArray)
          val checksum = SnappyChecksum.checksum(chunk)
          val result = ByteString.newBuilder
            .append(Int24.writeLE(compressed.length))
            .putInt(checksum)
            .putBytes(compressed)
            .result()
          (result, ChunkParser)
        }
      }

      startWith(HeaderWriter)
    }
  }

}

object SnappyFramed {
  object Flags {
    val StreamIdentifier = 0xff
    val UncompressedData = 0x01
    val CompressedData = 0x00
  }

  private val HeaderBytes =
    Array[Byte](Flags.StreamIdentifier.toByte, 0x06, 0x00, 0x00, 0x73, 0x4e, 0x61, 0x50, 0x70, 0x59)

  val Header = ByteString(HeaderBytes)
}

object SnappyChecksum {
  val MaskDelta = 0xa282ead8

  def checksum(data: ByteString): Int = {
    val crc32c = new PureJavaCrc32C
    crc32c.update(data.toArray, 0, data.length)
    val crc = crc32c.getIntegerValue
    ((crc >>> 15) | (crc << 17)) + MaskDelta
  }
}
