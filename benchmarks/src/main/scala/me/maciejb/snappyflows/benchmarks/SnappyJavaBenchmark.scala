package me.maciejb.snappyflows.benchmarks

import java.nio.file.{Path, Files, Paths}

import akka.actor.ActorSystem
import akka.stream.{Attributes, ActorMaterializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.maciejb.snappyflows.SnappyFlows
import me.maciejb.snappyflows.util.Chunking
import org.openjdk.jmh.annotations.{TearDown, Benchmark, Scope, State}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}
import org.openjdk.jmh.util.NullOutputStream
import org.xerial.snappy.SnappyFramedOutputStream

import scala.concurrent.Await
import scala.concurrent.duration._

@State(Scope.Benchmark)
class SnappyJavaBenchmark {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  val input = EColi.bytesX10
  val inputByteString = ByteString.fromArray(input)

  val compressionGraph =
    Source.single(inputByteString)
      .via(SnappyFlows.compress())
      .toMat(Sink.last)(Keep.right)

  val asyncCompressionGraph =
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
  def compressViaAsyncFlows() = {
    Await.ready(asyncCompressionGraph.run(), 1.second)
  }

  @Benchmark
  def chunk(): Unit = {
    Await.ready(asyncCompressionGraph.run(), 1.second)
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(system.terminate(), 10.seconds)
  }

}

object EColi {
  val bytes = {
    val cwd: Path = Paths.get("").toAbsolutePath

    val basePath =
      if (cwd.endsWith("benchmarks")) cwd
      else if (cwd.endsWith("snappy-flows")) cwd.resolve("benchmarks")
      else sys.error(s"what's your working directory, sir? $cwd")

    Files.readAllBytes(basePath.resolve("data/E.coli"))
  }

  lazy val bytesX10: Array[Byte] = (0 until 10).flatMap(_ => bytes).toArray
}

object SnappyJavaBenchmarkApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[SnappyJavaBenchmark].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .build
    new Runner(opt).run
  }

}
