package me.maciejb.snappyflows.impl

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.concurrent.duration._

class ChunkingTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit lazy val system: ActorSystem = ActorSystem()

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(scaled(500.millis), scaled(15.millis))

  def byteStringSeq(strings: String*) = strings.map(ByteString.fromString).toList

  def chunk(size: Int, elems: List[ByteString]): Future[Seq[ByteString]] = {
    Source(elems)
      .via(Chunking.fixedSize(size))
      .grouped(1024)
      .toMat(Sink.head)(Keep.right).run()
  }

  it should "split a ByteString larger than the chunkSize into chunks" in {
    whenReady(chunk(3, byteStringSeq("foobar"))) {_ shouldEqual byteStringSeq("foo", "bar")}
  }

  it should "always emit the last element on upstream termination" in {
    whenReady(chunk(4, byteStringSeq("abc", "bcd"))) {_ shouldEqual byteStringSeq("abcb", "cd")}
  }

  it should "emit all small trailing chunks" in {
    whenReady(chunk(1, byteStringSeq("abcde"))) {_ shouldEqual byteStringSeq("a", "b", "c", "d", "e")}
  }

  it should "emit single element on completion even if smaller than chunk size" in {
    whenReady(chunk(42, byteStringSeq("abcd"))) {_ shouldEqual byteStringSeq("abcd")}
  }

  it should "complete successfully if input is empty" in {
    val fut = Source.empty.via(Chunking.fixedSize(42)).toMat(Sink.headOption)(Keep.right).run()
    whenReady(fut) {
      case _: Some[ByteString] => fail("Not expecting any elements here")
      case None => /* all fine */
    }
  }

  it should "fail if chunk size is 0" in {
    an[IllegalArgumentException] should be thrownBy {Chunking.fixedSize(0)}
  }

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
