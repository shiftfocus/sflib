package ca.shiftfocus.lib.uuid

import ca.shiftfocus.uuid.UUID
import play.api.mvc.PathBindable

object UuidPathBinder {
  implicit def uuidPathBindable(implicit stringBinder: PathBindable[String]) = new PathBindable[ca.shiftfocus.uuid.UUID] {

    def bind(key: String, value: String): Either[String, UUID] = {
      for {
        textValue <- stringBinder.bind(key, value).right
        uuid <- {
          val result = if (UUID.isValid(textValue)) {
            Right(UUID(textValue))
          }
          else {
            Left("Invalid UUID")
          }
          result
        }.right
      }
      yield uuid
    }

    def unbind(key: String, uuid: UUID): String = {
      stringBinder.unbind(key, uuid.string)
    }

  }
}