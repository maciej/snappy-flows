package me.maciejb.snappyflows.benchmarks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.maciejb.snappyflows.SnappyFlows
import me.maciejb.snappyflows.benchmarks.data.EColi
import me.maciejb.snappyflows.impl.Chunking
import org.openjdk.jmh.annotations.{Benchmark, Scope, State, TearDown}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{Options, OptionsBuilder}
import org.openjdk.jmh.util.NullOutputStream
import org.xerial.snappy.SnappyFramedOutputStream

import scala.concurrent.Await
import scala.concurrent.duration._

@State(Scope.Benchmark)
class CompressionBenchmark {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  val input = EColi.bytesX10
  val inputByteString = ByteString.fromArray(input)

  val compressionGraph =
    Source.single(inputByteString)
      .via(SnappyFlows.compress())
      .toMat(Sink.last)(Keep.right)

  val asyncCompressionGraph4 =
    Source.single(inputByteString)
      .via(SnappyFlows.compressAsync(4))
      .toMat(Sink.ignore)(Keep.right)

  val asyncCompressionGraph2 =
    Source.single(inputByteString)
      .via(SnappyFlows.compressAsync(2))
      .toMat(Sink.ignore)(Keep.right)

  val chunkGraph =
    Source.single(inputByteString)
      .via(Chunking.fixedSize(SnappyFlows.DefaultChunkSize))
      .toMat(Sink.ignore)(Keep.right)

  @Benchmark
  def compressWithPlainJava() = {
    val stream = new SnappyFramedOutputStream(new NullOutputStream)
    try {
      stream.write(input)
    } finally {
      stream.close()
    }
  }

  @Benchmark
  def compressViaFlows() = {
    Await.ready(compressionGraph.run(), 1.second)
  }

  @Benchmark
  def compressViaAsyncFlows2() = {
    Await.ready(asyncCompressionGraph2.run(), 1.second)
  }

  @Benchmark
  def compressViaAsyncFlows4() = {
    Await.ready(asyncCompressionGraph4.run(), 1.second)
  }

  @Benchmark
  def chunk(): Unit = {
    Await.ready(chunkGraph.run(), 1.second)
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(system.terminate(), 10.seconds)
  }

}


object CompressionBenchmarkApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[CompressionBenchmark].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .build
    new Runner(opt).run
  }

}
