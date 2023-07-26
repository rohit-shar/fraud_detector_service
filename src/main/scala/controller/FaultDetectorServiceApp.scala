package controller

import actor.FraudActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import json.OrderJsonProtocoal
import model.OrderModels.Order
import objects.FraudActor.OrderRequestReceiveCommand
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object FaultDetectorServiceApp extends App with OrderJsonProtocoal {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val actorSystem = ActorSystem("FaultDetectorActorSystem")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val timeout = Timeout.durationToTimeout(2 seconds)
  val serverSource = Http().bind("localhost", 8080)
  val fraudActor = actorSystem.actorOf(Props[FraudActor])

  import actorSystem.dispatcher

  //   REQUEST HANDLER
  val faultDetectorController: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.POST, Uri.Path("/fraud-detect"), _, entity, _) =>
      val strictEntity = entity.toStrict(3 seconds)
      val jsonContentFuture = strictEntity.map(myEntity => myEntity.data.utf8String)
      val responseFuture = jsonContentFuture.map { // getting the guitar future
        jsonContent =>
          jsonContent.parseJson.convertTo[Order]
      }
      responseFuture.map {
        order =>
          // CODE OF INTERNAL LOGIC
          fraudActor ? OrderRequestReceiveCommand(order.orderNumber, order)
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              """
                |{
                |"Message":"SAVED THE GUITAR RECORD SUCCESSFULY"
                |}
                |""".stripMargin
            )
          )
      }
  }

  // CONNECTION SINK RECEIVER
  val connectionSink = Sink.foreach[IncomingConnection] {
    incomingConnection => incomingConnection.handleWithAsyncHandler(faultDetectorController)
  }

  // LINKING THE REQUET SOURCE WITH SINK
  serverSource.to(connectionSink).run()


}
