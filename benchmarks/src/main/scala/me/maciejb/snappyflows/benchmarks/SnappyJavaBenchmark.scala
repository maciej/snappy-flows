package me.maciejb.snappyflows.benchmarks

import java.nio.file.{Path, Files, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.maciejb.snappyflows.SnappyFlows
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

  val compressionGraph =
    Source.single(ByteString.fromArray(EColi.bytes))
      .via(SnappyFlows.compress())
      .toMat(Sink.last)(Keep.right)


  @Benchmark
  def compressEColiWithSnappyJava() = {
    val stream = new SnappyFramedOutputStream(new NullOutputStream)
    try {
      stream.write(EColi.bytes)
    } finally {
      stream.close()
    }
  }

  @Benchmark
  def compressEColiWithSnappyFlows() = {
    Await.ready(compressionGraph.run(), 1.second)
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
      else sys.error(s"what's your working directory, sir? ${cwd}")

    Files.readAllBytes(basePath.resolve("data/E.coli"))
  }
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
