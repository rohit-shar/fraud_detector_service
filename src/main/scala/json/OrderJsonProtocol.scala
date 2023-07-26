package json

import spray.json._
import model._

import java.util.UUID

object UUIDFormat extends JsonFormat[UUID] {
  def write(uuid: UUID) = JsString(uuid.toString)

  def read(value: JsValue) = {
    value match {
      case JsString(uuid) => UUID.fromString(uuid)
      case _ => throw new DeserializationException("Expected hexadecimal UUID string")
    }
  }
}

/*
Below code is for unmarshalling
* */
trait OrderJsonProtocol extends DefaultJsonProtocol {
  implicit val uuid = UUIDFormat
  implicit val cartItemJsonFormat = jsonFormat11(OrderModels.CartItem)
  implicit val billingShippingAddressJsonFormat = jsonFormat8(OrderModels.BillingShippingAddress)
  implicit val paymentInfoJsonFormat = jsonFormat4(OrderModels.PaymentInfo)
  implicit val shipToGroupJsonFormat = jsonFormat22(OrderModels.ShipToGroup)
  implicit val shippingAddressPerItemJsonFormat = jsonFormat2(OrderModels.ShippingAddressPerItem)
  implicit val httpDataFormat = jsonFormat2(OrderModels.HttpData)
  implicit val cartEntry = jsonFormat19(OrderModels.CartEntry)
  implicit val cartJsonFormat = jsonFormat6(OrderModels.Cart)
  implicit val paymentResponseJsonFormat = jsonFormat11(OrderModels.PaymentResponse)
  implicit val orderJsonFormat = jsonFormat21(OrderModels.Order)
}
