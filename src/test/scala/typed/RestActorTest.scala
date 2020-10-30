package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.ActorSystem

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object Rest {
  implicit lazy val formats = DefaultFormats

  def getJoke(implicit system: ActorSystem, dispatcher: ExecutionContext): Future[String] = {
    val client = Http()
    client.singleRequest( HttpRequest(uri = "http://api.icndb.com/jokes/random/") ).flatMap { response =>
      Unmarshal(response)
        .to[String]
        .map { json => s"<p>${parseJson(json)}</p>" }
        .recover { case error => s"<p>${error.getMessage}</p>" }
    }
  }

  def parseJson(json: String): String = {
    val jValue = parse(json) 
    (jValue \ "value" \ "joke").extract[String]
  }
}

object Messages {
  sealed trait Message extends Product with Serializable
  final case class GetJoke(sender: ActorRef[Joke]) extends Message
  final case class Joke(text: String, sender: ActorRef[Joke]) extends Message
}

object RestActor {  
  import Messages._

  def restActorBehavior(implicit system: ActorSystem, dispatcher: ExecutionContext): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetJoke(sender) =>
          context.log.info("*** GetJoke for {}", sender.path.name)
          context.pipeToSelf( Rest.getJoke ) {
            case Success(text) => Joke(text, sender)
            case Failure(failure) => Joke(failure.getMessage, sender)
          }
          Behaviors.same
        case joke @ Joke(text, sender) =>
          context.log.info("*** Joke {} for {}", text, sender)
          sender ! joke
          Behaviors.same
      }
    }
}

class RestActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  implicit override val system = testKit.internalSystem
  implicit val dispatcher = system.executionContext

  import Messages._
  import RestActor._

  "RestActor behavior" should {
    "getJoke / joke" in {
      val testProbe = createTestProbe[Message]("test-rest")
      val restActor = spawn(restActorBehavior(system.classicSystem, dispatcher), "rest-actor")
      restActor ! GetJoke(testProbe.ref)
      testProbe.expectMessageType[Joke]
    }
  }
}