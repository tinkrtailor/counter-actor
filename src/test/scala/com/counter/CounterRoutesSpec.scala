package com.counter

import java.util.UUID

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.counter.Counter.State
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CounterRoutesSpec
    extends AnyWordSpec
    with ScalatestRouteTest
    with Matchers
    with ScalaFutures {

  import akka.actor.typed.scaladsl.adapter._
  val typedSystem: ActorSystem[Nothing] = system.toTyped
  val counterId = "counterId"

  val counter = typedSystem.systemActorOf(Counter(counterId), "Counter")
  lazy val routes = new CounterRoutes(counter)(typedSystem).counterRoutes
  // use the json formats to marshal and unmarshall objects in the test
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._


  "CounterRoutes" should {
    "return a the value for the counter  (GET /counter)" in {
      Get("/counter") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        // we expect the response to be json:
        contentType shouldBe ContentTypes.`application/json`
        // and the counter value should be returned as zero:
        entityAs[String] shouldEqual """{"count":0}"""
        // we can also decode it into a domain object for more cleaner matching
        entityAs[State] shouldEqual State(0)
      }
    }
    "Increment message should successfully increment the counter (PATCH /counter)" in {
      val userCommand = UserCommand(message = "Increment", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[State] shouldEqual State(1)
      }
    }
    "Decrement message should successfully decrement the counter back to 0 (PATCH /counter)" in {
      val userCommand = UserCommand(message = "Decrement", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[State] shouldEqual State(0)
      }
    }

    "Passing an unknown command as message should return an BadRequest Error (PATCH /counter)" in {
      val userCommand = UserCommand(message = "Foo", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        entityAs[String] shouldEqual "An error occurred: Unknown command passed as message to actor"
      }
    }
  }
}
