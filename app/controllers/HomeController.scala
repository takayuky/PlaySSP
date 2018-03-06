package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action { request =>
    request.body.asJson.map { json =>
      (json \ "app_id").asOpt[Int].map {
        case id@123 => Ok(Json.toJson(
          Map("app_id" -> id)
        ))
        case _ => BadRequest("Miss")
      }.getOrElse {
        BadRequest("Miss")
      }
    }.getOrElse{
      BadRequest("Miss")
    }
  }
}
