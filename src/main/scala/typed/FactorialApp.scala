package typed

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

import typed.FactorialActor.factorialActorBehavior

import scala.annotation.tailrec

object Messages {
  sealed trait Message extends Product with Serializable
  final case class Numbers(numbers: List[Long]) extends Message
  final case class CalculateFactorials(numbers: List[Long], sender: ActorRef[FactorialsCalculated]) extends Message
  final case class FactorialsCalculated(numbers: List[Long]) extends Message
}

object FactorialActor {
  import Messages._

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

  @tailrec
  def factorial(n: Long, acc: Long = 1): Long = n match {
    case i if i < 1 => acc
    case _ => factorial(n - 1, acc * n)
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
      case _: Message => Behaviors.same
    }
  }
}

object FactorialApp extends App {
  val factorialAppBehavior = Behaviors.setup[NotUsed] { context =>
    import DelegateActor._
    import Messages._

    context.log.info("FactorialApp started!")

    val delegateActor = context.spawn(delegateActorBehavior, "delegate-actor")
    context.log.info("DelegateActor started!")
    context.watch(delegateActor)
    delegateActor ! Numbers(List[Long](3, 6, 9))

    Behaviors.receiveSignal {
      case (_, Terminated(_)) =>
        context.log.info("DelegateActor stopped!")
        context.log.info("FactorialApp stopped!")
        context.system.terminate()
        Behaviors.stopped
    }
  }

  val system = ActorSystem(factorialAppBehavior, "factorial-app")
}