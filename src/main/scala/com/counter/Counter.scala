package com.counter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  RetentionCriteria
}
import scala.concurrent.duration._

object Counter {
  // The protocol for which the actor can be communicated with:
  sealed trait Command extends CborSerializable
  final case class Increment(replyTo: ActorRef[StatusReply[State]])
      extends Command
  final case class Decrement(replyTo: ActorRef[StatusReply[State]])
      extends Command
  final case class Get(replyTo: ActorRef[State]) extends Command

  sealed trait Event extends CborSerializable
  final case object Incremented extends Event
  final case object Decremented extends Event

  final case class State(count: Int) {
    def increment(): State = {
      copy(count + 1)
    }
    def decrement(): State = {
      copy(count - 1)
    }
  }
private def commandHandler(state: State,
                           command: Command): Effect[Event, State] =
  command match {
    case Get(replyTo) => Effect.reply(replyTo)(state)
    case Increment(replyTo) =>
      Effect
        .persist(Incremented)
        .thenRun((updatedState: State) =>
          replyTo ! StatusReply.Success(updatedState))
    case Decrement(replyTo) =>
      Effect
        .persist(Decremented)
        .thenRun((updatedState: State) =>
          replyTo ! StatusReply.Success(updatedState))
    case _ => Effect.none
  }

private def handleEvent(state: State, event: Event): State = event match {
  case Incremented => state.increment()
  case Decremented => state.decrement()
}

  def apply(counterId: String): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("Counter", counterId),
      State(0),
      (state, command) => commandHandler(state, command),
      (state, event) => handleEvent(state, event)
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100,
                                                     keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy
        .restartWithBackoff(200.millis, 5.seconds, randomFactor = 0.1))
  }

}
