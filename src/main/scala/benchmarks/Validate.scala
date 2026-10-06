package benchmarks

import marshallers._

/** Check typed decoding and round trips outside measured JMH iterations. */
object Validate {
  def main(args: Array[String]): Unit = {
    val libraries: Seq[(String, Marshaller)] = Seq(
      "argonaut" -> new ArgonautMarshaller,
      "circe" -> new CirceMarshaller,
      "json4s" -> new Json4SMarshaller,
      "jsoniter-scala" -> new JsoniterMarshaller,
      "lift-json" -> new LiftMarshaller,
      "play-json" -> new PlayMarshaller,
      "spray-json" -> new SprayMarshaller
    )
    libraries.foreach { case (name, codec) =>
      Fixture.json.indices.foreach { i =>
        val bird = Fixture.birds(i)
        require(codec.parse(Fixture.json(i)) == bird, s"$name decode mismatch at row $i")
        require(codec.parse(codec.toStr(bird)) == bird, s"$name round-trip mismatch at row $i")
      }
      println(s"PASS $name: ${Fixture.json.length} decodes and round trips")
    }
  }
}
