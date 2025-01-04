package commands

import akka.actor.typed.ActorRef
import scala.collection.mutable
import classes.*

trait BidderCommand
trait BidderEvent
// Messages for Bidder
case class BidRejected(item: String, reason: String) extends BidderCommand
case class BidAccepted(item: String) extends BidderCommand
case class BidWithdrawn(item: String) extends BidderCommand
case class BidSurpassed(item: String, amount: Double) extends BidderCommand
case class AvailableAuctions(auctions: mutable.Map[String, Auction]) extends BidderCommand
case class AuctionDeleted(item: String) extends BidderCommand
case class NewWinner(message: String) extends BidderCommand
case class NotifyBidder(item: String, amount: Double, auction: ActorRef[AuctionCommand], bank: ActorRef[BankCommand], seller: ActorRef[SellerCommand], bidder: ActorRef[BidderCommand]) extends BidderCommand
case class AuctionBought(item: String, auction: ActorRef[AuctionCommand]) extends BidderCommand
case class ReturnedSuccessfully(item: String) extends BidderCommand
case class NotReturned(msg: String) extends BidderCommand
case class BidCanceled(item: String) extends BidderCommand
case class CreateBid(auction: String, amount: Double, item: String) extends BidderCommand
case class RemoveBid(auction: String) extends BidderCommand
case class ReturnAuction(auction: String) extends BidderCommand
case object RandomBid extends BidderCommand
case class Refounded(msg: String) extends BidderCommand
case class NotWithdrawn(msg: String) extends BidderCommand