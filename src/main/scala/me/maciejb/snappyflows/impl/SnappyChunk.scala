package me.maciejb.snappyflows.impl

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import me.maciejb.snappyflows.impl.Chunks._
import me.maciejb.snappyflows.{InvalidHeader, IllegalChunkFlag}
import me.maciejb.snappyflows.impl.ByteStringParser.{ParseResult, ByteReader, ParseStep}


private[snappyflows] object Chunks {
  sealed trait SnappyChunk
  case class CompressedData(data: ByteString, checksum: Int) extends SnappyChunk
  case class UncompressedData(data: ByteString, checksum: Int) extends SnappyChunk
}

private[snappyflows] object SnappyChunk {
  def decodingFlow: Flow[ByteString, SnappyChunk, NotUsed] = Flow.fromGraph(new Decoder)

  private class Decoder extends ByteStringParser[SnappyChunk] {
    override def createLogic(inheritedAttributes: Attributes) = new ParsingLogic {

      object HeaderParse extends ParseStep[SnappyChunk] {
        override def parse(reader: ByteReader) = {
          val header = reader.take(SnappyFramed.Header.length)

          if (header == SnappyFramed.Header) ParseResult(None, ChunkParser)
          else throw new InvalidHeader(header)
        }
      }

      object ChunkParser extends ParseStep[SnappyChunk] {

        override def parse(reader: ByteReader) = {
          reader.readByte() match {
            case SnappyFramed.Flags.CompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val checksum = reader.readIntLE()
              val data = reader.take(segmentLength)
              ParseResult(Some(CompressedData(data, checksum)), ChunkParser)
            case SnappyFramed.Flags.UncompressedData =>
              val segmentLength = Int24.readLE(reader) - 4
              val checksum = reader.readIntLE()
              val data = reader.take(segmentLength)
              ParseResult(Some(UncompressedData(data, checksum)), ChunkParser)
            case flag => throw new IllegalChunkFlag(flag)
          }
        }
      }

      startWith(HeaderParse)
    }
  }
}
