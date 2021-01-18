package me.maciejb.snappyflows

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, StreamConverters}
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._

object ManualTest extends App {
  implicit val system = ActorSystem()

  val source = StreamConverters.
    fromInputStream(() => getClass.getClassLoader.getResourceAsStream("framing_format.txt.sz"))

  val sink = Sink.foreach[ByteString](bs => println(bs.utf8String))

  Await.ready(source.via(SnappyFlows.decompress()).log("decoder").toMat(sink)(Keep.right).run(), 2.seconds)

  Await.ready(system.terminate(), 2.seconds)
}
