# Mini: Scala JSON benchmark

Each JMH operation processes the complete 25,000-record fixture. Lower time is better.
The ± values are JMH's reported 99.9% confidence intervals, not standard deviations.
This is a single-thread typed case-class microbenchmark, not an HTTP, Spark or streaming-parser benchmark.
Encoders retain library defaults; their JSON output is round-trip equivalent but can differ in formatting and optional fields.

| Library | Decode batch ms | Encode batch ms | Decode rows/s | Decode input MB/s | Decode allocation B/row |
|---|---:|---:|---:|---:|---:|
| jsoniter | 13.651 ± 0.247 | 14.406 ± 0.107 | 1,831,415 | 733.8 | 1,256 |
| jacksonstreaming | 29.200 ± 0.690 | 24.653 ± 0.250 | 856,168 | 343.0 | 1,560 |
| circebooster | 43.190 ± 0.394 | 34.145 ± 0.710 | 578,837 | 231.9 | 8,129 |
| jacksonscala | 48.357 ± 1.786 | 22.098 ± 0.392 | 516,988 | 207.1 | 3,499 |
| jawnfacade | 54.833 ± 1.685 | — | 455,931 | 182.7 | 5,016 |

Fixture: 25,000 JSON records, 10,016,688 UTF-8 bytes excluding line separators.
Raw timings, all allocation metrics and per-fork samples: `jackson-jmh.json`. Full run output: `jackson-jmh.log`.
CPU/JVM and fixture checksum: `jackson-environment.txt`; source checksums: `jackson-source.sha256`.
