package events

import akka.actor.typed.ActorRef
import classes.*

trait eBayEvent

case class RegisterAuctionEvent(auction_name: String, auction: Auction) extends eBayEvent
case class FinishAuctionEvent(auction_name: String) extends eBayEvent
case class UpdateAuctionEvent(auction_name: String, amount: Double) extends eBayEvent
case class ReregisterAuctionEvent(auction_name: String, startingPrice: Double) extends eBayEvent
 
 
