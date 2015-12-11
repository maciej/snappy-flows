# Snappy flows
[![Maven Central][maven-central-badge]][maven-central-link]
[![Build Status][travis-ci-badge]][travis-ci-link]

Naive compression and decompression `ByteString` [Akka Steams][akka-streams] `Flow`s implementation.

Uses Snappy's [framing format][snappy-framing].

## Getting started
In your `build.sbt`:
```scala
libraryDependencies += "me.maciejb.snappyflows" %% "snappy-flows" % "0.0.2"
```

## Usage
```scala
import me.maciejb.snappyflows.SnappyFlows

// To decompress your stream:
val sourceWithCompressedData: Source[ByteString] = ???
sourceWithCompressedData.via(SnappyFlows.decompress())

// To compress it
val sourceWithRawBytes: Source[ByteString] = ???
sourceWithRawBytes.via(SnappyFlows.compress())
```

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
