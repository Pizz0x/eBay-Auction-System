package events

import akka.actor.typed.ActorRef
import classes.*

trait BankEvent

case class RegisterAccountEvent(bidder: Bidder, money: Double) extends BankEvent
case class AuctionFinishedEvent(bidder: Bidder, value: Double) extends BankEvent
case class AcknowledgeEvent(auction_name: String, amount: Double) extends BankEvent
case class BidderAcknowledgeEvent(auction_name: String, amount: Double) extends BankEvent
case class SellerAcknowledgeEvent(auction_name: String, amount: Double) extends BankEvent
case class RefoundBidderEvent(bidder: Bidder, amount: Double) extends BankEvent

 
