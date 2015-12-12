package me.maciejb.snappyflows.benchmarks

import java.nio.file.{Files, Paths}

import me.maciejb.snappyflows.benchmarks.data.EColiCompressed

object SaveEColiCompressedToFileApp {

  def main(args: Array[String]): Unit = {
    require(args.length > 0, "Please specify the destination path as the first argument.")
    Files.write(Paths.get(args(0)), EColiCompressed.bytesX10)
  }

}
