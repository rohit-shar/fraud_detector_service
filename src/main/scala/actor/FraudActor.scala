package actor

import akka.actor.{Actor, ActorLogging}
import database.FraudDetectionDatabaseService
import objects.FraudActor._

class FraudActor extends Actor with ActorLogging {
  private val fraudDetectionDatabaseService = new FraudDetectionDatabaseService()

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
        sender() ! None
        // You can handle this scenario as needed, such as sending a duplicate order response back
      }
  }
}
