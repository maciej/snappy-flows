package me.maciejb.snappyflows.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.{Keep, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}


class ChunkingTest extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit lazy val system = ActorSystem()
  implicit lazy val mat = ActorMaterializer()

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
