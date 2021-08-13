package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, PostStop}

import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.Behavior

sealed trait Entity extends Product with Serializable
final case class Message(text: String, sender: ActorRef[Echo]) extends Entity
final case class Echo(text: String) extends Entity

object MessageActor {
  def apply(): Behavior[Message] = Behaviors.receive[Message] { (context, message) =>
    message match {
      case Message(text, sender) =>
        context.log.info("*** Message.text = {} from {}", text, sender.path.name)
        sender ! Echo(text)
        Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("*** MessageActor stopped!")
      Behaviors.same
  }
}

class MessageActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "MessageActor behavior" should {
    "message / echo" in {
      val testProbe = createTestProbe[Echo]("test-probe")
      val messageActor = spawn(MessageActor(), "message-actor")
      messageActor ! Message("test", testProbe.ref)
      testProbe.expectMessage(Echo("test"))
    }
  }
}