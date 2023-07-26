package model

import model.OrderModels.Order

case class FraudResponse(
                          var userId: String,
                          var orderId: String,
                          var orderNumber: String,
                          var emailId: String,
                          var orderStatus: String,
                          var avg_score: Double = 0.0,
                          var paymentAbuseScore: Double = 0.0,
                          var order: Order,
                          var errorMessage: String = null
                        )



