package com.counter

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import akka.persistence.typed.PersistenceId
import com.counter.Counter.{ Decrement, Decremented, Get, Increment, Incremented}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CounterSpec
    extends ScalaTestWithActorTestKit(
      PersistenceTestKitPlugin.config.withFallback(
        ConfigFactory.defaultApplication().resolve()))
    with Matchers
    with BeforeAndAfterEach
    with AnyWordSpecLike {

  val persistenceTestKit = PersistenceTestKit(system)
  lazy val counterId = "Counter1"
  lazy val pid = PersistenceId("Counter", counterId)


  "The Counter" should {
    "Return the state when queried with a Get command" in {
      val replyProbe = createTestProbe[Counter.State]()
      val testCounter = spawn(Counter(counterId))
      testCounter ! Get(replyProbe.ref)
      replyProbe.expectMessage(Counter.State(0))
    }
    "Increment the counter and reply with a status message containing the updated state" in {
      val replyProbe = createTestProbe[StatusReply[Counter.State]]()
      val testCounter = spawn(Counter(counterId))
      testCounter ! Increment(replyProbe.ref)
      persistenceTestKit.expectNextPersisted(pid.id, Incremented)
      replyProbe.expectMessage(StatusReply.Success(Counter.State(1)))
    }

    "Decrement the counter back to zero and reply with a status message containing the updated state" in {
      val replyProbe = createTestProbe[StatusReply[Counter.State]]()
      val testCounter = spawn(Counter(counterId))
      testCounter ! Decrement(replyProbe.ref)
      persistenceTestKit.expectNextPersisted(pid.id, Decremented)
      replyProbe.expectMessage(StatusReply.Success(Counter.State(0)))
    }
    "keep its state" in {
      val testActor = testKit.spawn(Counter(counterId))
      val probe = testKit.createTestProbe[StatusReply[Counter.State]]()
      testActor ! Increment(probe.ref)
      probe.expectMessage(StatusReply.Success(Counter.State(1)))
      persistenceTestKit.expectNextPersisted(pid.id, Incremented)
      testKit.stop(testActor)

      // start again with same id
      val restartedListing = testKit.spawn(Counter(counterId))
      val stateProbe = testKit.createTestProbe[Counter.State]()
      restartedListing ! Get(stateProbe.ref)
      stateProbe.expectMessage(Counter.State(1))
      persistenceTestKit.expectNothingPersisted(pid.id)
    }
  }
}
