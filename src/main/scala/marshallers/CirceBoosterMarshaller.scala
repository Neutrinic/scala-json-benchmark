package marshallers

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import models.{Bird, Place}

/** Retain Circe's AST and derived codecs; accelerate text IO and primitive codecs. */
class CirceBoosterMarshaller extends Marshaller {
  implicit val placeDecoder: Decoder[Place] = deriveDecoder[Place]
  implicit val birdDecoder: Decoder[Bird] = deriveDecoder[Bird]
  implicit val placeEncoder: Encoder[Place] = deriveEncoder[Place]
  implicit val birdEncoder: Encoder[Bird] = deriveEncoder[Bird]

  def parse(s: String): Bird =
    readFromString[Json](s).as[Bird].fold(throw _, identity)

  def toStr(bird: Bird): String =
    writeToString[Json](bird.asJson)
}
