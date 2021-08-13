package typed

import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

sealed trait Emotion extends Product with Serializable
case object Happy extends Emotion
case object Sad extends Emotion

object EmotionActor {
  def apply(): Behavior[Emotion] = Behaviors.setup[Emotion] { context =>
    var level = 0
    Behaviors.receiveMessage[Emotion] {
      case Happy =>
        level += 1
        context.log.info(s"*** Happy +1, Emotion level: $level")
        Behaviors.same
      case Sad =>
        level -= 1
        context.log.info(s"*** Sad -1, Emotion level: $level")
        Behaviors.same
    }.receiveSignal {
      case (context, PostStop) =>
        context.log.info("*** EmotionActor stopped!")
        Behaviors.same
    }
  }
}

class StatefulActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "EmotionActor behavior" should {
    "happy / sad" in {
      val testProbe = createTestProbe[Emotion]("test-emotion")
      val emotionActor = spawn(EmotionActor(), "emotion-actor")
      emotionActor ! Happy
      testProbe.expectNoMessage()
      emotionActor ! Sad
      testProbe.expectNoMessage()
    }
  }
}