package benchmarks

import models.Bird

import marshallers._


import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class DeserializationBenchmark {
  import DeserializationBenchmark._

  @Benchmark
  def Json4SMarshaller_parse(): Array[Bird] = {
    val parser = new Json4SMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }

  @Benchmark
  def CirceMarshaller_parse(): Array[Bird] = {
    val parser = new CirceMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }

  @Benchmark
  def SprayMarshaller_parse(): Array[Bird] = {
    val parser = new SprayMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }

  @Benchmark
  def ArgonautMarshaller_parse(): Array[Bird] = {
    val parser = new ArgonautMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }

  @Benchmark
  def PlayMarshaller_parse(): Array[Bird] = {
    val parser = new PlayMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }

  @Benchmark
  def LiftMarshaller_parse(): Array[Bird] = {
    val parser = new LiftMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }

  @Benchmark
  def JsoniterMarshaller_parse(): Array[Bird] = {
    val parser = new JsoniterMarshaller
    val parsed = data.map(parser.parse)
    parsed
  }
}

object DeserializationBenchmark {
  val data: Array[String] = Fixture.json
}
