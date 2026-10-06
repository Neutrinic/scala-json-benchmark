# Scala JSON benchmark

Eleven Scala JSON implementations, tested against the same 25,000-record fixture.
The **bold library names are the original seven**; the other four are additions to this fork.

## Results on mini

Minisforum AI X1 Lite · Ryzen 7 255 · 32 GB RAM · Ubuntu 24.04 · OpenJDK 21.
**Lower is better. Times are milliseconds for all 25,000 records**, with JMH's
99.9% confidence interval after ±. Allocation is bytes per decoded record.

| Implementation | Version | Decode ms ± CI | Encode ms ± CI | Decode B/record |
|---|---|---:|---:|---:|
| **jsoniter-scala** | 2.41.2 | 13.60 ± 0.26 | 14.23 ± 0.14 | 1,256 |
| Jackson streaming (handwritten) | 3.2.3 | 29.20 ± 0.69 | 24.65 ± 0.25 | 1,560 |
| Circe + jsoniter booster | 0.14.16 / 2.41.2 | 43.19 ± 0.39 | 34.14 ± 0.71 | 8,129 |
| Jackson Scala module | 3.2.3 | 48.36 ± 1.79 | 22.10 ± 0.39 | 3,499 |
| Jawn direct facade (handwritten) | 1.8.0 | 54.83 ± 1.69 | — | 5,016 |
| **spray-json** | 1.3.6 | 65.47 ± 2.18 | 53.45 ± 0.43 | 7,087 |
| **Circe** | 0.14.16 | 67.38 ± 0.68 | 44.48 ± 0.45 | 9,672 |
| **Lift JSON** | 3.5.0 | 81.97 ± 1.87 | 52.75 ± 4.10 | 12,537 |
| **Play JSON** | 3.0.6 | 84.04 ± 0.18 | 121.49 ± 1.32 | 16,954 |
| **Argonaut** | 6.3.10 | 110.93 ± 0.43 | 93.94 ± 1.72 | 23,090 |
| **json4s** | 4.0.7 | 126.20 ± 1.07 | 116.32 ± 1.34 | 13,653 |

Sorted by decode time. Jawn is decode-only. The original seven come from the
[initial full run](results/summary.csv); additions come from the
[latest Jackson comparison](results/jackson-summary.csv). These are separate runs
on the same machine with the same JMH settings, not one simultaneous eleven-way
measurement. The initial run resolved Jawn 1.7.0; the latest run pins 1.8.0.
Small differences between runs should not be treated as meaningful.

Native jsoniter leads both operations on this fixture. Jackson streaming decodes
faster than Jackson's Scala binding, while the Scala binding encodes faster.
This measures single-thread String-to-case-class codecs, not HTTP or Spark throughput.

## Reproduce

Install a JDK, `curl` and Python 3 on Linux, then run:

```sh
bash scripts/run-mini.sh
```

This validates every implementation and runs all 21 benchmarks: eleven decoders
and ten encoders. JMH uses one thread, two forks, five two-second warmups and five
two-second measurements per fork, a fixed 2 GiB heap, G1 GC and the GC profiler.
Allow about fifteen minutes plus compilation and dependency downloads.
Set `JAVA_HOME` to select another installed JDK.

To keep another run without adding folders:

```sh
RESULTS_PREFIX=full- bash scripts/run-mini.sh
```

The script saves CSV, raw JMH JSON, logs, JVM/hardware metadata and source checksums
in `results/`. The displayed table uses [baseline raw samples](results/jmh.json)
and [latest comparison raw samples](results/jackson-jmh.json).

<details>
<summary>Implementation details and validation</summary>

### What is being compared

The original seven are Argonaut, Circe, json4s, jsoniter-scala, Lift JSON,
Play JSON and spray-json. Their independent implementations are retained.

- **Circe booster** retains Circe's derived codecs and intermediate `io.circe.Json`
  tree, using jsoniter-scala to parse/write it. Booster numeric codecs are imported
  before case-class derivation. It is distinct from native jsoniter's generated codec.
- **Jawn direct facade** builds `Bird` and `Place` during parsing without a generic
  JSON tree. It requires handwritten contexts for each schema and has no encoder.
  Typed array builders and numeric tokens still allocate temporary values.
- **Jackson Scala module** uses a cached `JsonMapper`, `DefaultScalaModule` and
  `ObjectReader`/`ObjectWriter`. Setup and first-use metadata discovery are warmed up.
- **Jackson streaming** uses handwritten token-to-model parsing and model-to-token
  encoding, with a reusable factory and a parser/generator closed per record.
  Encoding uses `StringWriter`; absent options are written as null.

Jackson 3 (`tools.jackson.*`) coexists with the legacy Jackson 2 dependencies used
by json4s and Play. The custom Jawn and Jackson parsers check required fields, skip
unknown subtrees, accept null/missing optional fields, and reject fractional or
out-of-range integers. Jawn instances are for sequential use.

### Validation and interpretation

`benchmarks.Validate` checks every decoder against all 25,000 fixture records,
and all ten encoders through round trips. The handwritten parsers also share
checks for field order, escaping, unknown nested fields, optional values, numeric
limits, malformed input and recovery after a failed parse.

Benchmarks return result arrays for JMH to consume. Parser/codec creation once
per batch is retained from upstream. The unchanged fixture tests agreement on
these records; it does not establish identical semantics for every possible input.
Serialization retains each library's formatting and optional-field behavior, so
outputs are semantically equivalent rather than byte-identical.

### Build

Scala 2.13.18 supports all original seven libraries. The build uses sbt 2.0.10,
sbt-jmh 0.4.8 and JMH 1.37, with Maven Central over HTTPS. Library versions are
listed in the results table; the checked dependency metadata is in
[dependency-versions.json](dependency-versions.json).

### Focused runs

Filter the existing script rather than adding benchmark projects or result folders:

```sh
RESULTS_PREFIX=jackson- \
BENCHMARK_LIBRARIES=circebooster,jsoniter,jawnfacade,jacksonscala,jacksonstreaming \
BENCHMARK_FILTER='benchmarks.*Benchmark.(CirceBooster|Jsoniter|JawnFacade|JacksonScala|JacksonStreaming)Marshaller.*' \
  bash scripts/run-mini.sh
```

`BENCHMARK_OPERATIONS=decode` selects decode-only summaries; pair it with a
`benchmarks.DeserializationBenchmark.*` filter to run only decoders.
Earlier focused comparisons remain in `results/` for provenance; the overview
above is the main entry point.

</details>

## Credits

Forked from [REASY/scala-json-benchmark](https://github.com/REASY/scala-json-benchmark).
The [Bird](src/main/scala/models/Bird.scala), [Place](src/main/scala/models/Place.scala)
and [fixture](src/main/resources/birds.data) originate from
[nlw0/scala-json-benchmark](https://github.com/nlw0/scala-json-benchmark).
Upstream's historical measurements remain available in its README and Git history;
hardware, JVMs, dependencies and result consumption differ from this fork.
See [LICENSE](LICENSE).
