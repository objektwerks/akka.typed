package typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

import scala.annotation.tailrec

final case class Numbers(numbers: List[Long]) extends Message
final case class CalculateFactorials(numbers: List[Long], sender: ActorRef[FactorialsCalculated]) extends Message
final case class FactorialsCalculated(numbers: List[Long]) extends Message

object FactorialActor {
  val behavior = Behaviors.receive[Message] { (context, message) =>
    message match {
      case CalculateFactorials(numbers, sender) =>
        context.log.info("*** CalculateFactorial numbers = {} sender {}", numbers, sender.path.name)
        sender ! FactorialsCalculated(numbers.map(n => factorial(n)))
        Behaviors.same
      case _: Message => Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("*** FactorialActor stopped!")
      Behaviors.same
  }

  @tailrec
  def factorial(n: Long, acc: Long = 1): Long = n match {
    case i if i < 1 => acc
    case _ => factorial(n - 1, acc * n)
  }
}

object DelegateActor {
  val behavior = Behaviors.receive[Message] { (context, message) =>
    message match {
      case Numbers(numbers) =>
        context.log.info("*** Numbers = {}", numbers)
        val factorialActor = context.spawn(FactorialActor.behavior, "factorial-actor")
        factorialActor ! CalculateFactorials(numbers, context.self)
        Behaviors.same
      case FactorialsCalculated(numbers) =>
        context.log.info("*** FactorialsCalculated numbers: {}", numbers)
        Behaviors.stopped
      case _: Message => Behaviors.same
    }
  }
}

object FactorialApp {
  def main(args: Array[String]): Unit = {
    val behavior = Behaviors.receive[Numbers] { (context, numbers) =>
      val delegateActor = context.spawn(DelegateActor.behavior, "delegate-actor")
      context.log.info("*** DelegateActor started!")
      context.watch(delegateActor)
      delegateActor ! numbers

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          context.log.info("*** DelegateActor stopped!")
          context.log.info("*** FactorialApp terminated!")
          context.system.terminate()
          Behaviors.stopped
      }
    }
    val system = ActorSystem(behavior, "factorial-app")
    system.log.info("*** FactorialApp running!")
    system ! Numbers(List[Long](3, 6, 9))
  }
}