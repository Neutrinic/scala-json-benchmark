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
| Lift JSON | 3.5.0 |
| Play JSON | 3.0.6 |
| spray-json | 1.3.6 |

Unused Argonaut integration dependencies and the obsolete dependency-graph
plugin are removed. The build uses Maven Central over HTTPS; the old HTTP,
snapshot and repository-webpage resolvers are removed. Play JSON now uses its
`org.playframework` coordinates. Codec APIs are updated for the newer releases.

Benchmarks return their result arrays so JMH consumes the generated values,
instead of returning only an array length. Resources are closed after loading.
One measured operation still processes all **25,000 records**; results are
**milliseconds per batch**, not milliseconds per record. Parser/codec creation
once per batch is retained from the upstream benchmark.

`benchmarks.Validate` checks all 25,000 source records and encode/decode round
trips using every library before measurements. The fixture data is unchanged.
These checks demonstrate agreement on this fixture, not identical semantics
for arbitrary missing fields, duplicate keys, malformed input or numeric limits.
Each library retains its default formatting and optional-field behavior, so
serialization produces semantically equivalent values, not byte-identical JSON.
The dependency metadata snapshot is in `dependency-versions.json`.

### Run on Linux

Install a JDK (the mini run uses OpenJDK 21), plus `curl` and Python 3, then:

```sh
bash scripts/run-mini.sh
```

The script downloads the pinned sbt launcher into ignored `.tools/`, compiles
and validates, then runs all fourteen benchmarks: one thread, two forks,
five two-second warmup iterations and five two-second measurement iterations.
Forks use a fixed 2 GiB heap and G1 GC; the GC profiler records allocation.
Allow roughly ten minutes plus dependency downloads and compilation.
Set `JAVA_HOME` to select another installed JDK.

Raw JMH JSON, logs, hardware/JVM metadata, CSV and a readable table are saved in
`results/`. The mini machine is a Minisforum AI X1 Lite with a Ryzen 7 255;
this is a single-thread case-class codec benchmark, not HTTP or Spark throughput.
[Completed mini results](results/README.md) include confidence intervals and
allocation measurements; [raw JMH samples](results/jmh.json) retain both forks.
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
