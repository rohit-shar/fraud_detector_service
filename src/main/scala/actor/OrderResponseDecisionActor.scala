package actor

import Entity.{FraudDetectionStates, OrderFailedMail}
import akka.actor.Actor
import akka.http.scaladsl.model.HttpResponse
import client.EmailSender._
import com.typesafe.scalalogging.Logger
import database.FraudDetectionDatabaseService
import model.FraudResponse
import model.OrderModels.{CartEntryDetail, Order, SuccessOrderEmailRequest}

import java.util
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success}

object OrderResponseDecisionActor {
  case class OrderAccepted(fraudResponse: FraudResponse)

  case class OrderDeclined(fraudResponse: FraudResponse)

  case class OrderReview(fraudResponse: FraudResponse)

  case class OrderEmailSent(var orderNumber: String, var status: Int)

  case class DuplicateOrder(order: Order)

  case class SiftFailureResponse(fraudResponse: FraudResponse)
}

class OrderResponseDecisionActor extends Actor {

  import OrderResponseDecisionActor._

  var fraudDetectionDatabaseService = new FraudDetectionDatabaseService
  var mailResponse = OrderEmailSent(null, 0)
  var reason = ""
  var logger = Logger("OrderResponseDecisionActor")

  override def receive: Receive = {

    case OrderAccepted(fraudResponse: FraudResponse) => {
      logger.info(s"ORDER RECEIVED WITH ORDER STATUS ${fraudResponse.orderStatus} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      reason = s"Order with order number #${fraudResponse.orderNumber} is Accepted"
      var updateCount = fraudDetectionDatabaseService.updateExistingFraudEntryRecord(fraudResponse)
      if (updateCount == 0) {
        logger.info("FRAUD RECORD NOT UPDATED IN DATABASE")
        logger.info("NO. OF FRAUD RECORDS UPDATED IN DATABASE ARE : " + updateCount)
      }
      var shippingCost = 0.0;
      fraudResponse.order.cart.cart_entries.values.foreach(value => shippingCost = shippingCost + value.shipping)
      var cartDetailList: util.ArrayList[CartEntryDetail] = new util.ArrayList[CartEntryDetail]
      var cartEntryList = fraudResponse.order.cart.cart_entries.values.toList
      cartEntryList.foreach(cartEntry => {
        var cartEntryDetail = CartEntryDetail(cartEntry.imageUrl, cartEntry.title, cartEntry.price.toString, cartEntry.quantity.toString, cartEntry.product_url, cartEntry.partType, cartEntry.core_deposit.toString)
        cartDetailList.add(cartEntryDetail)
      })
      var myCartDetailsList: List[CartEntryDetail] = cartDetailList.asScala.toList
      var orderSuccessMail = SuccessOrderEmailRequest("sanwalrohit333@gmail.com",
        "shivaysharma@gmail.com",
        "kakaji@gmail.com",
        "test@gmail.com",
        "ORDER ACCEPTED",
        fraudResponse.order.userName,
        "",
        fraudResponse.orderNumber,
        myCartDetailsList,
        fraudResponse.order.cart.cart_total.toString,
        fraudResponse.order.salesTax.toString,
        shippingCost.toString,
        fraudResponse.order.total_payble.toString,
        "NA", // add alt phone no. field below
        fraudResponse.order.billingAddress.address,
        fraudResponse.order.billingAddress.city,
        fraudResponse.order.billingAddress.state,
        fraudResponse.order.billingAddress.zip,
        fraudResponse.order.billingAddress.country,
        fraudResponse.order.total_payble.toString,
        fraudResponse.order.creditCardLastFourDigit, "VISA")
      var mailFuture = sendAcceptedEmail(orderSuccessMail)
      var st = 0
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          st = value.status.intValue()
          if (value.status.intValue() == 200) {
            logger.info("SUCCESSFULLY SENT EMAIL FOR ACCEPTED ORDER")
            println(s"STATUS CODE RETRIEVED IS : $st")
            println("wait here")
          }
          else {
            logger.info("EMAIL NOT SENT FOR DENIED ORDER")
            println(s"RESPONSE CODE IS: $st")
          }
        case Failure(exception: Throwable) =>
          logger.info("ERROR WHILE CALLING EMAIL API")
          logger.info(s"ERROR MESSAGE : ${exception.getCause}")
      }
      sender() ! OrderEmailSent(fraudResponse.orderNumber, st)
    }


    case OrderDeclined(fraudResponse: FraudResponse) => {
      logger.info(s"ORDER RECEIVED WITH ORDER STATUS ${fraudResponse.orderStatus} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      reason = s"Order with order number #${fraudResponse.orderNumber} is denied"
      var updateCount = fraudDetectionDatabaseService.updateExistingFraudEntryRecord(fraudResponse)
      if (updateCount == 0) {
        logger.info("FRAUD RECORD NOT UPDATED IN DATABASE")
        logger.info("NO. OF FRAUD RECORDS UPDATED IN DATABASE ARE : " + updateCount)
      }
      var orderFailedMail = OrderFailedMail(fraudResponse.userId, fraudResponse.orderId, fraudResponse.orderNumber, "NA", "NA", "fraud-detector-service", reason, fraudResponse.orderStatus, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      var st = 0
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          st = value.status.intValue()
          if (value.status.intValue() == 200) {
            logger.info("SUCCESSFULLY SENT EMAIL FOR DENIED ORDER")
            println(s"STATUS CODE RETRIEVED IS : $st")
            println("wait here")
          }
          else {
            logger.info("EMAIL NOT SENT FOR DENIED ORDER")
            println(s"RESPONSE CODE IS: $st")
          }
        case Failure(exception: Throwable) =>
          logger.info("ERROR WHILE CALLING EMAIL API")
          logger.info(s"ERROR MESSAGE : ${exception.getCause}")
      }
      sender() ! OrderEmailSent(fraudResponse.orderNumber, st)
    }


    case DuplicateOrder(order: Order) => {
      logger.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.DUPLICATE_ORDER} WITH ORDER NUMBER #${order.orderNumber}")
      reason = s"Order with order number #${order.orderNumber} is accepted"
      var orderFailedMail = OrderFailedMail(order.user_id, order.order_id.toString, order.orderNumber, "NA", "NA", "fraud-detector-service", reason, FraudDetectionStates.DUPLICATE_ORDER, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      var st = 0
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          st = value.status.intValue()
          if (value.status.intValue() == 200) {
            logger.info("SUCCESSFULLY SENT EMAIL FOR DUPLICATE ORDER")
            logger.info(s"STATUS CODE RETRIEVED IS : $st")
          }
          else {
            logger.info("EMAIL NOT SENT FOR DUPLICATE ORDER")
            logger.info(s"RESPONSE CODE IS: $st")
          }
        case Failure(exception: Throwable) =>
          logger.info("ERROR WHILE CALLING EMAIL API")
          logger.info(s"ERROR MESSAGE :  ${exception.getCause}")
      }
      sender() ! OrderEmailSent(order.orderNumber, st)
    }

    case SiftFailureResponse(fraudResponse: FraudResponse) =>
      logger.info(s"ORDER RECEIVED WITH ORDER STATUS ${fraudResponse.orderStatus} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      reason = s"Error from sift api, not able to accept order"
      var orderFailedMail = OrderFailedMail(fraudResponse.userId, fraudResponse.orderId, fraudResponse.orderNumber, "NA", "NA", "fraud-detector-service", reason, fraudResponse.orderStatus, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      var st = 0
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          st = value.status.intValue()
          if (value.status.intValue() == 200) {
            logger.info("SUCCESSFULLY SENT EMAIL FOR ORDER FAILURE")
            println(s"STATUS CODE RETRIEVED IS : $st")
          }
          else {
            logger.info("EMAIL NOT SENT FOR FAILED ORDER")
            logger.info(s"Response Code Is: $st")
          }
        case Failure(exception: Throwable) =>
          logger.info("ERROR WHILE CALLING EMAIL API")
          logger.info(s"ERROR MESSAGE : ${exception.getCause}")
      }
      sender() ! OrderEmailSent(fraudResponse.orderNumber, st)
  }
}
