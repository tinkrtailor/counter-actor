package com.counter

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.counter.Counter.{
  Decrement,
  Increment,
}

import scala.util.{Failure, Success}

object CounterApp extends {

  def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    // Akka binds to some port and address on the local machine and waits for incoming requests
    val futureBinding =
      Http().newServerAt("localhost", port = 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/",
                        address.getHostString,
                        address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val guardianActor = Behaviors.setup[Nothing] { context =>
      // Spawn a counter actor as it's child
      val counterActor =
        context.spawn(Counter("counter!"), name = "CounterActor")
      // Subscribe to termination notifications for the spawned actor
      context.watch(counterActor)

      val routes = new CounterRoutes(counterActor)(context.system)
      startHttpServer(routes.counterRoutes)(context.system)

      // The guardian's behavior is just to create and monitor the child actor it creates
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](guardianActor, "CounterApp")
  }
}
