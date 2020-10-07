package com.counter

import com.counter.Counter.{
  ActionPerformed,
  ClearCounter,
  Decrement,
  GetCounter,
  GetCounterResponse,
  Increment,
  SetValue
}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

import scala.util.{Failure, Success}

case class UserCommand(message: String, data: Option[Int])

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import spray.json.DefaultJsonProtocol._

  implicit val userCommandInputJsonFormat = jsonFormat2(UserCommand)

  implicit val getCounterResponseJsonFormat = jsonFormat1(GetCounterResponse)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}

class CounterRoutes(counterActor: ActorRef[Counter.Command])(
    implicit val system: ActorSystem[_]) {

  private implicit val timeout = Timeout(3.seconds)

  def getCounter(): Future[GetCounterResponse] = counterActor.ask(GetCounter)
  def increment(): Future[ActionPerformed] = counterActor.ask(Increment)
  def decrement(): Future[ActionPerformed] = counterActor.ask(Decrement)
  def setValue(value: Int): Future[ActionPerformed] =
    counterActor.ask(SetValue(value, _))
  def clearCounter(): Future[ActionPerformed] = counterActor.ask(ClearCounter)

  def mapUserCommand(message: String,
                     maybeData: Option[Int]): Future[ActionPerformed] =
    message match {
      case "Increment"    => increment()
      case "Decrement"    => decrement()
      case "ClearCounter" => clearCounter()
      case "SetValue" =>
        maybeData match {
          case Some(value) => setValue(value)
          case None =>
            Future.failed(
              new Exception(
                "Missing parameter for data to go along with SetValue command"))
        }
      case _ =>
        Future.failed(
          new Exception("Unknown command passed as message to actor"))
    }

  // Import the SprayJsonSupport trait  for providing automatic to and from JSON marshalling/unmarshalling
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  val counterRoutes: Route = pathPrefix("counter") {
    concat(
      get {
        complete(getCounter())
      },
      patch {
        entity(as[UserCommand]) { userCmd =>
          onComplete(mapUserCommand(userCmd.message, maybeData = userCmd.data)) {
            case Failure(ex) =>
              complete(StatusCodes.BadRequest,
                       s"An error occurred: ${ex.getMessage}")
            case Success(performed) => complete(performed)
          }
        }
      }
    )
  }
}
