package classes

import akka.actor.typed.ActorRef
import commands.*


// Main class
case class Bid(bidder: ActorRef[BidderCommand], name: String, bankaccount: String, value: Double)
case class Auction(auction: ActorRef[AuctionCommand], item: String, var amount: Double, seller: ActorRef[SellerCommand], var active: Boolean)
case class Bidder(name: String, bankaccount: String) extends AuctionCommand
case class State(bidder: Boolean, seller: Boolean, value: Double)