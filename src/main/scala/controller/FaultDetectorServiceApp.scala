package controller

import Entity.FraudServiceResponseEntity
import actor.FraudActor
import akka.actor.Status.Failure
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.StatusReply.Success
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import json.OrderJsonProtocol
import model.OrderModels.Order
import objects.FraudActor.OrderRequestReceiveCommand
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object FaultDetectorServiceApp extends App with OrderJsonProtocol with SprayJsonSupport {

  implicit val actorSystem = ActorSystem("FaultDetectorActorSystem")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val timeout = Timeout.durationToTimeout(2 seconds)

  val fraudActor = actorSystem.actorOf(Props[FraudActor])

  val faultDetectorRouteController = {
    path("fraud-detect") {
      post {
        entity(as[Order]) { orderRequest =>
          val orderReceivedResponse = fraudActor ? OrderRequestReceiveCommand(orderRequest.orderNumber, orderRequest)

          complete(orderReceivedResponse.map { orderNumber =>
            orderNumber match {
              case None =>
                var fraudServiceResponse: FraudServiceResponseEntity = FraudServiceResponseEntity(s"""Message": "The fraud record is already present for order number : ${orderRequest.orderNumber}""")
                HttpResponse(
                  StatusCodes.BadRequest,
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    fraudServiceResponse.toJson.prettyPrint
                  )
                )

              case orderNumber =>
                var fraudResponse: FraudServiceResponseEntity = FraudServiceResponseEntity(s"""Message": "FraudService Received order verification request. Order number is : ${orderNumber}""")
                HttpResponse(
                  StatusCodes.OK,
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    fraudResponse.toJson.prettyPrint
                  )
                )

            }
          })
        }
      }
    }
  }
  Http().bindAndHandle(faultDetectorRouteController, "localhost", 8080)
}
