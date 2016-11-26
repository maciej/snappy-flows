package me.maciejb.snappyflows

import java.nio.ByteOrder

package object impl {

  private[impl] implicit val byteOrder = ByteOrder.LITTLE_ENDIAN

}
