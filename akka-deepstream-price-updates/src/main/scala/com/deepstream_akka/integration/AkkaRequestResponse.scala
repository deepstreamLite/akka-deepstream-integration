package com.deepstream_akka.integration

import java.util

import akka.actor.{Actor, ActorSystem, Props}
import com.google.gson.JsonObject
import io.deepstream.{DeepstreamClient, ListenListener, RpcRequestedListener, RpcResponse}

import scala.collection.mutable
import scala.io.Source


class PurchasingActor extends Actor {
  override def preStart = println(s"SupervisingActor started")
  override def postStop: Unit = println("SupervisingActor stopped")
  override def receive: Receive = Actor.emptyBehavior

  val child = context.actorOf(Props[PurchaseDeepstreamActor])
  child ! "init"
}

private class PurchaseDeepstreamActor extends Actor with RpcRequestedListener {
  private var dsClient : DeepstreamClient = _
  private var failNext : Boolean = false

  override def preStart: Unit = println("PurchaseActor started")
  override def postStop: Unit = println("PurchaseActor stopped")

  override def receive = {
    case "init" => {
      this.dsClient = new DeepstreamClient("ec2-18-196-1-50.eu-central-1.compute.amazonaws.com:6020")
      val loginResult = this.dsClient.login()
      if (loginResult.loggedIn) {
        println("DeepstreamClient login")
      } else {
        throw new Exception("unable to initialise deepstream connection")
      }
      this.dsClient.rpc.provide("purchase", this)
    }
  }

  override def onRPCRequested(rpcName: String, data: scala.Any, response: RpcResponse) = {

    data match {
      case data: JsonObject => {
        val price = data.get("price").getAsDouble
        val name = data.get("name").getAsString

        if (this.failNext) {
          response.error("unable to purchase stock")
          this.failNext = false
        } else {
          response.send(null)
          this.failNext = true
        }
      }
      case _ => {
        response.error("incorrect data format received")
      }
    }
  }
}
