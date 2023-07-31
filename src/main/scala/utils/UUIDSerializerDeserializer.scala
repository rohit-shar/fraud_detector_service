package utils

import java.util.UUID

import net.liftweb.json._

class UUIDSerializerDeserializer extends Serializer[UUID] {
  private val UUIDClass = classOf[UUID]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), UUID] = {
    case (TypeInfo(UUIDClass, _), json) => json match {
      case JString(s) => UUID.fromString(s)
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: UUID => JString(x.toString)
  }
}
