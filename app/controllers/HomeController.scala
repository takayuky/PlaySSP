package controllers

import javax.inject._

import play.api.Logger
import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Success

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc) {

  val request: WSRequest = ws.url("http://localhost:8080/main.php")
  val dspRequest: Int => Future[WSResponse] =
    (id: Int) => request.addHttpHeaders("Accept" -> "application/json")
                        .withRequestTimeout(50 milliseconds)
                        .post(Json.toJson(
                          Map("app_id" -> id)
                        ))

  def index = Action { request =>
    val response = for {
      json <- request.body.asJson
      id <- (json \ "app_id").asOpt[Int] if id == 123 if Await.ready(dspRequest(id), Duration.Inf).isCompleted
    } yield Ok (
      Json.toJson(Map("app_id" -> id))
    )
    response.getOrElse(BadRequest("Miss"))
  }
}
