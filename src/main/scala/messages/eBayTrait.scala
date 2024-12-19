package messages

import akka.actor.typed.ActorRef

trait eBayTrait
// Messages for Ebay Service
case class GetAvailableAuctions(bidder: ActorRef[BidderTrait]) extends eBayTrait
case class RegisterAuction(auction: ActorRef[AuctionTrait], item: String, startingPrice: Double, seller: ActorRef[SellerTrait]) extends eBayTrait
case class FinishAuction(auction: ActorRef[AuctionTrait]) extends eBayTrait
case class UpdateAuction(auction: ActorRef[AuctionTrait], item: String, amount: Double) extends eBayTrait
case class PassBid(name: String, bankaccount: String, amount: Double, auction: String, bidder: ActorRef[BidderTrait]) extends eBayTrait
case class PassWithdraw(auction: String, bidder: ActorRef[BidderTrait]) extends eBayTrait
case class PassReturn(auction: String, bidder: ActorRef[BidderTrait], name: String, bankaccount: String) extends eBayTrait
case class RetryAuction(auction: String, item: String, startingPrice: Double, duration: Int, seller: ActorRef[SellerTrait]) extends eBayTrait
case class ReregisterAuction(auction: ActorRef[AuctionTrait], item: String, startingPrice: Double, seller: ActorRef[SellerTrait]) extends eBayTrait