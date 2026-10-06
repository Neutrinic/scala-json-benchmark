package benchmarks

import marshallers._

/** Check typed decoding and round trips outside measured JMH iterations. */
object Validate {
  def main(args: Array[String]): Unit = {
    val libraries: Seq[(String, BirdParser)] = Seq(
      "argonaut" -> new ArgonautMarshaller,
      "circe" -> new CirceMarshaller,
      "circe-booster" -> new CirceBoosterMarshaller,
      "json4s" -> new Json4SMarshaller,
      "jsoniter-scala" -> new JsoniterMarshaller,
      "jawn-facade" -> new JawnFacadeParser,
      "jackson-scala" -> new JacksonScalaMarshaller,
      "jackson-streaming" -> new JacksonStreamingMarshaller,
      "lift-json" -> new LiftMarshaller,
      "play-json" -> new PlayMarshaller,
      "spray-json" -> new SprayMarshaller
    )
    libraries.foreach { case (name, codec) =>
      Fixture.json.indices.foreach { i =>
        val bird = Fixture.birds(i)
        require(codec.parse(Fixture.json(i)) == bird, s"$name decode mismatch at row $i")
        codec match {
          case writer: Marshaller => require(codec.parse(writer.toStr(bird)) == bird, s"$name round-trip mismatch at row $i")
          case _ => ()
        }
      }
      println(s"PASS $name: ${Fixture.json.length} decodes" + (if (codec.isInstanceOf[Marshaller]) " and round trips" else " (decode-only)"))
    }
    ValidateStreaming.run()
  }
}
