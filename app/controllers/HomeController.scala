package controllers

import javax.inject._

import play.api.mvc._
import play.api.libs.json.{Json}
import play.api.libs.ws._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
case class BidResponse(url: String, price: Int)

@Singleton
class HomeController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc) {

  val requestUrl: WSRequest = ws.url("http://localhost:8080/main.php")
  val requestUrl2: WSRequest = ws.url("http://localhost:8000/main.php")

  def requestToDsp(id: Int, request: WSRequest): Future[BidResponse] =
    request.addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(1 seconds)
      .post(Json.toJson(
        Map("app_id" -> id)
      ))
      .map {
        response => BidResponse.apply(
          (response.json \ "url").get.as[String],
          (response.json \ "price").get.as[Int]
        )
      }

  def index = Action { request =>

    val response = for {
      json <- request.body.asJson
      id <- (json \ "app_id").asOpt[Int] if id == 123
      futureOfSequense <- Option(Future.sequence(List(requestToDsp(id, requestUrl), requestToDsp(id, requestUrl2))))
      result <- Await.result(futureOfSequense, Duration.Inf)
    } yield {
      Ok(Json.toJson(Map(
        "url" -> result.url
      )))
    }
    response.getOrElse(BadRequest("Miss"))
  }
}
