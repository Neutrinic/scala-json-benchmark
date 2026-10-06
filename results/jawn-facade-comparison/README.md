# Mini: Scala JSON benchmark

Each JMH operation processes the complete 25,000-record fixture. Lower time is better.
The ± values are JMH's reported 99.9% confidence intervals, not standard deviations.
Decode-only run: encode timings are not measured. The custom Jawn facade has no encoder.
This is a single-thread typed case-class microbenchmark, not an HTTP, Spark or streaming-parser benchmark.
Encoders retain library defaults; their JSON output is round-trip equivalent but can differ in formatting and optional fields.

| Library | Decode batch ms | Encode batch ms | Decode rows/s | Decode input MB/s | Decode allocation B/row |
|---|---:|---:|---:|---:|---:|
| jsoniter | 13.505 ± 0.196 | — | 1,851,112 | 741.7 | 1,256 |
| circebooster | 44.192 ± 0.910 | — | 565,711 | 226.7 | 8,129 |
| jawnfacade | 53.366 ± 0.476 | — | 468,464 | 187.7 | 5,016 |
| circe | 67.903 ± 0.536 | — | 368,172 | 147.5 | 9,664 |

Fixture: 25,000 JSON records, 10,016,688 UTF-8 bytes excluding line separators.
Raw timings, all allocation metrics and per-fork samples: `jmh.json`. Full run output: `jmh.log`.
CPU/JVM and fixture checksum: `environment.txt`; source checksums: `source.sha256`.
