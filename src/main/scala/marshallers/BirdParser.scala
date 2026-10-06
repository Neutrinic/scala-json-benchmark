package marshallers

import models.Bird

trait BirdParser {
  def parse(s: String): Bird
}
