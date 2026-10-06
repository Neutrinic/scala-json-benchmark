# Mini: Scala JSON benchmark

Each JMH operation processes the complete 25,000-record fixture. Lower time is better.
The ± values are JMH's reported 99.9% confidence intervals, not standard deviations.
This is a single-thread typed case-class microbenchmark, not an HTTP, Spark or streaming-parser benchmark.
Encoders retain library defaults; their JSON output is round-trip equivalent but can differ in formatting and optional fields.

| Library | Decode batch ms | Encode batch ms | Decode rows/s | Decode input MB/s | Decode allocation B/row |
|---|---:|---:|---:|---:|---:|
| jsoniter | 13.601 ± 0.265 | 14.226 ± 0.136 | 1,838,104 | 736.5 | 1,256 |
| spray | 65.468 ± 2.182 | 53.448 ± 0.433 | 381,869 | 153.0 | 7,087 |
| circe | 67.385 ± 0.681 | 44.484 ± 0.452 | 371,003 | 148.6 | 9,672 |
| lift | 81.975 ± 1.869 | 52.746 ± 4.100 | 304,972 | 122.2 | 12,537 |
| play | 84.038 ± 0.183 | 121.495 ± 1.316 | 297,483 | 119.2 | 16,954 |
| argonaut | 110.935 ± 0.427 | 93.941 ± 1.722 | 225,358 | 90.3 | 23,090 |
| json4s | 126.199 ± 1.075 | 116.321 ± 1.336 | 198,100 | 79.4 | 13,653 |

Fixture: 25,000 JSON records, 10,016,688 UTF-8 bytes excluding line separators.
Raw timings, all allocation metrics and per-fork samples: `jmh.json`. Full run output: `jmh.log`.
CPU/JVM and fixture checksum: `environment.txt`; source checksums: `source.sha256`.
