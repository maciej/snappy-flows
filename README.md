# Snappy flows
[![Maven Central][maven-central-badge]][maven-central-link]
[![Build Status][travis-ci-badge]][travis-ci-link]

Snappy compression and decompression [Akka Steams][akka-streams] `Flow`s.

Uses Snappy's [framing format][snappy-framing].

## Getting started
In your `build.sbt`:
```scala
libraryDependencies += "me.maciejb.snappyflows" %% "snappy-flows" % "0.3.0"
```
Snappy flows are available for Scala 2.11, 2.12 and 2.13.
We use a recent Akka 2.6 branch version (2.6.10 as of snappy-flows 0.3.0). 

Previous releases:

| Snappy flows     | Akka                 | Scala |
|------------------|----------------------|--------
| `0.2.0 - 0.2.1`  | Akka >2.4.2          | 2.11  |
| `0.1.3 - 0.1.5`  | Akka >2.4.2          | 2.11  |
| `  0.1.2      `  | Akka 2.4.2-RC3       | 2.11  |
| `   0.1.1     `  | Akka Streams 2.0     | 2.11  |
| `<= 0.1.0     `  | Akka Streams 2.0-M2  | 2.11  |

## Usage
Sync and async versions of the flows are available:

```scala
import me.maciejb.snappyflows.SnappyFlows

val Parallelism = 4

// Let's take a source we want to decompress
val sourceWithCompressedData: Source[ByteString] = Source(...)

sourceWithCompressedData.via(SnappyFlows.decompress())
// or
sourceWithCompressedData.via(SnappyFlows.decompressAsync(Parallelism))

// Now, one we want to compress
val sourceWithRawBytes: Source[ByteString] = Source(...)

sourceWithRawBytes.via(SnappyFlows.compress())
// or
sourceWithRawBytes.via(SnappyFlows.compressAsync(Parallelism))
```

## Performance
Initial benchmarks show that the non-async Snappy Flows achieve ~90% of performance of
`SnappyFramedInputStream` and `SnappyFramedOutputStream`. `async` versions provide very good speedup giving 550-600MiB/s throughput on the test data on a a late 2013 15’ MBP with Intel(R) Core(TM) i7-4960HQ CPU @ 2.60GHz, 16GiB.
For details refer to [benchmarks/README.md](benchmarks/README.md).

## Resources
* [Reference Snappy implementation][google-snappy]
* [Snappy for Java][snappy-java]
* [Akka Streams documentation][akka-streams]
* [Akka project][akka]
* [Snappy flows at bintray][bintray-snappy-flows]

[akka-streams]: http://doc.akka.io/docs/akka-stream-and-http-experimental/snapshot/scala.html
[snappy-framing]: https://github.com/google/snappy/blob/master/framing_format.txt
[google-snappy]: https://github.com/google/snappy
[snappy-java]: https://github.com/xerial/snappy-java
[akka]: http://akka.io
[maven-central-badge]: https://maven-badges.herokuapp.com/maven-central/me.maciejb.snappyflows/snappy-flows_2.11/badge.svg
[maven-central-link]: https://maven-badges.herokuapp.com/maven-central/me.maciejb.snappyflows/snappy-flows_2.11
[travis-ci-badge]: https://travis-ci.org/maciej/snappy-flows.svg
[travis-ci-link]: https://travis-ci.org/maciej/snappy-flows
[bintray-snappy-flows]: https://bintray.com/maciej/maven/snappy-flows
