package typed

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import org.scalatest.wordspec.AnyWordSpecLike

object EmotionActor {
  import akka.actor.typed.Behavior
  import akka.actor.typed.scaladsl.Behaviors

  sealed trait Emotion extends Product with Serializable
  case object Happy extends Emotion
  case object Sad extends Emotion

  val emotionActorBahvior: Behavior[Emotion] = Behaviors.setup { context =>
    var level = 0

    Behaviors.receiveMessage {
      case Happy =>
        level += 1
        context.log.info(s"*** Happy +1, Emotion level: $level")
        Behaviors.same
      case Sad =>
        level -= 1
        context.log.info(s"*** Sad -1, Emotion level: $level")
        Behaviors.same
    }
  }
}

class StatefulActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import EmotionActor._

  "EmotionActor behavior" should {
    "happy / sad" in {
      val testProbe = createTestProbe[Emotion]("test-emotion")
      val emotionActor = spawn(emotionActorBahvior, "emotion-actor")
      emotionActor ! Happy
      testProbe.expectNoMessage()
      emotionActor ! Sad
      testProbe.expectNoMessage()
    }
  }
}