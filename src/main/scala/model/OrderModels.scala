package model

import java.util.UUID

object OrderModels {
  case class CartItem(
                       `$item_id`: String,
                       `$product_title`: String,
                       `$price`: Double,
                       `$currency_code`: String = "USD",
                       //                     `"$upc"`: String,
                       `$sku`: String,
                       `$brand`: String,
                       `$manufacturer`: String,
                       `$category`: String,
                       `$tags`: List[String],
                       `$color`: String,
                       `$quantity`: Int
                     )

  case class BillingShippingAddress(`$name`: String, `$phone`: String, `$address_1`: String, `$address_2`: String, `$city`: String, `$region`: String, `$country`: String, `$zipcode`: String)

  case class PaymentInfo(`$payment_type`: String = "$credit_card", `$payment_gateway`: String = "authorize.net", `$card_bin`: String = "", `$card_last4`: String = "")

  //  case class CreateOrderRequestBody(`$type`: String, `$api_key`: String, `$user_id`: String, `$session_id`: String, var `user_order_id`: String, `$user_email`: String = "", var `$order_id`: String = "", var `$amount`: Double = 0,
  //                                    var `$currency_code`: String = "USD", `$billing_address`: BillingShippingAddress, `$shipping_address`: BillingShippingAddress, `$payment_methods`: List[PaymentInfo], `$items`: List[CartItem], `$ip`: String)
  //
  //  case class OrderTransactionRequestBody(`$type`: String, `$api_key`: String, `$user_id`: String, var `$user_email`: String = "", `$transaction_type`: String, `$transaction_status`: String, `$seller_user_id`: String, var `$order_id`: String = "", var `user_order_id`: String, var `$transaction_id`: String, var `$amount`: Double = 0,
  //                                         var `$session_id`: String, var `$currency_code`: String = "USD", `$billing_address`: BillingShippingAddress, `$shipping_address`: BillingShippingAddress, `$payment_method`: PaymentInfo, `$ip`: String, `avs_status`: String)

  case class AbuseBody(score: Double, reason: List[ReasonBody])

  case class ReasonBody(name: String, value: String)

  case class AbuseScoreResponse(var status: Int, var `error_message`: String, var time: Long, var request: String, var `score_response`: ScoreResponse)

  case class ScoreResponse(status: Int, `error_message`: String, scores: Map[String, AbuseBody], `user_id`: String)

  case class AbuseScoreResponseWithAverageScore(var status: Int,
                                                var `error_message`: String,
                                                var time: Long,
                                                var request: String,
                                                var `score_response`: ScoreResponse,
                                                var `average_score`: Double = 0.0,
                                                var `current_state`: String = "None")

  case class FinalResponse(var `customer_UUID`: String,
                           var `order_UUID`: String,
                           var `average_score`: Double = 0.0,
                           var order: Order)

  case class CartEntry(sku_id: String,
                       quantity: Int,
                       price: Double,
                       title: String,
                       imageUrl: String,
                       core_deposit: Double,
                       distributor_group: String,
                       product_url: String,
                       fitmentuid: String,
                       brand: String,
                       category: String,
                       partType: String,
                       shipping: Double = 0.0,
                       customField1: Double = 0.0,
                       customField2: Double = 0.0,
                       customField3: String = "",
                       customField4: String = "",
                       customField5: Boolean = false,
                       primaryId: String = ""
                      )


  case class Cart(var cart_id: UUID,
                  var user_id: String,
                  var cart_entries: Map[String, CartEntry],
                  var total_numberOf_items: Int,
                  var cart_total: Double,
                  var core_deposit_total: Double)

  // customField3 is used as alt contact number
  case class ShipToGroup(var customerUUID: String,
                         var shipToGroupUUID: String,
                         var name: String,
                         var address: String,
                         var address2: String,
                         var companyName: String,
                         var phoneNumber: String,
                         var city: String,
                         var state: String,
                         var zip: String,
                         var country: String,
                         var landmark: String,
                         var addressType: Int,
                         var emailId: String,
                         var addedAsBillingAddress: Boolean,
                         var usedAsBillingAddress: Boolean,
                         var isDefault: Boolean,
                         var customField1: Double = 0.0,
                         var customField2: Double = 0.0,
                         var customField3: String = "",
                         var customField4: String = "",
                         var customField5: Boolean = false)

  case class ShippingAddressPerItem(var sku_id: String, var shipping_address: ShipToGroup)

  case class PaymentResponse(order_id: UUID,
                             payment_uuid: UUID,
                             paymentGatewayResponseCode: String,
                             paymentGatewayResponseText: String,
                             paymentTransactionStatus: String,
                             payment_id: UUID,
                             transaction_id: String,
                             paypalPaymentId: Option[String],
                             paypalPayerId: Option[String],
                             paypalInvoiceId: Option[String],
                             paymentGateway: Option[String])

  case class HttpData(
                       userIP: String,
                       sessionID: String
                     )

  case class Order(var order_id: UUID,
                   var user_id: String,
                   var orderNumber: String,
                   var orderDate: String,
                   var userName: String,
                   var listOfShippingAddressPerItem: List[ShippingAddressPerItem],
                   var billingAddress: ShipToGroup,
                   var paymentResponse: PaymentResponse,
                   var creditCardLastFourDigit: String,
                   var creditCardFirstSixDigit: String,
                   var cart: Cart,
                   var salesTax: Double,
                   var total_payble: Double,
                   var orderStatus: Boolean,
                   var emailId: String,
                   var httpData: HttpData,
                   var customField1: Double = 0.0,
                   var customField2: Double = 0.0,
                   var customField3: String = "",
                   var customField4: String = "",
                   var customField5: Boolean = false
                  )

  case class Orders(orders: List[Order])

}
