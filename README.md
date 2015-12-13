# Snappy flows
[![Maven Central][maven-central-badge]][maven-central-link]
[![Build Status][travis-ci-badge]][travis-ci-link]

Snappy compression and decompression [Akka Steams][akka-streams] `Flow`s.

Uses Snappy's [framing format][snappy-framing].

## Getting started
In your `build.sbt`:
```scala
libraryDependencies += "me.maciejb.snappyflows" %% "snappy-flows" % "0.0.3"
```
Snappy flows are available only for Scala 2.11 and Akka Streams 2.0-M2.

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
`SnappyFramedInputStream` and `SnappyFramedOutputStream`. `async` versions provide very good speedup, though.
For details refer to [benchmarks/README.md](benchmarks/README.md).

## Resources
* [Reference Snappy implementation][google-snappy]
* [Snappy for Java][snappy-java]
* [Akka Steams documentation][akka-streams]
* [Akka project][akka]

[akka-streams]: http://doc.akka.io/docs/akka-stream-and-http-experimental/snapshot/scala.html
[snappy-framing]: https://github.com/google/snappy/blob/master/framing_format.txt
[google-snappy]: https://github.com/google/snappy
[snappy-java]: https://github.com/xerial/snappy-java
[akka]: http://akka.io
[maven-central-badge]: https://maven-badges.herokuapp.com/maven-central/me.maciejb.snappyflows/snappy-flows_2.11/badge.svg
[maven-central-link]: https://maven-badges.herokuapp.com/maven-central/me.maciejb.snappyflows/snappy-flows_2.11
[travis-ci-badge]: https://travis-ci.org/maciej/snappy-flows.svg
[travis-ci-link]: https://travis-ci.org/maciej/snappy-flows
