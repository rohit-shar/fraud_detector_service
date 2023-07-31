package actor


import Entity.CreateOrderResponse
import actor.OrderResponseDecisionActor.{OrderAccepted, OrderDeclined, OrderReview}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import client._
import database.FraudDetectionDatabaseService
import objects.FraudActor._
import service.SiftScienceEventService
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

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

  /**
   * Defines the behavior of the actor when it receives messages.
   * In this case, it handles OrderRequestReceiveCommand messages.
   */
  override def receive: Receive = {
    case OrderRequestReceiveCommand(orderNumber, order) =>
      log.info(s"ORDER REQUEST RECEIVED FOR ORDER: $orderNumber")

      // Check if the order already exists in the database
      if (!fraudDetectionDatabaseService.isOrderAlreadyExists(orderNumber)) {
        // If order does not exist, save it into the database
        try {
          // Save the order in the database
          fraudDetectionDatabaseService.saveFraudDetectionRecord(order)
          log.info(s"Order $orderNumber saved successfully.")
          // Respond to the sender with the orderNumber to indicate successful processing
          // FIRST CALL THE CUSTOMER UUID API IF ITS NOT NULL, THEN WE NEED TO CALL THE SIFT API
          val siftScienceEventService = new SiftScienceEventService()
          val orderResponse: CreateOrderResponse = siftScienceEventService.createOrderEvent(order)

          if (orderResponse != null && orderResponse.exception == null) {
            log.info("RECEIVED THE ORDER RESPONSE")
            if (siftScienceEventService.fraudResponse.orderStatus.equals("DECLINED")) {
              orderResponseDecisionActor ? OrderDeclined(orderResponse)
            }
            else if (siftScienceEventService.fraudResponse.orderStatus.equals("REVIEW")) {
              orderResponseDecisionActor ? OrderReview(orderResponse)
            }
            else if (siftScienceEventService.fraudResponse.orderStatus.equals("ACCEPTED")) {
              orderResponseDecisionActor ? OrderAccepted(orderResponse)
            }
          }


          sender() ! orderNumber
        } catch {
          case ex: Exception =>
            // Handle the exception if there's an error while saving the order
            log.error(s"Failed to save order $orderNumber: ${ex.getMessage}")
            // Respond to the sender with None to indicate failure
            sender() ! None
        }
      } else {
        // If the order already exists in the database, inform the sender
        log.warning(s"Order $orderNumber already exists in the database.")
        // Respond to the sender with None to indicate that the order is a duplicate
        var futureResponse = EmailSender.sendFailEmail("duplicateOrder")
        futureResponse.map { response =>
          if (response.status == 200)
            log.info("Successfully sent email")
          else {
            log.info("Not able to send failure email ")
            log.info("ERROR RESPONSE", response.httpMessage)
          }
        }
        sender() ! None
      }
  }
}
