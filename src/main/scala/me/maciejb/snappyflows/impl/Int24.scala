package me.maciejb.snappyflows.impl

import org.apache.pekko.util.ByteString
import me.maciejb.snappyflows.impl.ByteStringParser.ByteReader


private[snappyflows] object Int24 {

  def readLE(r: ByteReader): Int = r.readByte() | r.readByte() << 8 | r.readByte() << 16
  def readLE(arr: Array[Byte]): Int = arr(0) | arr(1) << 8 | arr(2) << 16

  def writeLE(number: Int): ByteString = ByteString.apply(
    (number & 0xff).toByte,
    (number >> 8 & 0xff).toByte,
    (number >> 16 & 0xff).toByte
  )

}
