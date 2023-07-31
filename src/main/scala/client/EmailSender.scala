package client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import json.OrderJsonProtocol
import utils.HttpUtil.EMAIL_SERVICE_URL

import scala.concurrent.Future

//"localhost:8082/api/sendEmail"
object EmailSender extends OrderJsonProtocol {
  implicit val actorSystem = ActorSystem("FaultDetectorActorSystem")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val DUPLICATE_ORDER = "duplicateOrder"

  def sendFailEmailDuplicateOrder(): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = EMAIL_SERVICE_URL + DUPLICATE_ORDER,
      method = HttpMethods.GET
    )
    );
  }
}
