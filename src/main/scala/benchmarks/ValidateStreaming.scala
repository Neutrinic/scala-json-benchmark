package benchmarks

import marshallers.{BirdParser, CirceMarshaller, JacksonStreamingMarshaller, JawnFacadeParser}
import scala.util.Try

/** Exercise handwritten schema contexts independently of the performance fixture. */
object ValidateStreaming {
  def run(): Unit = {
    validate(new JawnFacadeParser, "jawn-facade")
    validate(new JacksonStreamingMarshaller, "jackson-streaming")
  }

  private def validate(parser: BirdParser, name: String): Unit = {
    val reference = new CirceMarshaller
    val reordered = """{"hangs_out":[{"latlon":[1e1,-2.5],"_id":2147483647,"name":"a\\b\"c__unicode__"}],"wing_span":1.25e2,"sights":4.0,"common_names":[],"scientific_name":"bird","unknown":{"nested":[{},[true,null,1]]}}""".replace("__unicode__", "\\" + "u263a")
    require(parser.parse(reordered) == reference.parse(reordered), "Field order, escapes, absent options, unknown subtrees or numbers differ")
    val minimal = """{"scientific_name":"bird","common_names":["one"],"sights":0,"wing_span":1,"hangs_out":[]}"""
    require(parser.parse(minimal) == reference.parse(minimal))
    val nullable = """{"scientific_name":"bird","common_names":[],"sights":1,"wing_span":2,"hangs_out":[{"name":"place","_id":1,"latlon":[],"description":null,"michelin_rate":null}]}"""
    require(parser.parse(nullable) == reference.parse(nullable))
    for (bad <- Seq(
      minimal.replace("\"sights\":0", "\"sights\":4.2"),
      minimal.replace("\"sights\":0", "\"sights\":2147483648"),
      minimal.replace("\"sights\":0", "\"sights\":\"0\""),
      minimal.replace("\"common_names\":[\"one\"]", "\"common_names\":[1]"),
      minimal.replace("\"hangs_out\":[]", "\"hangs_out\":[null]"),
      minimal.replace("\"scientific_name\":\"bird\"", "\"scientific_name\":null"),
      minimal.replace("\"sights\":0,", ""),
      "[]", "null", minimal.dropRight(1), minimal + " trailing"
    )) {
      require(Try(parser.parse(bad)).isFailure, s"Accepted invalid schema/input: $bad")
      require(parser.parse(minimal) == reference.parse(minimal), "Facade state leaked after an error")
    }
    println(s"PASS $name edge cases: order, escapes, unknown nested fields, options, numeric limits, invalid shapes and recovery")
  }
}
