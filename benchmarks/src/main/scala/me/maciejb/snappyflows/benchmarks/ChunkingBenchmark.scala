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

import scala.concurrent.Await
import scala.concurrent.duration._

@State(Scope.Benchmark)
class ChunkingBenchmark {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  val input = EColi.bytesX10

  val chunkingGraph =
    Source.single(ByteString.fromArray(input))
      .via(Chunking.fixedSize(SnappyFlows.DefaultChunkSize))
      .toMat(Sink.last)(Keep.right)

  @Benchmark
  def compressViaFlows() = {
    Await.ready(chunkingGraph.run(), 1.second)
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(system.terminate(), 10.seconds)
  }

}

object ChunkingBenchmarkApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[ChunkingBenchmark].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .build
    new Runner(opt).run
  }

}
