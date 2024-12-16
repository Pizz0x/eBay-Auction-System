package classes

import akka.actor.typed.ActorRef
import solutions.*


// Main class
case class Bid(bidder: ActorRef[BidderTrait], name: String, bankaccount: String, value: Double)
case class Auction(auctionId: String, item: String, startingPrice: Double, duration: Int) extends AuctionTrait
case class Bidder(name: String, bankaccount: String) extends AuctionTrait
case class State(bidder: Boolean, seller: Boolean, value: Double)