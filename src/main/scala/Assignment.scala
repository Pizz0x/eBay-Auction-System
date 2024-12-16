package solutions

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.util.Random
import scala.collection.mutable
import scala.collection.mutable.*
import scala.concurrent.duration.*
import messages.*
import classes.*
import actors.*


object Assignment extends App:
  val system: ActorSystem[eBayTrait] = ActorSystem(eBayActor(), "eBay Auction System")

  // Using Thread.sleep only for demonstration purposes
  Thread.sleep(9000)
  system.terminate()
