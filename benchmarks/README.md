# Benchmarks

Preliminary benchmarks show that the slowdown is not that bad as I expected. When compressing
the E.coli file from the [Canterbury Corpus][canterbury-corpus] Snappy Flows reaches around 90%
performance of `SnappyFramedOutputStream` with a `NullOutputStream`.

```
SnappyJavaBenchmark.compressEColiWithSnappyFlows  thrpt   20  36.933 ± 0.397  ops/s
SnappyJavaBenchmark.compressEColiWithSnappyJava   thrpt   20  40.690 ± 0.929  ops/s
```

Running a late 2013 15’ MBP, Intel(R) Core(TM) i7-4960HQ CPU @ 2.60GHz, 16GiB.

[canterbury-corpus]: http://corpus.canterbury.ac.nz
