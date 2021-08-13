package typed

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final case class GetJoke(sender: ActorRef[Joke]) extends Message
final case class Joke(text: String, sender: ActorRef[Joke]) extends Message

object Rest {
  implicit lazy val formats = DefaultFormats

  def getJoke(implicit system: ActorSystem, dispatcher: ExecutionContext): Future[String] = {
    val client = Http()
    client.singleRequest( HttpRequest(uri = "http://api.icndb.com/jokes/random/") ).flatMap { response =>
      Unmarshal(response)
        .to[String]
        .map { json => s"${parseJson(json)}" }
        .recover { case error => s"${error.getMessage}" }
    }
  }

  def parseJson(json: String): String = {
    val jValue = parse(json) 
    (jValue \ "value" \ "joke").extract[String]
  }
}

object RestActor {
  def apply(implicit system: ActorSystem, dispatcher: ExecutionContext): Behavior[Message] =
    Behaviors.receive[Message] { (context, message) =>
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
        case _ => Behaviors.same
      }
    }
}

class RestActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  implicit override val system = testKit.internalSystem
  implicit val dispatcher = system.executionContext

  "RestActor behavior" should {
    "getJoke / joke" in {
      val testProbe = createTestProbe[Message]("test-rest")
      val restActor = spawn(RestActor(system.classicSystem, dispatcher), "rest-actor")
      restActor ! GetJoke(testProbe.ref)
      testProbe.expectMessageType[Joke]
    }
  }
}