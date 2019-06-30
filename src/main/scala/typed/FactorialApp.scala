package typed

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import typed.FactorialActor.factorialActorBehavior

import scala.annotation.tailrec

object Messages {
  sealed trait Message
  case class Numbers(numbers: List[Long]) extends Message
  case class CalculateFactorials(numbers: List[Long], sender: ActorRef[FactorialsCalculated]) extends Message
  case class FactorialsCalculated(numbers: List[Long]) extends Message
}

object FactorialActor {
  import Messages._

  @tailrec
  private def factorial(n: Long, acc: Long = 1): Long = n match {
    case i if i < 1 => acc
    case _ => factorial(n - 1, acc * n)
  }

  val factorialActorBehavior = Behaviors.receive[Message] { (context, message) =>
    message match {
      case CalculateFactorials(numbers, sender) =>
        context.log.info("CalculateFactorial.numbers = {} from {}", numbers, sender.path.name)
        sender ! FactorialsCalculated(numbers.map(n => factorial(n)))
        Behaviors.same
      case _:Message => Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("FactorialActor stopped!")
      Behaviors.same
  }
}

object DelegateActor {
  import Messages._

  val delegateActorBehavior = Behaviors.receive[Message] { (context, message) =>
    message match {
      case Numbers(numbers) =>
        context.log.info("Numbers.numbers = {}", numbers)
        val factorialActor = context.spawn(factorialActorBehavior, "factorial-actor")
        factorialActor ! CalculateFactorials(numbers, context.self)
        Behaviors.same
      case FactorialsCalculated(numbers) =>
        context.log.info("FactorialsCalculated.numbers: {}", numbers)
        Behaviors.stopped
      case _:Message => Behaviors.same
    }
  }
}

object FactorialApp extends App {
  val main: Behavior[NotUsed] =
    Behaviors.setup { context =>
      import DelegateActor._
      import Messages._

      context.log.info("FactorialApp started!")

      val delegateActor = context.spawn(delegateActorBehavior, "delegate-actor")
      context.watch(delegateActor)
      delegateActor ! Numbers(List[Long](1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

      Behaviors.receiveSignal {
        case (ctx, Terminated(_)) =>
          ctx.log.info("FactorialApp stopped!")
          Behaviors.stopped
      }
    }

  val system = ActorSystem(main, "factorial-app")
}