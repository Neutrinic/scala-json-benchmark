package marshallers

import java.io.StringWriter
import models.{Bird, Place}
import scala.collection.mutable.ListBuffer
import tools.jackson.core.{JsonGenerator, JsonParser, JsonToken, ObjectReadContext, ObjectWriteContext}
import tools.jackson.core.json.JsonFactory

/** Handwritten token-to-model parsing and model-to-token writing, without a JSON tree. */
class JacksonStreamingMarshaller extends Marshaller {
  import JacksonStreamingMarshaller._

  def parse(s: String): Bird = {
    val parser = factory.createParser(ObjectReadContext.empty(), s)
    try {
      parser.nextToken()
      val bird = readBird(parser)
      require(parser.nextToken() == null, "Trailing content")
      bird
    } finally parser.close()
  }

  def toStr(bird: Bird): String = {
    val output = new StringWriter
    val writer = factory.createGenerator(ObjectWriteContext.empty(), output)
    try writeBird(writer, bird) finally writer.close()
    output.toString
  }
}

private object JacksonStreamingMarshaller {
  val factory: JsonFactory = JsonFactory.builder().build()

  private def expect(p: JsonParser, token: JsonToken): Unit =
    require(p.currentToken() == token, s"Expected $token, got ${p.currentToken()}")

  private def readString(p: JsonParser): String = { expect(p, JsonToken.VALUE_STRING); p.getString() }
  private def readInt(p: JsonParser): Int = {
    if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) p.getIntValue()
    else {
      expect(p, JsonToken.VALUE_NUMBER_FLOAT)
      val n = p.getDecimalValue()
      n.intValueExact()
    }
  }
  private def readDouble(p: JsonParser): Double = {
    require(p.currentToken() != null && p.currentToken().isNumeric(), "Expected a number")
    val n = p.getDoubleValue()
    require(java.lang.Double.isFinite(n), "Expected a finite double")
    n
  }
  private def readList[A](p: JsonParser)(read: JsonParser => A): List[A] = {
    expect(p, JsonToken.START_ARRAY)
    val values = ListBuffer.empty[A]
    while (p.nextToken() != JsonToken.END_ARRAY) {
      require(p.currentToken() != null, "Unterminated array")
      values += read(p)
    }
    values.toList
  }
  private def readBird(p: JsonParser): Bird = {
    expect(p, JsonToken.START_OBJECT)
    var scientificName: String = null
    var commonNames = List.empty[String]
    var sights = 0
    var wingSpan = 0.0
    var places = List.empty[Place]
    var seen = 0
    while (p.nextToken() != JsonToken.END_OBJECT) {
      expect(p, JsonToken.PROPERTY_NAME)
      val key = p.currentName()
      require(p.nextToken() != null, "Missing value")
      key match {
        case "scientific_name" => scientificName = readString(p); seen |= 1
        case "common_names" => commonNames = readList(p)(readString); seen |= 2
        case "sights" => sights = readInt(p); seen |= 4
        case "wing_span" => wingSpan = readDouble(p); seen |= 8
        case "hangs_out" => places = readList(p)(readPlace); seen |= 16
        case _ => p.skipChildren()
      }
    }
    require(seen == 31, "Missing required Bird fields")
    Bird(scientificName, commonNames, sights, wingSpan, places)
  }
  private def readPlace(p: JsonParser): Place = {
    expect(p, JsonToken.START_OBJECT)
    var name: String = null
    var id = 0
    var latlon = List.empty[Double]
    var description = Option.empty[String]
    var rate = Option.empty[Int]
    var seen = 0
    while (p.nextToken() != JsonToken.END_OBJECT) {
      expect(p, JsonToken.PROPERTY_NAME)
      val key = p.currentName()
      require(p.nextToken() != null, "Missing value")
      key match {
        case "name" => name = readString(p); seen |= 1
        case "_id" => id = readInt(p); seen |= 2
        case "latlon" => latlon = readList(p)(readDouble); seen |= 4
        case "description" => description = if (p.currentToken() == JsonToken.VALUE_NULL) None else Some(readString(p))
        case "michelin_rate" => rate = if (p.currentToken() == JsonToken.VALUE_NULL) None else Some(readInt(p))
        case _ => p.skipChildren()
      }
    }
    require(seen == 7, "Missing required Place fields")
    Place(name, id, latlon, description, rate)
  }
  private def writeBird(g: JsonGenerator, bird: Bird): Unit = {
    g.writeStartObject()
    g.writeStringProperty("scientific_name", bird.scientific_name)
    g.writeArrayPropertyStart("common_names")
    bird.common_names.foreach(g.writeString)
    g.writeEndArray()
    g.writeNumberProperty("sights", bird.sights)
    g.writeNumberProperty("wing_span", bird.wing_span)
    g.writeArrayPropertyStart("hangs_out")
    bird.hangs_out.foreach { place =>
      g.writeStartObject()
      g.writeStringProperty("name", place.name)
      g.writeNumberProperty("_id", place._id)
      g.writeArrayPropertyStart("latlon")
      place.latlon.foreach(g.writeNumber)
      g.writeEndArray()
      place.description match {
        case Some(value) => g.writeStringProperty("description", value)
        case None => g.writeNullProperty("description")
      }
      place.michelin_rate match {
        case Some(value) => g.writeNumberProperty("michelin_rate", value)
        case None => g.writeNullProperty("michelin_rate")
      }
      g.writeEndObject()
    }
    g.writeEndArray()
    g.writeEndObject()
  }
}
