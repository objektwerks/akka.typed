package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

object CountActor {
  import akka.actor.typed.Behavior
  import akka.actor.typed.scaladsl.Behaviors

  sealed trait Count extends Product with Serializable
  case object Increment extends Count
  case object Decrement extends Count

  def onOffActorBehavior(count: Int = 0): Behavior[Count] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increment =>
          context.log.info(s"*** Increment +1, Count: ${count + 1}")
          onOffActorBehavior(count + 1)
        case Decrement =>
          context.log.info(s"*** Decrement -1, Count: ${count - 1}")
          onOffActorBehavior(count - 1)
      }
    }
  }

class StatelessActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import CountActor._

  "CountActor behavior" should {
    "increment / decrement" in {
      val testProbe = createTestProbe[Count]("test-count")
      val countActor = spawn(onOffActorBehavior(0), "count-actor")
      countActor ! Increment
      testProbe.expectNoMessage()
      countActor ! Decrement
      testProbe.expectNoMessage()
    }
  }
}