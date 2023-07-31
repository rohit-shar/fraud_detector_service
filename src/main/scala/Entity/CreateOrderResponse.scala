package Entity


case class CreateOrderResponse(orderNum: String, var score: Double, var avgScore: Double, var exception: String, var siftResponse: String)
