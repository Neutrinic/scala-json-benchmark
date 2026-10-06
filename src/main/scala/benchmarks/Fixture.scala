package benchmarks

import java.nio.charset.StandardCharsets
import scala.io.Source
import scala.util.Using
import marshallers.CirceMarshaller
import models.Bird

object Fixture {
  val json: Array[String] = Using.resource(Source.fromInputStream(
    getClass.getClassLoader.getResourceAsStream("birds.data"), StandardCharsets.UTF_8.name()
  ))(_.getLines().toArray)
  require(json.length == 25000, s"Expected 25000 records, found ${json.length}")
  val birds: Array[Bird] = {
    val parser = new CirceMarshaller
    json.map(parser.parse)
  }
}
