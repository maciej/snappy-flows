package me.maciejb.snappyflows

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._


object Snzip extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  val source = FileIO.fromFile(new File(args(0)), chunkSize = 65536)

  val sink = FileIO.toFile(new File(args(0) + ".sz"))

  Await.ready(
    source.via(SnappyFlows.compressAsync(4)).log("compress").toMat(sink)(Keep.right).run()
      .andThen { case _ => system.terminate() },
    1.minutes)

}
