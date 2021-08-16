package typed

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import java.time.Instant

object NowApp {
  def main(args: Array[String]): Unit = {
    final case class Now(now: String = Instant.now.toString) extends Product with Serializable
    val nowActor = Behaviors.receiveMessage[Now] {
      case Now(now) =>
        println(s"*** Now: $now")
        Behaviors.same
    }
    val system = ActorSystem[Now](nowActor, "now-app")
    system ! Now()
    system.terminate()
  }
}