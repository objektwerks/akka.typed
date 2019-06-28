package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.WordSpecLike

object EchoActor {
  case class Message(message: String, response: ActorRef[Echo])
  case class Echo(message: String)

  val echoActor: Behavior[Message] = Behaviors.receive {
    (_, message) => message match {
      case Message(m, replyTo) =>
        replyTo ! Echo(m)
        Behaviors.same
    }
  }
}

class EchoActorTest  extends ScalaTestWithActorTestKit with WordSpecLike {
  import EchoActor._

  "Echo behavior" should {
    "echo message" in {
      val probe = createTestProbe[Echo]()
      val echo = testKit.spawn(echoActor, "echo")
      echo ! Message("ping", probe.ref)
      probe.expectMessage(Echo("ping"))
    }
  }
}