package events

import classes.*
import commands.*
import akka.actor.typed.ActorRef

trait AuctionEvent

case class PlaceBidEvent(bid: Bid) extends AuctionEvent
case class WithdrawBidEvent(bidder: ActorRef[BidderCommand]) extends AuctionEvent
case class ReBidEvent(bidder: ActorRef[BidderCommand], eBay: ActorRef[eBayCommand]) extends AuctionEvent
case object AuctionRemovedEvent extends AuctionEvent
case object AuctionEndedEvent extends AuctionEvent
case class BeActiveEvent(startingPrice: Double, duration: Int) extends AuctionEvent

