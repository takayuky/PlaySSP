package controllers

import javax.inject._

import akka.actor._
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import play.api.libs.json._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class WebSocketController @Inject()(cc: ControllerComponents) (implicit system: ActorSystem, mat: Materializer)  extends AbstractController(cc) {

  def socket: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef { out =>
      WebSocketActor.props(out)
    }
  }

  object WebSocketActor {
    def props(out: ActorRef) = Props(new WebSocketActor(out))
  }

  class WebSocketActor(out: ActorRef) extends Actor {
    override def preStart(): Unit = {
      Logger.debug("socket start")
    }

    def receive: Receive = {
      case msg: String =>
        val json = Json.parse(msg)
        Logger.debug(s"""message: ${(json \ "app_id").get.asOpt[Int]}""")
        if ((json \ "app_id").get.toString() == "123")
          out ! s"""send dsp => $msg"""
        else
          out ! "return client"
    }

    override def postStop(): Unit = {
      Logger.debug("socket stop")
    }
  }
}