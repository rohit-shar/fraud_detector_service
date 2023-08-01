package actor

import Entity.{FraudDetectionStates, OrderFailedMail}
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.HttpResponse
import client.EmailSender._
import database.FraudDetectionDatabaseService
import model.FraudResponse
import model.OrderModels.Order

import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object OrderResponseDecisionActor {
  case class OrderAccepted(fraudResponse: FraudResponse)

  case class OrderDeclined(fraudResponse: FraudResponse)

  case class OrderReview(fraudResponse: FraudResponse)

  case class OrderEmailSent(var orderNumber: String, var status: Int)

  case class DuplicateOrder(order: Order)
}

class OrderResponseDecisionActor extends Actor with ActorLogging {

  import OrderResponseDecisionActor._

  var fraudDetectionDatabaseService = new FraudDetectionDatabaseService
  var mailResponse = OrderEmailSent(null, 0)
  var reason = ""

  override def receive: Receive = {

    case OrderAccepted(fraudResponse: FraudResponse) => {
      log.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.ORDER_ACCEPTED} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      reason = s"Order with order number #${fraudResponse.orderNumber} is accepted"
      var updateCount = fraudDetectionDatabaseService.updateExistingFraudEntryRecord(fraudResponse)
      if (updateCount == 0) {
        log.warning("FRAUD RECORD NOT UPDATED IN DATABASE")
      }
      var orderFailedMail = OrderFailedMail(fraudResponse.userId, fraudResponse.orderId, fraudResponse.orderNumber, "NA", "NA", "fraud-detector-service", reason, fraudResponse.orderStatus, new Date(System.currentTimeMillis()).toGMTString)

      var status = 0
      sendFailEmail(orderFailedMail).onComplete {
        case Success(value: HttpResponse) =>
          if (value.status.intValue() == 200) {
            log.info("SUCCESSFULLY SENT EMAIL FOR ACCEPTED ORDER")
            println("STATUS CODE RETRIEVED IS : ", value.status.intValue())
            println("wait here")
          }
          else {
            log.info("EMAIL NOT SENT FOR ACCEPTED ORDER")
            println("RESPONSE CODE IS: ", value.status.intValue())
          }
        case Failure(exception: Throwable) =>
          log.info("ERROR WHILE CALLING EMAIL API")
          log.info("ERROR MESSAGE : ", exception.getCause)
      }
      sender() ! OrderEmailSent(fraudResponse.orderNumber, status)
    }


    case OrderDeclined(fraudResponse: FraudResponse) => {
      log.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.ORDER_DENIED} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      reason = s"Order with order number #${fraudResponse.orderNumber} is denied"
      var updateCount = fraudDetectionDatabaseService.updateExistingFraudEntryRecord(fraudResponse)
      if (updateCount == 0) {
        log.warning("FRAUD RECORD NOT UPDATED IN DATABASE")
        log.info("NO. OF FRAUD RECORDS UPDATED IN DATABASE ARE : ", updateCount)
      }
      var orderFailedMail = OrderFailedMail(fraudResponse.userId, fraudResponse.orderId, fraudResponse.orderNumber, "NA", "NA", "fraud-detector-service", reason, fraudResponse.orderStatus, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      var status = 0
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          status = value.status.intValue()
          if (value.status.intValue() == 200) {
            log.info("SUCCESSFULLY SENT EMAIL FOR DENIED ORDER")
            println("STATUS CODE RETRIEVED IS : ", value.status.intValue())
            println("wait here")
          }
          else {
            log.info("EMAIL NOT SENT FOR DENIED ORDER")
            println("RESPONSE CODE IS: ", value.status.intValue())
          }
        case Failure(exception: Throwable) =>
          log.info("ERROR WHILE CALLING EMAIL API")
          log.info("ERROR MESSAGE : ", exception.getCause)
      }
      sender() ! OrderEmailSent(fraudResponse.orderNumber, status)
    }


    case DuplicateOrder(order: Order) => {
      log.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.DUPLICATE_ORDER} WITH ORDER NUMBER #${order.orderNumber}")
      reason = s"Order with order number #${order.orderNumber} is accepted"
      var orderFailedMail = OrderFailedMail(order.user_id, order.order_id.toString, order.orderNumber, "NA", "NA", "fraud-detector-service", reason, FraudDetectionStates.DUPLICATE_ORDER, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      var status = 0
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          status = value.status.intValue()
          if (value.status.intValue() == 200) {
            log.info("SUCCESSFULLY SENT EMAIL FOR DUPLICATE ORDER")
            println("STATUS CODE RETRIEVED IS : ", value.status.intValue())
            println("wait here")
          }
          else {
            log.info("EMAIL NOT SENT FOR DUPLICATE ORDER")
            println("RESPONSE CODE IS: ", value.status.intValue())
          }
        case Failure(exception: Throwable) =>
          log.info("ERROR WHILE CALLING EMAIL API")
          log.info("ERROR MESSAGE : ", exception.getCause)
      }
      sender() ! OrderEmailSent(order.orderNumber, status)
    }


  }
}
