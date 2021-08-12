package typed

import akka.NotUsed
import akka.actor.typed.Terminated
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
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
  val commandHandler: (State, Command) => Effect[Event, State] = { (_, command) =>
    command match {
      case Add(data) => Effect.persist(Added(data))
      case Clear => Effect.persist(Cleared)
    }
  }

  val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case Added(data) => state.copy((data :: state.history))
      case Cleared => State(Nil)
    }
  }

  def apply(): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(CombinerActor.getClass.getSimpleName()),
      emptyState = State(),
      commandHandler = (state, command) => commandHandler( state, command ),
      eventHandler = (state, event) => eventHandler( state, event )
    )
}

object CombinerApp {
  val appBehavior = Behaviors.setup[NotUsed] { context =>
    context.log.info("*** CombinerApp started!")

    val combinerActor = context.spawn(CombinerActor(), "combiner-actor")
    context.log.info("*** CombinerActor started!")
    context.watch(combinerActor)
    combinerActor ! Add("hello, ")
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