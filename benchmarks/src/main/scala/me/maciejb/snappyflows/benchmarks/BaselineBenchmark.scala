package me.maciejb.snappyflows.benchmarks

import org.openjdk.jmh.annotations.{Benchmark, Scope, State}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}
import org.openjdk.jmh.util.NullOutputStream
import org.xerial.snappy.SnappyFramedOutputStream


@State(Scope.Benchmark)
class BaselineBenchmark {

  @Benchmark
  def constructASnappyOutputStream() = {
    val stream = new SnappyFramedOutputStream(new NullOutputStream)
    stream.close()
  }

}

object BaselineBenchmarkApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[BaselineBenchmark].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .build
    new Runner(opt).run
  }

}
