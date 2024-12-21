package events

import akka.actor.typed.ActorRef

trait SellerEvent

case class AuctionSoldEvent(auction: String) extends SellerEvent
 
