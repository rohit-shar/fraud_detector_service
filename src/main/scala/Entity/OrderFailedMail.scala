package Entity

case class OrderFailedMail(userId: String,
                           orderId: String,
                           orderNumber: String,
                           generatedOrderNumber: String,
                           vendor: String,
                           serviceName: String,
                           errorMessage: String,
                           status: String,
                           dateTime: String
                          )

