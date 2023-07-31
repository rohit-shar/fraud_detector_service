package actor

import Entity.CreateOrderResponse
import akka.actor.{Actor, ActorLogging}

object OrderResponseDecisionActor {
  case class OrderAccepted(orderResponse: CreateOrderResponse)

  case class OrderDeclined(orderResponse: CreateOrderResponse)

  case class OrderReview(orderResponse: CreateOrderResponse)
}

class OrderResponseDecisionActor extends Actor with ActorLogging {

  import OrderResponseDecisionActor._
  import service.SiftScienceEventService

  var siftScienceEventService = new SiftScienceEventService()

  override def receive: Receive = {
    case OrderAccepted(orderResponse: CreateOrderResponse) => {
      // write the code here to update the database table and sending the mail of acceptance

    }

    case OrderDeclined(orderResponse: CreateOrderResponse) => {

    }
  }
}
