package me.maciejb.snappyflows.impl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._


class ChunkingTest extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit lazy val system = ActorSystem()
  implicit lazy val mat = ActorMaterializer()

  override implicit val patienceConfig = PatienceConfig(scaled(300.millis), scaled(15.millis))

  it should "split a ByteString larger than the chunkSize into chunks" in {
    val fut = Source.single(ByteString.fromString("foobar"))
      .via(Chunking.fixedSize(3))
      .grouped(10)
      .toMat(Sink.head)(Keep.right).run()

    whenReady(fut) { chunks =>
      chunks shouldEqual Seq(
        ByteString.fromString("foo"),
        ByteString.fromString("bar")
      )
    }
  }

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
