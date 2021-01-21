package me.maciejb.snappyflows

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Keep}

import java.nio.file.Paths
import scala.concurrent.Await
import scala.concurrent.duration._


object Snzip extends App {

  implicit val system = ActorSystem()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  val source = FileIO.fromPath(Paths.get(args(0)), chunkSize = 65536)

  val sink = FileIO.toPath(Paths.get(args(0) + ".sz"))

  Await.ready(
    source.via(SnappyFlows.compressAsync(4)).log("compress").toMat(sink)(Keep.right).run()
      .andThen { case _ => system.terminate() },
    1.minutes)

}
