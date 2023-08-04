package controller

import actor.FraudActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import json.OrderJsonProtocol
import model.OrderModels.{FraudApplicationResponse, Order}
import objects.FraudActor.OrderRequestReceiveCommand
import spray.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
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
          val orderReceivedResponse = (fraudActor ? OrderRequestReceiveCommand(orderRequest.orderNumber, orderRequest)).mapTo[FraudApplicationResponse]
          complete(
            orderReceivedResponse.map { response =>
              var statusCode: Int = if (response.orderStatus.equals("DuplicateOrder")) {
                400
              }
              else if (response.orderStatus.equals("OrderAccepted")) {
                200
              }
              else {
                500
              }
              HttpResponse(
                StatusCode.int2StatusCode(statusCode),
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  response.toJson.prettyPrint
                )
              )
            }
          )
        }
      }
    }
  }
  Http().bindAndHandle(faultDetectorRouteController, "localhost", 8080)
}
