package objects

import model.OrderModels.Order

object FraudActor {
  case class OrderRequestReceiveCommand(orderNumber: String, order: Order)
}
