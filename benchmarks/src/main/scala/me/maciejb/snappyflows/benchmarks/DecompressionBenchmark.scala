package me.maciejb.snappyflows.benchmarks

import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.maciejb.snappyflows.SnappyFlows
import me.maciejb.snappyflows.benchmarks.data.{EColiCompressed, EColi}
import org.openjdk.jmh.annotations.{Scope, State, Benchmark, TearDown}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}
import org.openjdk.jmh.util.NullOutputStream
import org.xerial.snappy.{SnappyFramedInputStream, SnappyFramedOutputStream}

import scala.concurrent.Await
import scala.concurrent.duration._

@State(Scope.Benchmark)
class DecompressionBenchmark {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  val input = EColiCompressed.bytesX10
  val inputByteString = ByteString.fromArray(input)

  val decompressionGraph =
    Source.single(inputByteString)
      .via(SnappyFlows.decompress())
      .toMat(Sink.last)(Keep.right)

  val asyncDecompressionGraph4 =
    Source.single(inputByteString)
      .via(SnappyFlows.decompressAsync(4))
      .toMat(Sink.ignore)(Keep.right)

  val asyncDecompressionGraph2 =
    Source.single(inputByteString)
      .via(SnappyFlows.decompressAsync(2))
      .toMat(Sink.ignore)(Keep.right)

  @Benchmark
  def decompressWithPlainJava() = {
    val stream = new SnappyFramedInputStream(new ByteArrayInputStream(input))
    try {
      stream.transferTo(new NullOutputStream)
    } finally {
      stream.close()
    }
  }

  @Benchmark
  def decompressViaFlows() = {
    Await.ready(decompressionGraph.run(), 1.second)
  }

  @Benchmark
  def decompressViaAsyncFlows2() = {
    Await.ready(asyncDecompressionGraph2.run(), 1.second)
  }

  @Benchmark
  def decompressViaAsyncFlows4() = {
    Await.ready(asyncDecompressionGraph4.run(), 1.second)
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(system.terminate(), 10.seconds)
  }
}

object DecompressionBenchmarkApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[DecompressionBenchmark].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .build
    new Runner(opt).run
  }

}
