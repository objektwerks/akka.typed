package typed

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop, SupervisorStrategy}

import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

sealed trait Jokes extends Product with Serializable
final case class GetJoke(replyTo: ActorRef[Joke]) extends Jokes
final case class Joke(text: String, replyTo: ActorRef[Joke]) extends Jokes

object JokeActor {
  def apply(implicit system: ActorSystem,
            dispatcher: ExecutionContext): Behavior[Jokes] = Behaviors.receive[Jokes] {
    (context, jokes) => jokes match {
      case GetJoke(replyTo) =>
        context.log.info("*** GetJoke for {}", replyTo.path.name)
        context.pipeToSelf( RestJokeService.getJoke ) {
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
      context.log.info("*** RestJokeActor stopped!")
      Behaviors.same
  }
}

class RestJokeActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  implicit override val system = testKit.internalSystem
  implicit val dispatcher = system.executionContext

  "RestJokeActor behavior" should {
    "getJoke / joke" in {
      val testProbe = createTestProbe[Jokes]("test-rest")
      val restActorBehavior = JokeActor(system.classicSystem, dispatcher)
      Behaviors
        .supervise(restActorBehavior)
        .onFailure[Exception](SupervisorStrategy.restart)
      val restActor = spawn(restActorBehavior, "rest-actor")
      restActor ! GetJoke(testProbe.ref)
      testProbe.expectMessageType[Joke]
    }
  }
}