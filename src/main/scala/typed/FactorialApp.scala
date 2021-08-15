package typed

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import scala.annotation.tailrec

sealed trait Calculation extends Product with Serializable
final case class Numbers(numbers: List[Long]) extends Calculation
final case class CalculateFactorials(numbers: List[Long], sender: ActorRef[FactorialsCalculated]) extends Calculation
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
    context.log.info("*** CalculationActor started!")
    context.watch(factorialActor)

    Behaviors.receive[Calculation] {
      (context, message) =>
        message match {
          case Numbers(numbers) =>
            context.log.info("*** Numbers = {}", numbers)
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
    system.log.info("*** FactorialApp running!")
    system ! Numbers(List[Long](1, 2, 3))
    Thread.sleep(1000L)
    system ! Numbers(List[Long](4, 5, 6))
    Thread.sleep(1000L)
    system ! Numbers(List[Long](7, 8, 9))
    Thread.sleep(1000L)
    system.log.info("*** FactorialApp terminating ...")
    system.terminate()
  }
}