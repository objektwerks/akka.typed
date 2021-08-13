package typed

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

sealed trait Entity extends Product with Serializable
final case class Text(text: String, sender: ActorRef[Echo]) extends Entity
final case class Echo(text: String) extends Entity

object TextActor {
  def apply(): Behavior[Text] = Behaviors.receive[Text] { (context, text) =>
    text match {
      case Text(text, sender) =>
        context.log.info("*** Text = {} from {}", text, sender.path.name)
        sender ! Echo(text)
        Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("*** TextActor stopped!")
      Behaviors.same
  }
}

class TextActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "TextActor behavior" should {
    "message / echo" in {
      val testProbe = createTestProbe[Echo]("test-probe")
      val textActor = spawn(TextActor(), "text-actor")
      textActor ! Text("test", testProbe.ref)
      testProbe.expectMessage(Echo("test"))
    }
  }
}