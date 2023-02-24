package me.maciejb.snappyflows.impl

import org.apache.pekko.util.ByteString
import me.maciejb.snappyflows.SnappyChecksum
import org.xerial.snappy.Snappy


private[snappyflows] object SnappyFramed {
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
