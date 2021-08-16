package typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}

import java.time.Instant

object PingFpApp {
  final case class Ping(message: String) extends Product with Serializable

  def apply(): Behavior[Ping] = Behaviors.receive[Ping] {
    (context, ping) => ping match {
      case Ping(message) =>
        context.log.info("Ping fp message: {} at: {}", message, Instant.now)
        Behaviors.same
    }
  }.receiveSignal {
    case (context, PostStop) =>
      context.log.info("*** PingFpApp behavior stopped!")
      Behaviors.same
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Ping](PingFpApp(), "ping-fp-app")
    system.log.info("*** PingFpApp running!")
    system ! Ping("ping")
    system.log.info("*** PingFpApp terminating ...")
    system.terminate()
  }
}