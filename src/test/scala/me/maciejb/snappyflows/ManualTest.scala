package me.maciejb.snappyflows

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._

object ManualTest extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val source = Source.inputStream(() => getClass.getClassLoader.getResourceAsStream("framing_format.txt.sz"))

  val sink = Sink.foreach[ByteString](bs => println(bs.utf8String))

  Await.ready(source.via(SnappyFlows.decodeFramed()).log("decoder").toMat(sink)(Keep.right).run(), 2.seconds)

  Await.ready(system.terminate(), 2.seconds)
}
