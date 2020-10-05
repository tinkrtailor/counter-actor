package com.counter

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.counter.Counter.{
  ActionPerformed,
  ClearCounter,
  Decrement,
  GetCounter,
  GetCounterResponse,
  Increment,
  SetValue
}

object CounterApp extends {

  def main(args: Array[String]): Unit = {
    val guardianActor = Behaviors.setup[Counter.Command] { context =>
      // Spawn a counter actor as it's child
      val counterActor = context.spawn(Counter(), name = "CounterActor")
      // Subscribe to termination notifications for the spawned actor
      context.watch(counterActor)
      // Send a few messages to our CounterActor
      counterActor ! Increment(context.self)
      counterActor ! SetValue(100)
      counterActor ! GetCounter(context.self)
      counterActor ! Decrement(context.self)
      counterActor ! ClearCounter
      counterActor ! GetCounter(context.self)

      // This parent actor will need to keep an eye on messages sent by its child
      Behaviors.receiveMessagePartial[Counter.Command] {
        case GetCounterResponse(count) =>
          context.log.info("The counter is at: " + count)
          Behaviors.same
        case ActionPerformed(description) =>
          context.log.info(description)
          Behaviors.same
      }
    }
    val system = ActorSystem[Counter.Command](guardianActor, "CounterApp")
  }

  //The root actor only monitors it's CounterActor child and does not receive other messages
}
