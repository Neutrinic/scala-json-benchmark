package benchmarks



import marshallers._
import models.Bird


import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class SerializationBenchmark{
  import SerializationBenchmark._

  @Benchmark
  def Json4SMarshaller_toStr(): Array[String] = {
    val parser = new Json4SMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }

  @Benchmark
  def LiftMarshaller_toStr(): Array[String] = {
    val parser = new LiftMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }

  @Benchmark
  def PlayMarshaller_toStr(): Array[String] = {
    val parser = new PlayMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }

  @Benchmark
  def ArgonautMarshaller_toStr(): Array[String] = {
    val parser = new ArgonautMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }

  @Benchmark
  def SprayMarshaller_toStr(): Array[String] = {
    val parser = new SprayMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }

  @Benchmark
  def CirceMarshaller_toStr(): Array[String] = {
    val parser = new CirceMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }

  @Benchmark
  def JsoniterMarshaller_toStr(): Array[String] = {
    val parser = new JsoniterMarshaller
    val strs = birds.map(parser.toStr)
    strs
  }
}
object SerializationBenchmark {
  val birds: Array[Bird] = Fixture.birds
}
