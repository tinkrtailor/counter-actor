package com.counter

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.counter.Counter.{ActionPerformed, GetCounterResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CounterRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest {

  lazy val testKit = ActorTestKit()

  import akka.actor.typed.scaladsl.adapter._
  val typedSystem: ActorSystem[Nothing] = system.toTyped

  val counter = testKit.spawn(Counter())
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
        entityAs[GetCounterResponse] shouldEqual GetCounterResponse(0)
      }
    }
    "Increment message should successfully increment the counter (PATCH /counter)" in {
      val userCommand = UserCommand(message = "Increment", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[ActionPerformed] shouldEqual ActionPerformed(
          "Counter incremented by one")
      }
    }
    "Decrement message should successfully decrement the counter to  (PATCH /counter)" in {
      val userCommand = UserCommand(message = "Decrement", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[ActionPerformed] shouldEqual ActionPerformed(
          "Counter decremented by one")
      }
    }
    "ClearCounter message should successfully clear the counter (PATCH /counter)" in {
      val userCommand = UserCommand(message = "ClearCounter", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[ActionPerformed] shouldEqual ActionPerformed(
          "Counter reset to zero")
      }
    }

    "SetValue message should successfully set the value of the counter at some value v (PATCH /counter)" in {
      val userCommand = UserCommand(message = "SetValue", data = Some(100))
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        entityAs[ActionPerformed] shouldEqual ActionPerformed(
          s"Counter value set at 100")
      }
    }

    "SetValue message should return an BadRequest Error if the value to be set is missing (PATCH /counter)" in {
      val userCommand = UserCommand(message = "SetValue", data = None)
      val userCommandEntity = Marshal(userCommand).to[MessageEntity].futureValue

      Patch("/counter").withEntity(userCommandEntity) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)

        entityAs[String] shouldEqual "An error occurred: Missing parameter for data to go along with SetValue command"
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
