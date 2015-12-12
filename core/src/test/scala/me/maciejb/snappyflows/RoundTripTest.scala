package me.maciejb.snappyflows


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source, Sink}
import akka.testkit.{TestKit, TestKitBase}
import akka.util.ByteString
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}


class RoundTripTest extends FlatSpec with Matchers with TestKitBase with BeforeAndAfterAll
  with ScalaFutures with IntegrationPatience {

  override implicit lazy val system: ActorSystem = ActorSystem("RoundTripTest")
  implicit lazy val mat = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits._

  /* 1 MiB of data */
  val UncompressedData = {
    val arr = new Array[Byte](1024 * 1024)
    for (i <- arr.indices) arr.update(i, (i % 64).toByte)
    ByteString(arr)
  }

  "Compressing, then uncompromising" should "produce the same data, checksumed" in {
    val viaRoundTripFut = Source.single(UncompressedData).via(SnappyFlows.compressAsync(4)).via(SnappyFlows.decompress())
      .toMat(Sink.fold[ByteString, ByteString](ByteString.empty) {_ ++ _})(Keep.right).run()
    whenReady(viaRoundTripFut) { viaRoundTrip =>
      viaRoundTrip shouldEqual UncompressedData
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
