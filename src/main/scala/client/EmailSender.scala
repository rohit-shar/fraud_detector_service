package client

import Entity.OrderFailedMail
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import json.OrderJsonProtocol
import utils.HttpUtil.EMAIL_SERVICE_URL
import spray.json._

import scala.concurrent.Future
import akka.util.Timeout
import model.OrderModels.SuccessOrderEmailRequest

import scala.concurrent.duration.DurationInt
import scala.language.{implicitConversions, postfixOps}

//"localhost:8082/api/sendEmail"
object EmailSender extends OrderJsonProtocol with DefaultJsonProtocol {
  implicit val actorSystem = ActorSystem("FaultDetectorActorSystem")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val DUPLICATE_ORDER = "duplicateOrder"
  implicit val timeout = Timeout.durationToTimeout(5 seconds)

  import actorSystem.dispatcher

  def sendFailEmail(orderFailedMail: OrderFailedMail): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = EMAIL_SERVICE_URL,
      method = HttpMethods.POST,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        orderFailedMail.toJson.prettyPrint
      )
    )
    );
  }

  def sendAcceptedEmail(orderAcceptedMail: SuccessOrderEmailRequest): Future[HttpResponse] = {

    Http().singleRequest(HttpRequest(uri = EMAIL_SERVICE_URL,
      method = HttpMethods.POST,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        orderAcceptedMail.toJson.prettyPrint
      )
    )
    );
  }
}
