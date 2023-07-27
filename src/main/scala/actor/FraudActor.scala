package actor


import akka.actor.{Actor, ActorLogging}
import database.FraudDetectionDatabaseService
import objects.FraudActor._
import client._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

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
        var futureResponse = EmailSender.sendFailEmailDuplicateOrder()
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
