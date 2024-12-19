package classes

import akka.actor.typed.ActorRef
import messages.*


// Main class
case class Bid(bidder: ActorRef[BidderTrait], name: String, bankaccount: String, value: Double)
case class Auction(auction: ActorRef[AuctionTrait], item: String, var amount: Double, seller: ActorRef[SellerTrait], var active: Boolean)
case class Bidder(name: String, bankaccount: String) extends AuctionTrait
case class State(bidder: Boolean, seller: Boolean, value: Double)