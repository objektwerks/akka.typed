package typed

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.time.Instant

object PingOoApp {
  final case class Ping(message: String) extends Product with Serializable

  def apply(): Behavior[Ping] = Behaviors.setup[Ping] { context => new PingActor(context) }

  class PingActor(context: ActorContext[Ping]) extends AbstractBehavior[Ping](context) {
    override def onMessage(ping: Ping): Behavior[Ping] = {
      ping match {
        case Ping(message) =>
          context.log.info("Ping oo message: {} at: {}", message, Instant.now)
          Behaviors.same
      }
    }

    override def onSignal: PartialFunction[Signal, Behavior[Ping]] = {
      case PostStop =>
        context.log.info("*** PingActor stopped!")
        this
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Ping](PingOoApp(), "ping-oo-app")
    system.log.info("*** PingOoApp running!")
    system ! Ping("ping")
    system.log.info("*** PingOoApp terminating ...")
    system.terminate()
  }
}