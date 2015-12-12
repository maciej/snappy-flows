package me.maciejb.snappyflows.benchmarks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.maciejb.snappyflows.SnappyFlows
import me.maciejb.snappyflows.benchmarks.data.EColi

import scala.concurrent.Await
import scala.concurrent.duration._

object CompressApp {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val compressionGraph =
      Source.single(ByteString.fromArray(EColi.bytes))
        .via(SnappyFlows.compress())
        .toMat(Sink.last)(Keep.right)

    for (_ <- 0 until 100) {
      Await.ready(compressionGraph.run(), 1.second)
    }

    Await.ready(system.terminate(), 1.second)
  }

}
