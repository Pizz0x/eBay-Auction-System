package messages

import akka.actor.typed.ActorRef

trait SellerTrait
// Messages for Seller
case class CreateAuction(item: String, startingPrice: Double, duration: Int, replyTo: ActorRef[eBayTrait]) extends SellerTrait
case class NotifySeller(item: String, amount: Double, auction: ActorRef[AuctionTrait], bank: ActorRef[BankTrait], seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends SellerTrait
case class AuctionSold(item: String, auction: ActorRef[AuctionTrait]) extends SellerTrait
case class AuctionReturned(item: String, auction: ActorRef[AuctionTrait], bidder: ActorRef[BidderTrait], name: String, bankaccount: String) extends SellerTrait
