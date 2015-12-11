# Benchmarks

Preliminary benchmarks show that the slowdown is not that bad as I expected. When compressing
the E.coli file from the [Canterbury Corpus][canterbury-corpus] Snappy Flows reaches almost 90%
performance of `SnappyFramedOutputStream` with a `NullOutputStream`.

```
SnappyJavaBenchmark.compressEColiWithSnappyFlows  thrpt   20  35.473 ± 0.415  ops/s
SnappyJavaBenchmark.compressEColiWithSnappyJava   thrpt   20  40.651 ± 0.793  ops/s
```

Running a late 2013 15’ MBP, Intel(R) Core(TM) i7-4960HQ CPU @ 2.60GHz, 16GiB.

[canterbury-corpus]: http://corpus.canterbury.ac.nz
