package controllers

import javax.inject._

import akka.actor._
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.streams.ActorFlow
import play.api.mvc._

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
        Logger.debug(s"""message: $msg""")
        out ! msg
    }

    override def postStop(): Unit = {
      Logger.debug("socket stop")
    }
  }
}