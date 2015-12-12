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
These are pretty interesting results, though I have to say I'm a bit suspicious about the
async benchmark quality. After all the throughput should not be greater than that of simple
chunking:

```
SnappyJavaBenchmark.chunk                  thrpt   20  6.945 ± 0.025  ops/s
SnappyJavaBenchmark.compressViaAsyncFlows  thrpt   20  6.980 ± 0.019  ops/s
SnappyJavaBenchmark.compressViaFlows       thrpt   20  3.567 ± 0.102  ops/s
SnappyJavaBenchmark.compressWithPlainJava  thrpt   20  3.772 ± 0.093  ops/s
```


[canterbury-corpus]: http://corpus.canterbury.ac.nz
