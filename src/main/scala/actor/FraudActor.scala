package actor

import akka.actor.{Actor, ActorLogging}
import objects.FraudActor._

class FraudActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case OrderRequestReceiveCommand(orderNumber, jsonPayload) => {

    }
  }
}
