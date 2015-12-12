# Benchmarks

Preliminary benchmarks show that the slowdown is not that bad as I expected. When compressing
the E.coli file from the [Canterbury Corpus][canterbury-corpus] Snappy Flows reaches around 90%
performance of `SnappyFramedOutputStream` with a `NullOutputStream`.

All benchmarks were run on a late 2013 15’ MBP, Intel(R) Core(TM) i7-4960HQ CPU @ 2.60GHz, 16GiB.

```
SnappyJavaBenchmark.compressEColiWithSnappyFlows  thrpt   20  36.933 ± 0.397  ops/s
SnappyJavaBenchmark.compressEColiWithSnappyJava   thrpt   20  40.690 ± 0.929  ops/s
```

## Async compress
Async compression is fast. Even tough the compression itself could be optimized I get
much better performance than when using a `SnappyFramedOutputStream`:
```
SnappyJavaBenchmark.chunk                   thrpt   20  10998.012 ± 79.410  ops/s
SnappyJavaBenchmark.compressWithPlainJava   thrpt   20      3.950 ±  0.124  ops/s
SnappyJavaBenchmark.compressViaFlows        thrpt   20      3.669 ±  0.044  ops/s
SnappyJavaBenchmark.compressViaAsyncFlows2  thrpt   20      6.957 ±  0.050  ops/s
SnappyJavaBenchmark.compressViaAsyncFlows4  thrpt   20     11.768 ±  0.161  ops/s
```


[canterbury-corpus]: http://corpus.canterbury.ac.nz
