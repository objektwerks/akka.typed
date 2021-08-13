package typed

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import akka.event.slf4j.Logger

import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

sealed trait Command extends Product with Serializable
final case class Add(data: String) extends Command
case object Clear extends Command

sealed trait Event extends Product with Serializable
final case class Added(data: String) extends Event
case object Cleared extends Event

final case class State(history: List[String] = Nil)

object EventSourceActor {
  val log = Logger(getClass.getSimpleName)
  val id = EventSourceActor.getClass.getSimpleName

  val commandHandler: (State, Command) => Effect[Event, State] =
    (_, command) => command match {
      case Add(data) => Effect
        .persist(Added(data))
        .thenRun(state => log.info(s"*** Add data: {} state: {}", data, state))
      case Clear => Effect
        .persist(Cleared)
        .thenRun(state => log.info("*** Clear state: {}", state))
    }

  val eventHandler: (State, Event) => State =
    (state, event) => event match {
      case Added(data) =>
        val newState = state.copy(data :: state.history)
        log.info("*** Added data: {} state: {}", data, newState)
        newState
      case Cleared =>
        val newState = State(Nil)
        log.info("*** Cleared state: {}", newState)
        newState
    }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = State(Nil),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}

object EventSourceApp {
  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val eventSourceActor = context.spawn(EventSourceActor(EventSourceActor.id), "event-source-actor")
      context.log.info("*** EventSourceActor started!")
      context.watch(eventSourceActor)
      Behaviors.receiveMessage[Command] {
        case add: Add =>
          eventSourceActor ! add
          Behaviors.same
        case Clear =>
          eventSourceActor ! Clear
          Behaviors.same
      }
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Command](EventSourceApp(), "event-source-app")
    system.log.info("*** EventSourceApp running ...")
    system ! Add("Hello, ")
    system ! Add("world!")
    system ! Clear
    Thread.sleep(1000L)
    system.log.info("*** EventSourceApp terminated!")
    system.terminate()
  }
}