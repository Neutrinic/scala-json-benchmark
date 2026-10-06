# A Scala JSON serialization/deserialization benchmark

## Updated fork

This fork updates the original seven-library comparison to stable releases
available for Scala 2.13. Versions were checked against Maven Central metadata
on 6 October 2026 UTC. Scala 2.13 retains support for all seven libraries; it is
not a claim that Scala 2.13 is the newest Scala major version.

| Component | Version |
|---|---|
| Scala | 2.13.18 |
| sbt | 2.0.10 |
| sbt-jmh / JMH | 0.4.8 / 1.37 |
| Argonaut | 6.3.10 |
| Circe | 0.14.16 |
| json4s | 4.0.7 |
| jsoniter-scala | 2.41.2 |
| jsoniter-scala-circe booster | 2.41.2 |
| Lift JSON | 3.5.0 |
| Play JSON | 3.0.6 |
| spray-json | 1.3.6 |
| Jawn parser / custom direct facade | 1.8.0 |
| Jackson core / databind / Scala module | 3.2.3 |

Unused Argonaut integration dependencies and the obsolete dependency-graph
plugin are removed. The build uses Maven Central over HTTPS; the old HTTP,
snapshot and repository-webpage resolvers are removed. Play JSON now uses its
`org.playframework` coordinates. Codec APIs are updated for the newer releases.

Benchmarks return their result arrays so JMH consumes the generated values,
instead of returning only an array length. Resources are closed after loading.
One measured operation still processes all **25,000 records**; results are
**milliseconds per batch**, not milliseconds per record. Parser/codec creation
once per batch is retained from the upstream benchmark.

`benchmarks.Validate` checks all 25,000 source records using every decoder and
encode/decode round trips for implementations with encoders before measurements.
The fixture data is unchanged.
These checks demonstrate agreement on this fixture, not identical semantics
for arbitrary missing fields, duplicate keys, malformed input or numeric limits.
Each library retains its default formatting and optional-field behavior, so
serialization produces semantically equivalent values, not byte-identical JSON.
The dependency metadata snapshot is in `dependency-versions.json`.

### Circe booster

The eighth implementation, `CirceBoosterMarshaller`, keeps Circe's derived
encoders/decoders and intermediate `io.circe.Json` tree. It imports the booster's
numeric codecs before deriving the case-class codecs and uses jsoniter-scala
to parse and write that tree. It does not use the native generated `Bird` codec.
The original Circe and native jsoniter implementations remain independent.

For a fresh comparison of plain Circe, boosted Circe and native jsoniter:

```sh
RESULTS_DIR=results/booster-comparison \
BENCHMARK_LIBRARIES=circe,circebooster,jsoniter \
BENCHMARK_FILTER='benchmarks.*Benchmark.(Circe|CirceBooster|Jsoniter)Marshaller.*' \
  bash scripts/run-mini.sh
```

This validates all eleven implementations, then measures the selected three
together with identical JMH settings (about four minutes). These six fresh
measurements are kept in their own directory; results from separate runs are
not spliced together. The original seven-library results remain in `results/`.

### Direct Jawn facade

Plain Circe already uses Jawn to construct its JSON tree. The ninth implementation
is therefore a **custom, schema-specific facade** that builds `Bird` and `Place`
as Jawn parses, bypassing a generic JSON tree and a later decoder traversal.
Scalar number tokens and typed array builders are temporary values; this is not
a zero-allocation decoder. Adding another model requires handwritten contexts.

Jawn is a parser, so the facade has **no encoding benchmark**. `BirdParser` is the
decode-only interface; the existing encoders implement `Marshaller`. Validation
checks all 25,000 decodes against Circe, plus field order, escaped strings,
unknown nested fields, optional null/missing values, numeric limits, type errors
and reuse after a failed parse. Instances are for sequential use, not shared
concurrently. Required fields are checked, duplicate keys use their last value,
integers must be exact 32-bit values, and double fields must be finite.

Run a fresh, decode-only comparison with the other relevant implementations:

```sh
RESULTS_DIR=results/jawn-facade-comparison \
BENCHMARK_OPERATIONS=decode \
BENCHMARK_LIBRARIES=circe,circebooster,jsoniter,jawnfacade \
BENCHMARK_FILTER='benchmarks.DeserializationBenchmark.(Circe|CirceBooster|Jsoniter|JawnFacade)Marshaller_parse' \
  bash scripts/run-mini.sh
```

This explicitly pins Jawn 1.8.0 for both the custom facade and Circe. Earlier
recorded comparisons resolved Jawn 1.7.0, so use this fresh four-way run when
comparing the new facade. [Results and allocations](results/jawn-facade-comparison/README.md)
are recorded separately from the earlier runs.

### Jackson

Two implementations use Jackson **3.2.3** (`tools.jackson.*`), which coexists
with the legacy Jackson 2 dependencies used by json4s and Play JSON:

- `JacksonScalaMarshaller`: `JsonMapper` with `DefaultScalaModule`, using cached
  `ObjectReader`/`ObjectWriter` instances for `Bird`. Mapper setup and first-use
  metadata discovery are warmed up outside measured iterations. Unknown fields
  are ignored; trailing tokens and null primitive values are rejected.
- `JacksonStreamingMarshaller`: handwritten `JsonParser` token-to-model decoding
  and `JsonGenerator` model-to-token encoding. It does not construct a generic
  JSON tree. It checks required fields, skips unknown subtrees, supports null or
  absent optional fields, and rejects fractional/out-of-range integer values.
  Its reusable `JsonFactory` creates/cleans up a parser or generator per record.
  Encoding uses a standard `StringWriter`; absent options are written as null.

