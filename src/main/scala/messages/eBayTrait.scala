package messages

import akka.actor.typed.ActorRef

trait eBayTrait
// Messages for Ebay Service
case class GetAvailableAuctions(bidder: ActorRef[BidderTrait]) extends eBayTrait
case class RegisterAuction(auction: ActorRef[AuctionTrait], item: String, startingPrice: Double) extends eBayTrait
case class FinishAuction(auction: ActorRef[AuctionTrait]) extends eBayTrait
case class UpdateAuction(auction: ActorRef[AuctionTrait], item: String, amount: Double) extends eBayTrait