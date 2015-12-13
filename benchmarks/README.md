# Benchmarks

Preliminary benchmarks show that the slowdown is not that bad as I expected. When compressing
the E.coli file from the [Canterbury Corpus][canterbury-corpus] Snappy Flows reaches around 90%
performance of `SnappyFramedOutputStream` with a `NullOutputStream`.

All benchmarks were run on a late 2013 15’ MBP, Intel(R) Core(TM) i7-4960HQ CPU @ 2.60GHz, 16GiB.

```
SnappyJavaBenchmark.compressEColiWithSnappyFlows  thrpt   20  36.933 ± 0.397  ops/s
SnappyJavaBenchmark.compressEColiWithSnappyJava   thrpt   20  40.690 ± 0.929  ops/s
```

## Compression

Async compression is fast. Even tough the compression itself could be optimized I get
much better performance than when using a `SnappyFramedOutputStream`:
```
CompressionBenchmark.chunk                   thrpt   20  10998.012 ± 79.410  ops/s
CompressionBenchmark.compressWithPlainJava   thrpt   20      3.950 ±  0.124  ops/s
CompressionBenchmark.compressViaFlows        thrpt   20      3.669 ±  0.044  ops/s
CompressionBenchmark.compressViaAsyncFlows2  thrpt   20      6.957 ±  0.050  ops/s
CompressionBenchmark.compressViaAsyncFlows4  thrpt   20     11.768 ±  0.161  ops/s
```
compressing the the E.coli file concatenated 10x (~46MiB).

The `async(4)` version has a throughput of ~550MiB/s.

## Decompression
Single-threaded compression results in ~95% of `SnappyFramedInputStream`.
Async decompression can give a nice speedup, though:
```
DecompressionBenchmark.decompressWithPlainJava   thrpt   20   9.415 ± 0.179  ops/s
DecompressionBenchmark.decompressViaFlows        thrpt   20   8.907 ± 0.145  ops/s
DecompressionBenchmark.decompressViaAsyncFlows2  thrpt   20  16.088 ± 0.145  ops/s
DecompressionBenchmark.decompressViaAsyncFlows4  thrpt   20  26.850 ± 0.442  ops/s
```
decompressing the compressed (block size 2^16) E.coli file concatenated 10x (~22MiB).

Decompression throughput of the `async(4)` version is ~600MiB/s.

[canterbury-corpus]: http://corpus.canterbury.ac.nz
