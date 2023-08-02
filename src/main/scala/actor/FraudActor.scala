package actor


import Entity.{CreateOrderResponse, FraudDetectionStates}
import actor.OrderResponseDecisionActor._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import database.FraudDetectionDatabaseService
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

  /**
   * Defines the behavior of the actor when it receives messages.
   * In this case, it handles OrderRequestReceiveCommand messages.
   */
  override def receive: Receive = {

    case OrderRequestReceiveCommand(orderNumber, order) =>
      log.info(s"ORDER REQUEST RECEIVED FOR ORDER: $orderNumber")
      if (!fraudDetectionDatabaseService.isOrderAlreadyExists(orderNumber)) {

        fraudDetectionDatabaseService.saveFraudDetectionRecord(order)
        log.info(s"Order $orderNumber saved successfully.")
        val orderResponse: CreateOrderResponse = siftScienceEventService.createOrderEvent(order)
        if (orderResponse != null && orderResponse.exception == null) {
          log.info("RECEIVED THE ORDER RESPONSE")
          if (siftScienceEventService.fraudResponse.orderStatus.equals(FraudDetectionStates.ORDER_DENIED)) {
            orderResponseDecisionActor ? OrderDeclined(siftScienceEventService.fraudResponse)
          }
          else if (siftScienceEventService.fraudResponse.orderStatus.equals(FraudDetectionStates.ORDER_IN_REVIEW)) {
            orderResponseDecisionActor ? OrderReview(siftScienceEventService.fraudResponse)
          }
          else if (siftScienceEventService.fraudResponse.orderStatus.equals(FraudDetectionStates.ORDER_ACCEPTED)) {
            orderResponseDecisionActor ? OrderAccepted(siftScienceEventService.fraudResponse)
          }
        }
        else {
          orderResponseDecisionActor ? SiftFailureResponse(siftScienceEventService.fraudResponse)
        }
        sender() ! orderNumber
      }
      else {
        log.info(s"DUPLICATE ORDER REQUEST RECEIVED FOR ORDER NUMBER #$orderNumber")
        log.warning(s"Order $orderNumber already exists in the database.")
        reason = s"Order with order number #${orderNumber} already exists in database and already processed"
        var orderEmailSent = (orderResponseDecisionActor ? DuplicateOrder(order)).mapTo[OrderEmailSent]
        sender() ! None
      }

  }


}

