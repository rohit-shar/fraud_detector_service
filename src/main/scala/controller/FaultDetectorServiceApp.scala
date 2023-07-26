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
import json.OrderJsonProtocol
import model.OrderModels.Order
import objects.FraudActor.OrderRequestReceiveCommand
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
  // import actorSystem.dispatcher
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object FaultDetectorServiceApp extends App with OrderJsonProtocol {

  // Initialize Actor System, Materializer, and Timeout
  implicit val actorSystem = ActorSystem("FaultDetectorActorSystem")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val timeout = Timeout.durationToTimeout(2 seconds)

  // Bind the HTTP server to localhost on port 8080
  val serverSource = Http().bind("localhost", 8080)

  // Create the Fraud Actor
  val fraudActor = actorSystem.actorOf(Props[FraudActor])

  // Define a handler for incoming HTTP requests
  val faultDetectorController: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.POST, Uri.Path("/fraud-detect"), _, entity, _) =>
      // Extract the JSON content from the request entity
      val strictEntity = entity.toStrict(3 seconds)
      val jsonContentFuture = strictEntity.map(myEntity => myEntity.data.utf8String)

      // Parse the JSON content into an Order object
      val responseFuture = jsonContentFuture.map {
        jsonContent =>
          jsonContent.parseJson.convertTo[Order]
      }

      // Process the Order and send a message to the Fraud Actor
      responseFuture.map {
        order =>
          // CODE OF INTERNAL LOGIC
          fraudActor ? OrderRequestReceiveCommand(order.orderNumber, order)

          // Respond with a success message in the HTTP response
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              s"""
                 |{
                 |"Message":"FraudService Received order verification request. Order number is : ${order.orderNumber}"
                 |}
                 |""".stripMargin
            )
          )
      }
  }

  // Define a sink to handle incoming connections
  val connectionSink = Sink.foreach[IncomingConnection] {
    incomingConnection => incomingConnection.handleWithAsyncHandler(faultDetectorController)
  }

  // Link the server source to the connection sink to start the server
  serverSource.to(connectionSink).run()
}
