package marshallers

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import models.{Bird, Place}

class CirceMarshaller extends Marshaller {
  implicit val placeDecoder: Decoder[Place] = deriveDecoder[Place]
  implicit val birdDecoder: Decoder[Bird] = deriveDecoder[Bird]
  implicit val placeEncoder: Encoder[Place] = deriveEncoder[Place]
  implicit val birdEncoder: Encoder[Bird] = deriveEncoder[Bird]

  def parse(s: String): Bird = {
    io.circe.parser.decode[Bird](s).fold(throw _, identity)
  }

  def toStr(bird: Bird): String = {
    bird.asJson.toString
  }
}
