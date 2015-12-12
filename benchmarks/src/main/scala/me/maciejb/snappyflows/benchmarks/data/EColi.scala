package me.maciejb.snappyflows.benchmarks.data

import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.maciejb.snappyflows.SnappyFlows

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object EColi {
  val bytes = {
    val cwd: Path = Paths.get("").toAbsolutePath

    val basePath =
      if (cwd.endsWith("benchmarks")) cwd
      else if (cwd.endsWith("snappy-flows")) cwd.resolve("benchmarks")
      else sys.error(s"what's your working directory, sir? $cwd")

    Files.readAllBytes(basePath.resolve("data/E.coli"))
  }

  lazy val bytesX10: Array[Byte] = (0 until 10).flatMap(_ => bytes).toArray
}

object EColiCompressed {

  lazy val bytesX10: Array[Byte] = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    val byteStringFut = Source
      .single(ByteString(EColi.bytesX10))
      .via(SnappyFlows.compressAsync(1))
      .toMat(Sink.fold(ByteString.empty)(_ ++ _))(Keep.right)
      .run()

    byteStringFut.onComplete { case _ =>
      system.terminate()
    }

    Await.result(byteStringFut, 5.seconds).toArray
  }

}
