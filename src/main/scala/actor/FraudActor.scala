package actor


import Entity.{CreateOrderResponse, FraudDetectionStates}
import actor.OrderResponseDecisionActor._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import database.FraudDetectionDatabaseService
import model.OrderModels.FraudApplicationResponse
import net.liftweb.json.Serialization
import objects.FraudActor._
import service.SiftScienceEventService

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Success

/**
 * An advanced level FraudActor that processes incoming OrderRequestReceiveCommand messages.
 * This actor checks if the order already exists in the database and saves it if not.
 *
 * @constructor Creates a new FraudActor with a FraudDetectionDatabaseService instance.
 */
class FraudActor extends Actor with ActorLogging {
  // Create an instance of the FraudDetectionDatabaseService
  private val fraudDetectionDatabaseService = new FraudDetectionDatabaseService()
  var customerUUID = null
  var system = ActorSystem()
  var orderResponseDecisionActor = system.actorOf(Props[OrderResponseDecisionActor])
  implicit val timeout = Timeout.durationToTimeout(2 seconds)
  var reason = ""
  val siftScienceEventService = new SiftScienceEventService()
  val fraudApplicationResponse = FraudApplicationResponse("", "", "")

  /**
   * Defines the behavior of the actor when it receives messages.
   * In this case, it handles OrderRequestReceiveCommand messages.
   */
  override def receive: Receive = {

    case OrderRequestReceiveCommand(orderNumber, order) =>
      log.info(s"ORDER REQUEST RECEIVED FOR ORDER: $orderNumber")

      /**
       * The case in which the request is received for new order
       * In this case, we will update the database as well as call the sift api for getting abuse score
       */
      if (!fraudDetectionDatabaseService.isOrderAlreadyExists(orderNumber)) {
        fraudDetectionDatabaseService.saveFraudDetectionRecord(order)
        log.info(s"Order $orderNumber saved successfully.")
        val orderResponse: CreateOrderResponse = siftScienceEventService.createOrderEvent(order)

        /**
         * The case in which we get the successful response from Sift api without any exception is below
         */
        if (orderResponse != null && orderResponse.exception == null) {
          log.info("RECEIVED THE ORDER RESPONSE")
          if (siftScienceEventService.fraudResponse.orderStatus.equals(FraudDetectionStates.ORDER_DENIED)) { // FOR DENIED ORDER
            orderResponseDecisionActor ? OrderDeclined(siftScienceEventService.fraudResponse)
          }


          else if (siftScienceEventService.fraudResponse.orderStatus.equals(FraudDetectionStates.ORDER_IN_REVIEW)) { // FOR ORDER IN REVIEW
            orderResponseDecisionActor ? OrderReview(siftScienceEventService.fraudResponse)
          }


          else if (siftScienceEventService.fraudResponse.orderStatus.equals(FraudDetectionStates.ORDER_ACCEPTED)) { // FOR ACCEPTED ORDER
            fraudApplicationResponse.orderStatus = siftScienceEventService.fraudResponse.orderStatus
            fraudApplicationResponse.message = "ORDER ACCEPTED SUCCESSFULLY"
            var mailResponse = orderResponseDecisionActor ? OrderAccepted(siftScienceEventService.fraudResponse)
            val result = Await.result(mailResponse, timeout.duration)
            mailResponse.onComplete {
              case Success(value: OrderEmailSent) =>
                fraudApplicationResponse.emailStatus = if (value.status == 200) "EMAIL SENT SUCCESSFULLY" else "EMAIL NOT SENT"
            }

          }
        }

        /**
         * The case in which we get don't get the correct response from Sift api
         */
        else {
          (orderResponseDecisionActor ? SiftFailureResponse(siftScienceEventService.fraudResponse)).mapTo[OrderEmailSent]
          fraudApplicationResponse.message = if (siftScienceEventService.fraudResponse.errorMessage == null) "NA" else siftScienceEventService.fraudResponse.errorMessage
          fraudApplicationResponse.orderStatus = siftScienceEventService.fraudResponse.orderStatus
        }

      }


      /**
       * The case in which the requested order is duplicate.
       * In this case, we will send the duplicate email notification
       */
      else {
        log.info(s"DUPLICATE ORDER REQUEST RECEIVED FOR ORDER NUMBER #$orderNumber")
        log.warning(s"Order $orderNumber already exists in the database.")
        fraudApplicationResponse.message = s"Order with order number #${orderNumber} already exists in database and already processed"
        fraudApplicationResponse.orderStatus = "DuplicateOrder"
        var mailResponse = orderResponseDecisionActor ? DuplicateOrder(order)
        val result = Await.result(mailResponse, timeout.duration)
        mailResponse.onComplete {
          case Success(value: OrderEmailSent) =>
            fraudApplicationResponse.emailStatus = if (value.status == 200) "EMAIL SENT SUCCESSFULLY" else "EMAIL NOT SENT"
        }
      }
      sender() ! fraudApplicationResponse

  }


}

