package typed

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

sealed trait Count extends Product with Serializable
case object Increment extends Count
case object Decrement extends Count

object CountActor {
  def behavior(count: Int = 0): Behavior[Count] =
    Behaviors.receive {
      (context, message) => message match {
        case Increment =>
          context.log.info("*** Increment +1, Count: ", count + 1)
          behavior(count + 1)
        case Decrement =>
          context.log.info("*** Decrement -1, Count: ", count - 1)
          behavior(count - 1)
      }
    }
}

class StatelessActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "CountActor behavior" should {
    "increment / decrement" in {
      val testProbe = createTestProbe[Count]("test-count")
      val countActor = spawn(CountActor.behavior(0), "count-actor")
      countActor ! Increment
      testProbe.expectNoMessage()
      countActor ! Decrement
      testProbe.expectNoMessage()
    }
  }
}