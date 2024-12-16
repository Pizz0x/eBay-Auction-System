package messages

import akka.actor.typed.ActorRef
import classes.*

trait BankTrait
// Messages for Bank
case class RegisterAccount(name: String, bankaccount: String, money: Double) extends BankTrait
case class AuctionFinished(bid: Bid, seller: ActorRef[SellerTrait], item: String, replyTo: ActorRef[AuctionTrait]) extends BankTrait
case class SellerAcknowledge(auction: ActorRef[AuctionTrait], item: String, amount: Double, seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends BankTrait
case class BidderAcknowledge(auction: ActorRef[AuctionTrait], item: String, amount: Double, seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends BankTrait
case class RefoundBidder(auction: ActorRef[AuctionTrait], bidder: Bidder) extends BankTrait