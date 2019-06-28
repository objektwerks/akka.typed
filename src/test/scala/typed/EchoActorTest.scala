package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import org.scalatest.WordSpecLike

object EchoActor {
  case class Message(text: String, sender: ActorRef[Echo])
  case class Echo(text: String)

  val echoBehavior: Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Message(text, sender) =>
        context.log.info("Message.text = {} from {}", text, sender.path.name)
        sender ! Echo(text)
        Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("EchoActor stopped!")
      Behaviors.same
  }
}

class EchoActorTest extends ScalaTestWithActorTestKit with WordSpecLike {
  import EchoActor._

  "Echo actor behavior" should {
    "echo message" in {
      val testProbe = createTestProbe[Echo]("test-probe")
      val echoActor = spawn(echoBehavior, "echo-actor")
      echoActor ! Message("ping", testProbe.ref)
      testProbe.expectMessage(Echo("ping"))
    }
  }
}