package com.deepstream_akka.integration

import java.math.BigInteger
import java.net.InetSocketAddress

import akka.stream._
import akka.stream.scaladsl._
import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, ExtendedActorSystem, ExtensionId, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import io.deepstream.{DeepstreamClient, ListenListener}
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import java.util

import scala.collection.mutable.{Queue => MQueue}
import akka.actor.Actor.Receive
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import com.google.gson.JsonObject

import scala.collection.mutable
import scala.io.Source

case class Price(price: Double, name: String)
case class InterestRate(rate: Double)

class PricePublisher extends ActorPublisher[Price] {
  val r = new scala.util.Random
  val list = new util.ArrayList[String]()
  val lines = Source.fromFile("src/main/scala/com/deepstream_akka/integration/test-data.csv").getLines()
  lines.foreach(line => {
    val split = line.split(",")
    list.add(split(1))
  })

  def publish = {
    while(isActive && totalDemand > 0) {
      this.list.forEach(name => {
        val price = 20 + r.nextInt((70 - 20) + 1)
        onNext(Price(
          price,
          name
        ))
      })
      Thread.sleep(1000)
    }
  }

  override def receive: Receive = {
    case Request(cnt) => {
      this.publish
    }
  }
}

class Aggregator extends ActorSubscriber with ActorPublisher[Price] {

  var currentRate : Double = 1

  val requestStrategy = new MaxInFlightRequestStrategy(36) {
    override def inFlightInternally = (totalDemand).toInt

    override def batchSize: Int = 36
  }

  override def receive: Receive = {
    case OnNext(p: Price) => {
      onNext(Price(p.price * currentRate, p.name))
    }
    case InterestRate(rate) => {
      this.currentRate = rate
    }
  }
}

class DsActor extends ActorSubscriber with ListenListener {
  var subscriptions = mutable.Set[String]()
  private var dsClient : DeepstreamClient = _

  val requestStrategy = new MaxInFlightRequestStrategy(36) {
    override def inFlightInternally = 0
    override def batchSize: Int = 36
  }

  override def onSubscriptionForPatternAdded(subscription: String): Boolean = {
    this.subscriptions += subscription
    return true
  }

  override def onSubscriptionForPatternRemoved(subscription: String): Unit = {
    this.subscriptions -= subscription
  }

  override def receive: Receive = {
    case OnNext(p: Price) => {
      if (this.subscriptions.contains(s"prices/${p.name}")) {
        val jsonObject = new JsonObject
        jsonObject.addProperty("name", p.name)
        jsonObject.addProperty("price", p.price)
        this.dsClient.event.emit(s"prices/${p.name}", jsonObject)
      }
    }
    case "init" => {
      this.dsClient = new DeepstreamClient("ec2-18-196-1-50.eu-central-1.compute.amazonaws.com:6020")
      val loginResult = this.dsClient.login()
      if (loginResult.loggedIn) {
        println("DeepstreamClient login")
      } else {
        throw new Exception("unable to initialise deepstream connection")
      }
      this.dsClient.event.listen("prices/*", this)
    }
    case other => {
      println(s"received $other")
    }
  }
}

class PriceStreamingRootActor extends Actor {
  val r = new scala.util.Random

  val dsActor = context.actorOf(Props[DsActor])
  dsActor ! "init"
  val dsSubscriber = ActorSubscriber[Price](dsActor)

  Thread.sleep(1000)

  val publisherActor = context.actorOf(Props[PricePublisher])
  val publisher = ActorPublisher[Price](publisherActor)

  val aggregator = context.actorOf(Props[Aggregator])
  val aggregatorSubscriber = ActorSubscriber[Price](aggregator)
  val aggregatorPublisher = ActorPublisher[Price](aggregator)

  publisher.subscribe(aggregatorSubscriber)

  aggregatorPublisher.subscribe(dsSubscriber)

  while (true) {
    val interestRate = 1 + r.nextInt((5 - 1) + 1)
    aggregator ! InterestRate(interestRate)
    Thread.sleep(1000)
  }

  override def receive: Receive = ???
}

