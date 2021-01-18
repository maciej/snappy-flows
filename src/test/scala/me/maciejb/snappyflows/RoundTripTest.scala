package me.maciejb.snappyflows


import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.{TestKit, TestKitBase}
import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class RoundTripTest extends AnyFlatSpec with Matchers with TestKitBase with BeforeAndAfterAll
  with ScalaFutures with IntegrationPatience {

  override implicit lazy val system: ActorSystem = ActorSystem("RoundTripTest")

  import scala.concurrent.ExecutionContext.Implicits._

  /* 1 MiB of data */
  val UncompressedData = {
    val arr = new Array[Byte](1024 * 1024)
    for (i <- arr.indices) arr.update(i, (i % 64).toByte)
    ByteString(arr)
  }

  val compressionFlows = Seq(
    "sync" -> SnappyFlows.compress(),
    "async(1)" -> SnappyFlows.compressAsync(1),
    "async(4)" -> SnappyFlows.compressAsync(4)
  )

  val decompressionFlows = Seq(
    "sync" -> SnappyFlows.decompress(),
    "async(1)" -> SnappyFlows.decompressAsync(1),
    "async(4)" -> SnappyFlows.decompressAsync(4)
  )

  for {
    (cFlowDesc, cFlow) <- compressionFlows
    (dFlowDesc, dFlow) <- decompressionFlows
  } {
    s"Compressing ($cFlowDesc), then uncompromising ($dFlowDesc)" should "produce the same data, checksumed" in {
      val viaRoundTripFut = Source
        .single(UncompressedData)
        .via(cFlow)
        .via(dFlow)
        .toMat(Sink.fold[ByteString, ByteString](ByteString.empty) {_ ++ _})(Keep.right)
        .run()

      whenReady(viaRoundTripFut) { viaRoundTrip =>
        viaRoundTrip shouldEqual UncompressedData
      }
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
