# Mini: Scala JSON benchmark

Each JMH operation processes the complete 25,000-record fixture. Lower time is better.
The ± values are JMH's reported 99.9% confidence intervals, not standard deviations.
This is a single-thread typed case-class microbenchmark, not an HTTP, Spark or streaming-parser benchmark.
Encoders retain library defaults; their JSON output is round-trip equivalent but can differ in formatting and optional fields.

| Library | Decode batch ms | Encode batch ms | Decode rows/s | Decode input MB/s | Decode allocation B/row |
|---|---:|---:|---:|---:|---:|
| jsoniter | 13.674 ± 0.268 | 14.915 ± 0.868 | 1,828,229 | 732.5 | 1,256 |
| circebooster | 43.290 ± 0.474 | 33.830 ± 0.336 | 577,499 | 231.4 | 8,022 |
| circe | 67.554 ± 0.877 | 44.687 ± 0.314 | 370,074 | 148.3 | 9,672 |

Fixture: 25,000 JSON records, 10,016,688 UTF-8 bytes excluding line separators.
Raw timings, all allocation metrics and per-fork samples: `jmh.json`. Full run output: `jmh.log`.
CPU/JVM and fixture checksum: `environment.txt`; source checksums: `source.sha256`.
