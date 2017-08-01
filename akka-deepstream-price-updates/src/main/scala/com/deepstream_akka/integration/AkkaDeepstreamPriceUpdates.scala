package com.deepstream_akka.integration

import java.util

import akka.actor.{Actor, ActorSystem, Props}
import com.google.gson.JsonObject
import io.deepstream.{DeepstreamClient, ListenListener}

import scala.collection.mutable
import scala.io.Source


case class PriceUpdate(name: String, fullName: String, price: Double)

class PriceStreamingActor extends Actor {
  val child = context.actorOf(Props[StreamingDeepstreamActor])
  val r = new scala.util.Random
  var running = true

  val list = new util.ArrayList[(String, String)]()

  override def preStart: Unit = {
    val lines = Source.fromFile("src/main/scala/com/deepstream_akka/integration/test-data.csv").getLines()
    lines.foreach(line => {
      val split = line.split(",")
      list.add((split(0), split(1)))
    })
    val size = this.list.size
    println(s"PriceStreamingActor started with $size names")
    this.start
  }

  override def postStop: Unit = println("PriceStreamingActor stopped")
  override def receive: Receive = Actor.emptyBehavior

  child ! "init"

  def start = {
    while (this.running) {
      this.list.forEach(name => {
        child ! PriceUpdate(
          name._1,
          name._2,
          20 + r.nextInt((70 - 20) + 1))
      })
      Thread.sleep(500)
    }
  }
}

private class StreamingDeepstreamActor extends Actor with ListenListener {
  private var dsClient : DeepstreamClient = _
  private var subscriptions = mutable.Set[String]()

  override def preStart: Unit = println("DeepstreamActor started")
  override def postStop: Unit = println("DeepstreamActor stopped")

  override def receive = {
    case "init" => {
      this.dsClient = new DeepstreamClient("localhost:6020")
      val loginResult = this.dsClient.login()
      if (loginResult.loggedIn) {
        println("DeepstreamClient login")
      } else {
        throw new Exception("unable to initialise deepstream connection")
      }
      this.dsClient.event.listen("prices/*", this)
    }

    case PriceUpdate(fullName, name, price) => {
      if (this.subscriptions.contains(s"prices/$name")) {
        val jsonObject = new JsonObject
        jsonObject.addProperty("name", name)
        jsonObject.addProperty("price", price)
        this.dsClient.event.emit(s"prices/$name", jsonObject)
      }
    }
  }

  override def onSubscriptionForPatternAdded(subscription: String) : Boolean = {
    this.subscriptions += subscription
    return true
  }

  override def onSubscriptionForPatternRemoved(subscription: String) = {
    this.subscriptions -= subscription
  }
}
