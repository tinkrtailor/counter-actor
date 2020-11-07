package com.counter

import com.counter.Counter.{Decrement, Get, Increment, State}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.pattern.StatusReply
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, Route}

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

case class UserCommand(message: String, data: Option[Int])

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  implicit val userCommandInputJsonFormat = jsonFormat2(UserCommand)
  implicit val stateResponseJsonFormat = jsonFormat1(State)
}

class CounterRoutes(counterActor: ActorRef[Counter.Command])(
    implicit val system: ActorSystem[_]) {

  private implicit val timeout = Timeout(3.seconds)

  def getCounter() = counterActor.ask(Get)
  def increment() = counterActor.ask(Increment)
  def decrement() = counterActor.ask(Decrement)

  def mapUserCommand(message: String, maybeData: Option[Int]) =
    message match {
      case "Increment" => increment()
      case "Decrement" => decrement()
      case _ =>
        Future.failed(
          new UnsupportedOperationException(
            "Unknown command passed as message to actor"))
    }

  // Import the SprayJsonSupport trait  for providing automatic to and from JSON marshalling/unmarshalling
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

def myExceptionHandler: ExceptionHandler =
  ExceptionHandler {
    case ex: UnsupportedOperationException =>
      complete(StatusCodes.BadRequest, s"An error occurred: ${ex.getMessage}")
  }

val counterRoutes: Route = handleExceptions(myExceptionHandler) {
    pathPrefix("counter") {
      concat(
        get {
          complete(getCounter())
        },
        patch {
          entity(as[UserCommand]) {
            userCmd =>
              val reply: Future[StatusReply[State]] =
                mapUserCommand(userCmd.message, maybeData = userCmd.data)
              onSuccess(reply) {
                case StatusReply.Success(state: Counter.State) =>
                  complete(StatusCodes.OK -> state)
                case StatusReply.Error(reason) =>
                  complete(StatusCodes.BadRequest -> reason)
              }
          }
        }
      )
    }
  }
}
