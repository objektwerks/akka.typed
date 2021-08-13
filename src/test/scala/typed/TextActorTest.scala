package typed

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

final case class Text(text: String, sender: ActorRef[Echo]) extends Product with Serializable
final case class Echo(text: String) extends Product with Serializable

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
    "text / echo" in {
      val testProbe = createTestProbe[Echo]("test-probe")
      val textActor = spawn(TextActor(), "text-actor")
      textActor ! Text("abc123", testProbe.ref)
      testProbe.expectMessage(Echo("abc123"))
    }
  }
}