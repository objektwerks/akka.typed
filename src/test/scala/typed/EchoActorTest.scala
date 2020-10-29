package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, PostStop}

import org.scalatest.wordspec.AnyWordSpecLike

object EchoActor {
  sealed trait Entity extends Product with Serializable
  final case class Message(text: String, sender: ActorRef[Echo]) extends Entity
  final case class Echo(text: String) extends Entity

  val echoActorBehavior = Behaviors.receive[Message] { (context, message) =>
    message match {
      case Message(text, sender) =>
        context.log.info("*** Message.text = {} from {}", text, sender.path.name)
        sender ! Echo(text)
        Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("*** EchoActor stopped!")
      Behaviors.same
  }
}

class EchoActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import EchoActor._

  "Echo actor behavior" should {
    "echo message" in {
      val testProbe = createTestProbe[Echo]("test-probe")
      val echoActor = spawn(echoActorBehavior, "echo-actor")
      echoActor ! Message("ping", testProbe.ref)
      testProbe.expectMessage(Echo("ping"))
    }
  }
}