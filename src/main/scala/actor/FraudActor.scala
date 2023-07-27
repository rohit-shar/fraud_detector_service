package actor


import akka.actor.{Actor, ActorLogging}
import database.FraudDetectionDatabaseService
import objects.FraudActor._
import client._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class FraudActor extends Actor with ActorLogging {
  private val fraudDetectionDatabaseService = new FraudDetectionDatabaseService()
  var customerUUID = null

  override def receive: Receive = {
    case OrderRequestReceiveCommand(orderNumber, order) =>
      log.info(s"ORDER REQUEST RECEIVED FOR ORDER: $orderNumber")

      if (!fraudDetectionDatabaseService.isOrderAlreadyExists(orderNumber)) {
        // If order does not exist, save it into the database
        try {
          fraudDetectionDatabaseService.saveFraudDetectionRecord(order)
          log.info(s"Order $orderNumber saved successfully.")


          sender() ! orderNumber
        } catch {
          case ex: Exception =>
            log.error(s"Failed to save order $orderNumber: ${ex.getMessage}")
            sender() ! None
          // You can handle the error here, such as sending a failure response back
        }
      } else {
        log.warning(s"Order $orderNumber already exists in the database.")
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
        // You can handle this scenario as needed, such as sending a duplicate order response back
      }
  }
}
