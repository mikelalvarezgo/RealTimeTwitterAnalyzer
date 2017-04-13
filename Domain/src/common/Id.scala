package common

import scala.language.implicitConversions

case class Id(value: String)

object Id {

  implicit def toId(value: String): Id = Id(value)

  implicit def toValue(id: Id): String = id.value
}
