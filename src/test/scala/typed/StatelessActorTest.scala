package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

object OnOffActor {
  import akka.actor.typed.Behavior
  import akka.actor.typed.scaladsl.Behaviors

  sealed trait State extends Product with Serializable
  case object On extends State
  case object Off extends State

  def onOffActorBehavior(state: Int = 0): Behavior[State] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case On =>
        context.log.info(s"*** (1) On!")
        onOffActorBehavior(state + 1)
      case Off =>
        context.log.info(s"*** (0) Off")
        onOffActorBehavior(state - 1)
    }
  }
}

class StatelessActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import OnOffActor._

  "Emotion actor behavior" should {
    "emotions" in {
      val testProbe = createTestProbe[State]("test-on-off")
      val onOffActor = spawn(onOffActorBehavior(0), "on-off-actor")
      onOffActor ! On
      testProbe.expectNoMessage()
      onOffActor ! Off
      testProbe.expectNoMessage()
    }
  }
}