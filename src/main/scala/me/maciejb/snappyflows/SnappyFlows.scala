package me.maciejb.snappyflows

import java.nio.ByteOrder

import akka.stream.Attributes
import akka.stream.io.ByteStringParser
import akka.stream.io.ByteStringParser.{ByteReader, ParseStep}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.xerial.snappy.Snappy

import scala.util.control.NoStackTrace

object SnappyFlows {

  def decompress(verifyChecksums: Boolean = false): Flow[ByteString, ByteString, Unit] =
    Flow.fromGraph(new Decompressor(verifyChecksums))

  private class Decompressor(verifyChecksums: Boolean) extends ByteStringParser[ByteString] {
    override def createLogic(inheritedAttributes: Attributes) = new ParsingLogic {

      object HeaderParse extends ParseStep[ByteString] {
        override def parse(reader: ByteReader): (ByteString, ParseStep[ByteString]) = {
          val actualHeader = reader.take(SnappyFramed.Header.length)
          if (actualHeader == SnappyFramed.Header) (ByteString.empty, ChunkParser)
          else sys.error("Illegal header")
        }
      }

      object ChunkParser extends ParseStep[ByteString] {
        def checksumed(checksum: Int)(chunk: => ByteString) = {
          chunk
        }

        override def parse(reader: ByteReader) = {
          reader.readByte() match {
            case SnappyFramed.Flags.CompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val uncompressed = checksumed(reader.readIntLE()) {
                val segment = reader.take(segmentLength)
                ByteString(Snappy.uncompress(segment.toArray))
              }
              (uncompressed, ChunkParser)
            case SnappyFramed.Flags.UncompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val chunk = checksumed(reader.readIntLE()) {reader.take(segmentLength)}
              (chunk, ChunkParser)
            case _ => sys.error("Illegal chunk flag")
          }
        }
      }

      startWith(HeaderParse)
    }
  }

  def compress(chunkSize: Int = 65536): Flow[ByteString, ByteString, Unit] =
    Flow.fromGraph(new Compressor(chunkSize))

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
          val result = ByteString.newBuilder
            .append(Int24.writeLE(compressed.length))
            .putInt(0xBEEF)(ByteOrder.LITTLE_ENDIAN)
            .putBytes(compressed)
            .result()
          (result, ChunkParser)
        }
      }

      startWith(HeaderWriter)
    }
  }

}

private[snappyflows] object Int24 {
  def readLE(r: ByteReader): Int = {
    r.readByte() | r.readByte() << 8 | r.readByte() << 16
  }

  def writeLE(number: Int): ByteString = {
    ByteString.apply(
      (number & 0xff).toByte,
      (number >> 8 & 0xff).toByte,
      (number >> 16 & 0xff).toByte
    )
  }
}

object SnappyFramed {
  object Flags {
    val StreamIdentifier = 0xff
    val UncompressedData = 0x01
    val CompressedData = 0x00

  }

  private val HeaderBytes = Array[Byte](
    Flags.StreamIdentifier.toByte, 0x06, 0x00, 0x00, 0x73, 0x4e, 0x61, 0x50, 0x70, 0x59)

  val Header = ByteString(HeaderBytes)
}