Both implementations are checked against every fixture record and round trip.
The handwritten Jackson parser also shares the Jawn facade's schema edge checks.
This tests the String API on the same fixture as the other libraries; it does
not measure UTF-8 byte-stream input, HTTP delivery or a custom zero-copy sink.

New comparisons use filename prefixes in the existing `results/` directory,
without adding report directories:

```sh
RESULTS_PREFIX=jackson- \
BENCHMARK_LIBRARIES=circebooster,jsoniter,jawnfacade,jacksonscala,jacksonstreaming \
BENCHMARK_FILTER='benchmarks.*Benchmark.(CirceBooster|Jsoniter|JawnFacade|JacksonScala|JacksonStreaming)Marshaller.*' \
  bash scripts/run-mini.sh
```

This records nine fresh measurements (five decoders, four encoders) together.
The Jawn facade remains decode-only. [Jackson comparison](results/jackson-README.md)
includes timing uncertainty and allocations; raw files use the `jackson-` prefix.

### Run on Linux

Install a JDK (the mini run uses OpenJDK 21), plus `curl` and Python 3, then:

```sh
bash scripts/run-mini.sh
```

The script downloads the pinned sbt launcher into ignored `.tools/`, compiles
and validates, then runs all twenty-one benchmarks (eleven decoders, ten encoders): one thread, two forks,
five two-second warmup iterations and five two-second measurement iterations.
Forks use a fixed 2 GiB heap and G1 GC; the GC profiler records allocation.
Allow roughly fifteen minutes plus dependency downloads and compilation.
Set `JAVA_HOME` to select another installed JDK.
Use `RESULTS_PREFIX` to preserve additional runs as files without creating folders.

Raw JMH JSON, logs, hardware/JVM metadata, CSV and a readable table are saved in
`results/`. The mini machine is a Minisforum AI X1 Lite with a Ryzen 7 255;
this is a single-thread case-class codec benchmark, not HTTP or Spark throughput.
[Original seven-library mini results](results/README.md) include confidence
intervals and allocation measurements; [raw JMH samples](results/jmh.json)
retain both forks. [Fresh Circe booster comparison](results/booster-comparison/README.md)
compares boosted Circe with plain Circe and native jsoniter in the same run.
Modern measurements should not be directly compared with the historical tables
below: hardware, JVM, dependencies and result consumption have changed.

## Original upstream documentation and historical results

### Introduction
When decision about JSON library should be made because of performance, this benchmark can be helpful. Benchmarked libraries:
- [Argonaut](https://github.com/argonaut-io/argonaut)
- [circe](https://github.com/circe/circe)
- [JSON4S](https://github.com/json4s/json4s)
- [Lift](https://github.com/lift/framework/tree/master/core/json)
- [spray-json](https://github.com/spray/spray-json)
- [play-json](https://github.com/playframework/play-json)
- [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala)

This project also demonstrates how to use these different JSON libraries to serialize/deserialize a Scala `case class`.

### Benchmark
Dataset and types ([Bird](src/main/scala/models/Bird.scala) and [Place](src/main/scala/models/Place.scala)) are from this repo: [scala-json-benchmark](https://github.com/nlw0/scala-json-benchmark). For benchmark used [jmh](https://openjdk.java.net/projects/code-tools/jmh/) via [sbt-jmh plugin](https://github.com/ktoso/sbt-jmh/). File [birds.data](src/main/resources/birds.data) contains 25000 lines. Each of line is serialized json of `Bird`.
- Serialization benchmark is in [SerializationBenchmark.scala](src/main/scala/benchmarks/SerializationBenchmark.scala). To run it use `jmh:run -i 5 -wi 5 -f1 -t1 .*SerializationBenchmark*` in `SBT`
- Deserialization benchmark is in [DeserializationBenchmark.scala](src/main/scala/benchmarks/DeserializationBenchmark.scala). To run it use `jmh:run -i 5 -wi 5 -f1 -t1 .*DeserializationBenchmark*` in `SBT`

### Results
#### MacBook Pro (Retina, 13-inch, Early 2015):
- OS: macOS Sierra 10.12.6
- CPU: Intel Core i7 3.1 GHz
- Memory: 16 GB
- JVM: Java HotSpot(TM) 64-Bit Server VM, 25.181-b13

##### Serialization
| Library   | Time, ms|
| ----------| -------:|
| jsoniter  |  50.113 |
| circe     | 140.868 |
| spray     | 165.045 |
| lift      | 206.377 |
| play-json | 261.774 |
| Argonaut  | 306.668 |
| json4s    | 385.766 |

##### Deserialization
| Library   | Time, ms|
| ----------| -------:|
| jsoniter  | 78.070  |
| circe     | 130.938 |
| spray     | 180.159 |
| lift      | 220.421 |
| Argonaut  | 319.450 |
| play-json | 357.515 |
| json4s    | 467.872 |

#### Desktop machine:
- OS: Microsoft Windows 10 Pro N  x64 [Version 10.0.17134.590]
- CPU: AMD Ryzen 7 2700X Eight-Core Processor i7 3.7 GHz
- Memory: DDR4-3200 GHz 16 GB
- JVM: Java HotSpot(TM) 64-Bit Server VM, 25.201-b09

##### Serialization
| Library   | Time, ms|
| ----------| -------:|
| jsoniter  |  41.140 |
| circe     | 104.434 |
| spray     | 132.525 |
| lift      | 171.015 |
| play-json | 215.128 |
| Argonaut  | 255.371 |
| json4s    | 310.488 |

##### Deserialization
| Library   | Time, ms|
| ----------| -------:|
| jsoniter  | 68.833  |
| circe     | 117.592 |
| spray     | 155.961 |
| lift      | 220.304 |
| Argonaut  | 264.283 |
| play-json | 342.450 |
| json4s    | 403.946 |
