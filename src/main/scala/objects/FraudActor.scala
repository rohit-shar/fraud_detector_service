package objects

object FraudActor {
  case class OrderRequestReceiveCommand(orderNumber: String, jsonPayload: String)
}
