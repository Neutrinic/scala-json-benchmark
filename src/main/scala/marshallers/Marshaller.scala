package marshallers

import models.Bird

trait Marshaller extends BirdParser {
  def toStr(bird: Bird): String
}
