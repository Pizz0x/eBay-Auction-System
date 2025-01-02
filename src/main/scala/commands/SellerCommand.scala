package commands

import akka.actor.typed.ActorRef

trait SellerCommand
// Messages for Seller
case class CreateAuction(item: String, startingPrice: Double, duration: Int) extends SellerCommand
case class NotifySeller(item: String, name: String, amount: Double, auction: ActorRef[AuctionCommand], bank: ActorRef[BankCommand], seller: ActorRef[SellerCommand], bidder: ActorRef[BidderCommand]) extends SellerCommand
case class AuctionSold(item: String, auction: ActorRef[AuctionCommand]) extends SellerCommand
case class RemoveAuction(auction: String) extends SellerCommand
case class RecreateAuction(item: String, startingPrice: Double, duration: Int) extends SellerCommand
case class AuctionReturned(item: String, auction: ActorRef[AuctionCommand], bidder: ActorRef[BidderCommand], name: String, bankaccount: String) extends SellerCommand
case class AuctionExisted(item: String, startingPrice: Double, duration: Int, auction: ActorRef[AuctionCommand]) extends SellerCommand