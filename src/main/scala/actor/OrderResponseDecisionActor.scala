package actor

import Entity.{FraudDetectionStates, OrderFailedMail}
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.HttpResponse
import database.FraudDetectionDatabaseService
import model.FraudResponse
import client.EmailSender._
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

  override def receive: Receive = {
    case OrderAccepted(fraudResponse: FraudResponse) => {
      log.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.ORDER_ACCEPTED} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      var updateCount = fraudDetectionDatabaseService.updateExistingFraudEntryRecord(fraudResponse)
      if (updateCount == 0) {
        log.warning("FRAUD RECORD NOT UPDATED IN DATABASE")
      }
      var orderFailedMail = OrderFailedMail(fraudResponse.userId, fraudResponse.orderId, fraudResponse.orderNumber, "NA", "NA", "fraud-detector-service", fraudResponse.errorMessage, fraudResponse.orderStatus, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      mailFuture.map {
        response => mailResponse.status = response.status.intValue()
      }
      mailResponse.orderNumber = fraudResponse.orderNumber
      sender() ! mailResponse
    }
    case OrderDeclined(fraudResponse: FraudResponse) => {
      log.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.ORDER_DENIED} WITH ORDER NUMBER #${fraudResponse.orderNumber}")
      var updateCount = fraudDetectionDatabaseService.updateExistingFraudEntryRecord(fraudResponse)
      if (updateCount == 0) {
        log.warning("FRAUD RECORD NOT UPDATED IN DATABASE")
        log.info("NO. OF FRAUD RECORDS UPDATED IN DATABASE ARE : ", updateCount)
      }
      var orderFailedMail = OrderFailedMail(fraudResponse.userId, fraudResponse.orderId, fraudResponse.orderNumber, "NA", "NA", "fraud-detector-service", fraudResponse.errorMessage, fraudResponse.orderStatus, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      mailFuture.map {
        response => mailResponse.status = response.status.intValue()
      }
      mailResponse.orderNumber = fraudResponse.orderNumber
      sender() ! mailResponse
    }
    case DuplicateOrder(order: Order) => {
      log.info(s"ORDER RECEIVED WITH ORDER STATUS ${FraudDetectionStates.DUPLICATE_ORDER} WITH ORDER NUMBER #${order.orderNumber}")
      var orderFailedMail = OrderFailedMail(order.user_id, order.order_id.toString, order.orderNumber, "NA", "NA", "fraud-detector-service", "", FraudDetectionStates.DUPLICATE_ORDER, new Date(System.currentTimeMillis()).toGMTString)
      var mailFuture = sendFailEmail(orderFailedMail)
      mailResponse.orderNumber = order.orderNumber
      mailFuture.onComplete {
        case Success(value: HttpResponse) =>
          log.info("SUCCESSFULLY COMPLETED DUPLICATE ORDER")
          mailResponse.status = value.status.intValue()
        case Failure(exception: Throwable) =>
          log.info("ERROR WHILE CALLING EMAIL API")
        //          log.info("ERROR MESSAGE : ", exception.getCause)
      }


      sender() ! mailResponse
    }


  }
}
