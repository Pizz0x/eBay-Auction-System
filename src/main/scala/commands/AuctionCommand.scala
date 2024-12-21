package commands

import akka.actor.typed.ActorRef

trait AuctionCommand
// Messages for Auction
case class PlaceBid(name: String, bankaccount: String, amount: Double, bidder: ActorRef[BidderCommand], eBay: ActorRef[eBayCommand]) extends AuctionCommand
case class WithdrawBid(bidder: ActorRef[BidderCommand], eBay: ActorRef[eBayCommand]) extends AuctionCommand
case class ReBid(bidder: ActorRef[BidderCommand], eBay: ActorRef[eBayCommand]) extends AuctionCommand
case class AuctionRemoved(replyTo: ActorRef[eBayCommand]) extends AuctionCommand
case object AuctionEnded extends AuctionCommand
case class BeActive(startingPrice: Double, duration: Int) extends AuctionCommand