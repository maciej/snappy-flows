package me.maciejb

import java.nio.ByteOrder

package object snappyflows {

  private[snappyflows] implicit val byteOrder = ByteOrder.LITTLE_ENDIAN

}
