package commands

import akka.actor.typed.ActorRef
import classes.*

trait BankCommand
// Messages for Bank
case class RegisterAccount(name: String, bankaccount: String, money: Double) extends BankCommand
case class AuctionFinished(bid: Bid, seller: ActorRef[SellerCommand], item: String, replyTo: ActorRef[AuctionCommand]) extends BankCommand
case class SellerAcknowledge(auction: ActorRef[AuctionCommand], item: String, amount: Double, seller: ActorRef[SellerCommand], bidder: ActorRef[BidderCommand]) extends BankCommand
case class BidderAcknowledge(auction: ActorRef[AuctionCommand], item: String, amount: Double, seller: ActorRef[SellerCommand], bidder: ActorRef[BidderCommand]) extends BankCommand
case class RefoundBidder(auction: ActorRef[AuctionCommand], bidder: Bidder) extends BankCommand