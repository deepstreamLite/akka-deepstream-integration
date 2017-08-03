package com.deepstream_akka.integration

import akka.actor.{ActorSystem, Props}


object Runner extends App {
  val system: ActorSystem = ActorSystem("price-system")
  val streamingActor = system.actorOf(Props[PriceStreamingRootActor])
  val purchasingActor = system.actorOf(Props[PurchasingActor])
}
