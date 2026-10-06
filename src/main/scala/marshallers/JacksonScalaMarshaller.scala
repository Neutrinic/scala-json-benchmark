package marshallers

import models.Bird
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule

/** Reuse mapper/type metadata, as recommended for Jackson databinding. */
class JacksonScalaMarshaller extends Marshaller {
  def parse(s: String): Bird = JacksonScalaMarshaller.reader.readValue[Bird](s)
  def toStr(bird: Bird): String = JacksonScalaMarshaller.writer.writeValueAsString(bird)
}

private object JacksonScalaMarshaller {
  val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .build()
  val reader = mapper.readerFor(classOf[Bird])
  val writer = mapper.writerFor(classOf[Bird])
}
