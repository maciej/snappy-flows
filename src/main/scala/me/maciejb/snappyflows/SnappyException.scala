package me.maciejb.snappyflows

import org.apache.pekko.util.ByteString


class SnappyException(desc: String, causeOpt: Option[Exception] = None)
  extends Exception(desc: String, causeOpt.orNull)

class InvalidChecksum(expected: Int, actual: Int)
  extends SnappyException(s"Invalid checksum for chunk. Expected: 0x${expected.toHexString}, " +
    s"actual 0x${actual.toHexString}.")

class InvalidHeader(header: ByteString) extends SnappyException(s"Invalid header: $header.")

class IllegalChunkFlag(flag: Int) extends SnappyException(s"Illegal chunk flag 0x${flag.toHexString}")