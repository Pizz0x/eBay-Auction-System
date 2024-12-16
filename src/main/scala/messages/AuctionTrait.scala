package messages

import akka.actor.typed.ActorRef

trait AuctionTrait
// Messages for Auction
case class PlaceBid(name: String, bankaccount: String, amount: Double, bidder: ActorRef[BidderTrait], eBay: ActorRef[eBayTrait]) extends AuctionTrait
case class WithdrawBid(bidder: ActorRef[BidderTrait], eBay: ActorRef[eBayTrait]) extends AuctionTrait
case class ReBid(bidder: ActorRef[BidderTrait], eBay: ActorRef[eBayTrait]) extends AuctionTrait
case class AuctionRemoved(replyTo: ActorRef[eBayTrait]) extends AuctionTrait
case object AuctionEnded extends AuctionTrait