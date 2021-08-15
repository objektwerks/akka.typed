package typed

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import scala.annotation.tailrec

sealed trait Calculation extends Product with Serializable
final case class Calculate(numbers: List[Long]) extends Calculation
final case class CalculateFactorials(numbers: List[Long], sender: ActorRef[Calculation]) extends Calculation
final case class FactorialsCalculated(numbers: List[Long]) extends Calculation

object FactorialActor {
  def apply(): Behavior[Calculation] = Behaviors.receive[Calculation] {
    (context, calculation) => calculation match {
      case CalculateFactorials(numbers, sender) =>
        context.log.info("*** CalculateFactorial numbers: {} sender: {}", numbers, sender.path.name)
        sender ! FactorialsCalculated(numbers.map(n => factorial(n)))
        Behaviors.same
      case _: Calculation => Behaviors.same
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

object CalculationActor {
  def apply(): Behavior[Calculation] = Behaviors.setup { context =>
    val factorialActor = context.spawn(FactorialActor(), "factorial-actor")
    context.log.info("*** FactorialActor started!")

    Behaviors.receive[Calculation] {
      (context, message) =>
        message match {
          case Calculate(numbers) =>
            context.log.info("*** Calculate numbers = {}", numbers)
            factorialActor ! CalculateFactorials(numbers, context.self)
            Behaviors.same
          case FactorialsCalculated(numbers) =>
            context.log.info("*** FactorialsCalculated numbers: {}", numbers)
            Behaviors.same
          case _ => Behaviors.same
        }
    }.receiveSignal {
      case (context, PostStop) =>
        context.log.info("*** CalculationActor stopped!")
        Behaviors.same
    }
  }
}

object FactorialApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Calculation](CalculationActor(), "factorial-app")
    system.log.info("*** CalculationActor started!")
    system.log.info("*** FactorialApp running!")
    system ! Calculate(List[Long](3, 4, 5))
    system ! Calculate(List[Long](6, 7, 8))
    system ! Calculate(List[Long](9, 10, 11))
    Thread.sleep(1000L)
    system.log.info("*** FactorialApp terminating ...")
    system.terminate()
  }
}