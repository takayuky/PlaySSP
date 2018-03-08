package controllers

import akka.actor.ActorSystem
import akka.pattern.after
import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.ws._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
case class BidResponse(url: String, price: Int)

@Singleton
class HomeController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc) {
  implicit val system = ActorSystem("HomeController")

  val requestUrl: WSRequest = ws.url("http://localhost:8080/main.php")
  val requestUrl2: WSRequest = ws.url("http://localhost:8000/main.php")

  def requestToDsp(id: Int, request: WSRequest): Future[Either[Throwable, BidResponse]] =
    request.addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(100.milliseconds)
      .post(Json.toJson(
        Map("app_id" -> id)
      ))
      .map { response =>
        Right(BidResponse.apply(
          (response.json \ "url").get.as[String],
          (response.json \ "price").get.as[Int]
        ))
      }
      .withTimeout(100.milliseconds)
      .recover { case t =>
        Left(t)
      }

  def index = Action.async { request =>
    val response = for {
      json <- request.body.asJson
      id <- (json \ "app_id").asOpt[Int] if id == 123
    } yield {
      val listOfFutures = List(requestToDsp(id, requestUrl), requestToDsp(id, requestUrl2))
      val futureOfList = Future.sequence(listOfFutures)
      futureOfList.map { listOfEither =>
        val bidResponsesReceivedInTime = listOfEither.flatMap(_.toOption)

        if (bidResponsesReceivedInTime.nonEmpty) {
          Ok(Json.toJson(Map(
            "url" ->  bidResponsesReceivedInTime.maxBy(bid => bid.price).url
          )))
        } else {
          MovedPermanently("aa")
        }
      }
    }
    response.getOrElse(Future.successful(BadRequest))
  }

  implicit def toFutureHelper[T](f: Future[T]): FutureHelper[T] = new FutureHelper(f)
}

class FutureHelper[T](val f: Future[T]) extends AnyVal{
  def withTimeout(timeout: FiniteDuration)(implicit system: ActorSystem): Future[T] = {
    val delayed = after(timeout, system.scheduler)(Future.failed(new Exception))
    Future firstCompletedOf Seq(f, delayed)
  }
}
