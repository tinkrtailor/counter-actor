package com.counter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Counter {
  // The protocol for which the actor can be communicated with:
  sealed trait Command
  final case class Increment(replyTo: ActorRef[ActionPerformed]) extends Command
  final case class Decrement(replyTo: ActorRef[ActionPerformed]) extends Command
  final case class SetValue(value: Int) extends Command
  final case object ClearCounter extends Command
  final case class GetCounter(replyTo: ActorRef[GetCounterResponse])
      extends Command

  // The messages the actor can emit as replies:
  final case class GetCounterResponse(maybeCount: Int) extends Command
  final case class ActionPerformed(description: String) extends Command

  // The function declaring how the actor responds to messages sent to him (his behavior)
  private def counter(count: Int): Behavior[Command] =
    Behaviors.receiveMessagePartial {
      case GetCounter(replyTo) =>
        replyTo ! GetCounterResponse(count)
        Behaviors.same
      case Increment(replyTo) =>
        replyTo ! ActionPerformed("Counter incremented by one")
        counter(count = count + 1)
      case Decrement(replyTo) =>
        replyTo ! ActionPerformed("Counter decremented by one")
        counter(count = count - 1)
      case ClearCounter    => counter(0)
      case SetValue(value) => counter(value)
    }

  // A counter actor is initialized with it's current state as zero
  def apply(): Behavior[Command] = counter(0)
}
