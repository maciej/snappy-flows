package me.maciejb.snappyflows

import akka.stream.Attributes
import akka.stream.io.ByteStringParser
import akka.stream.io.ByteStringParser.{ByteReader, ParseStep}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.xerial.snappy.Snappy

object SnappyFlows {

  def decodeFramed(verifyChecksums: Boolean = false): Flow[ByteString, ByteString, Unit] =
    Flow.fromGraph(new DecoderGraph(verifyChecksums))

  private class DecoderGraph(verifyChecksums: Boolean) extends ByteStringParser[ByteString] {
    override def createLogic(inheritedAttributes: Attributes) = new ParsingLogic {

      def readByteTripleLE(r: ByteReader): Int = {
        r.readByte() | r.readByte() << 8 | r.readByte() << 16
      }

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
              val segmentLength = readByteTripleLE(reader) - 4
              val uncompressed = checksumed(reader.readIntLE()) {
                val segment = reader.take(segmentLength)
                ByteString(Snappy.uncompress(segment.toArray))
              }
              (uncompressed, ChunkParser)
            case SnappyFramed.Flags.UncompressedData =>
              val segmentLength = readByteTripleLE(reader) - 4
              val chunk = checksumed(reader.readIntLE()) {reader.take(segmentLength)}
              (chunk, ChunkParser)
            case _ => sys.error("Illegal chunk flag")
          }
        }
      }

      startWith(HeaderParse)
    }
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
