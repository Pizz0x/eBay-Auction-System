package commands

import akka.actor.typed.ActorRef

trait eBayCommand
// Messages for Ebay Service
case class GetAvailableAuctions(bidder: ActorRef[BidderCommand]) extends eBayCommand
case class RegisterAuction(auction: ActorRef[AuctionCommand], item: String, startingPrice: Double, seller: ActorRef[SellerCommand]) extends eBayCommand
case class FinishAuction(auction: ActorRef[AuctionCommand]) extends eBayCommand
case class UpdateAuction(auction: ActorRef[AuctionCommand], item: String, amount: Double) extends eBayCommand
case class PassBid(name: String, bankaccount: String, amount: Double, auction: String, bidder: ActorRef[BidderCommand]) extends eBayCommand
case class PassWithdraw(auction: String, bidder: ActorRef[BidderCommand]) extends eBayCommand
case class PassReturn(auction: String, bidder: ActorRef[BidderCommand], name: String, bankaccount: String) extends eBayCommand
case class RetryAuction(auction: String, item: String, startingPrice: Double, duration: Int, seller: ActorRef[SellerCommand]) extends eBayCommand
case class ReregisterAuction(auction: ActorRef[AuctionCommand], item: String, startingPrice: Double, seller: ActorRef[SellerCommand]) extends eBayCommand