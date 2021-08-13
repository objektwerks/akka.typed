package typed

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

sealed trait Command extends Message
final case class Add(data: String) extends Command
case object Clear extends Command

sealed trait Event extends Message
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
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val combinerActor = context.spawn(CombinerActor(CombinerActor.id), "combiner-actor")
    context.log.info("*** CombinerActor started!")
    context.watch(combinerActor)
    Behaviors.receiveMessage[Command] { command =>
      command match {
        case add : Add =>
          combinerActor ! add
          Behaviors.same
        case Clear =>
          combinerActor ! Clear
          Behaviors.same
      }
    }
  }
  
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Command](CombinerApp(), "combiner-app")
    system.log.info("*** CombinerApp running ...")
    system ! Add("Hello, ")
    system ! Add("world!")
    system ! Clear
    Thread.sleep(1000L)
    system.log.info("*** CombinerApp terminated!")
    system.terminate()
  }
}