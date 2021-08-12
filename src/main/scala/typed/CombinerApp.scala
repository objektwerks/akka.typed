package typed

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId

sealed trait Command
final case class Add(data: String) extends Command
case object Clear extends Command

sealed trait Event
final case class Added(data: String) extends Event
case object Cleared extends Event

final case class State(history: List[String] = Nil)

object CombinerActor {
  val id = CombinerActor.getClass.getSimpleName

  val commandHandler: (State, Command) => Effect[Event, State] = { (_, command) =>
    command match {
      case Add(data) => Effect.persist(Added(data)).thenRun(state => println(s"*** Add data: $data state: $state"))
      case Clear => Effect.persist(Cleared).thenRun(state => println(s"*** Clear state: $state"))
    }
  }

  val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case Added(data) =>
        val newState = state.copy((data :: state.history))
        println(s"*** Added data: $data state: $newState")
        newState
      case Cleared => 
        val newState = State(Nil)
        println(s"*** Cleared state: $newState")
        newState
    }
  }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = State(Nil),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}

object CombinerApp {
  val appBehavior = Behaviors.setup[NotUsed] { context =>
    context.log.info("*** CombinerApp started!")

    val combinerActor = context.spawn(CombinerActor(CombinerActor.id), "combiner-actor")
    context.log.info("*** CombinerActor started!")
    context.watch(combinerActor)
    combinerActor ! Add("Hello, ")
    combinerActor ! Add("world!")
    combinerActor ! Clear

    Behaviors.receiveSignal {
      case (_, Terminated(_)) =>
        context.log.info("*** CombinerActor stopped!")
        context.log.info("*** CombinerApp stopped!")
        context.system.terminate()
        Behaviors.stopped
    }
  }
  val system = ActorSystem(appBehavior, "combiner-app")

  def main(args: Array[String]): Unit = {
    println("*** CombinerApp main invoked!")
  }
}