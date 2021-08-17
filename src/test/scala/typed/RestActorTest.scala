package typed

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait Jokes extends Product with Serializable
final case class GetJoke(replyTo: ActorRef[Joke]) extends Jokes
final case class Joke(text: String, replyTo: ActorRef[Joke]) extends Jokes

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
  def apply(implicit system: ActorSystem,
            dispatcher: ExecutionContext): Behavior[Jokes] = Behaviors.receive[Jokes] {
    (context, jokes) => jokes match {
      case GetJoke(replyTo) =>
        context.log.info("*** GetJoke for {}", replyTo.path.name)
        context.pipeToSelf( Rest.getJoke ) {
          case Success(text) => Joke(text, replyTo)
          case Failure(failure) => Joke(failure.getMessage, replyTo)
        }
        Behaviors.same
      case joke @ Joke(text, replyTo) =>
        context.log.info("*** Joke {} for {}", text, replyTo)
        replyTo ! joke
        Behaviors.same
      case _ => Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("*** RestActor stopped!")
      Behaviors.same
  }
}

class RestActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  implicit override val system = testKit.internalSystem
  implicit val dispatcher = system.executionContext

  "RestActor behavior" should {
    "getJoke / joke" in {
      val testProbe = createTestProbe[Jokes]("test-rest")
      val restActor = spawn(RestActor(system.classicSystem, dispatcher), "rest-actor")
      restActor ! GetJoke(testProbe.ref)
      testProbe.expectMessageType[Joke]
    }
  }
}