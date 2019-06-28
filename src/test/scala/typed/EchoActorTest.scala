package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.WordSpecLike

object EchoActor {
  case class Message(text: String, sender: ActorRef[Echo])
  case class Echo(text: String)

  val echoActorBehavior = Behaviors.receive[Message] { (context, message) =>
    message match {
      case Message(text, sender) =>
        context.log.info("Message.text = {} from {}", text, sender.path.name)
        sender ! Echo(text)
        Behaviors.same
    }
  }
}

class EchoActorTest extends ScalaTestWithActorTestKit with WordSpecLike {
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