package actor

import akka.actor.{Actor, ActorLogging}
import database.FraudDetectionDatabaseService
import objects.FraudActor._

class FraudActor extends Actor with ActorLogging {
  val fraudDetectionDatabaseService = new FraudDetectionDatabaseService()

  override def receive: Receive = {
    case OrderRequestReceiveCommand(orderNumber, order) => {
      log.info("ORDER REQUEST RECEIVED FOR ORDER : ", order)

      if (!fraudDetectionDatabaseService.isOrderAlreadyExists(orderNumber)) { // If order does not exists already
        // SAVE THE ORDER INTO THE DATABASE
        fraudDetectionDatabaseService.saveFraudDetectionRecord(order)
      }
      else {
        // IN THIS CASE IF THE FraudRecordEntry already exists then we will call fail email service of Akshay
      }


    }
  }
}
