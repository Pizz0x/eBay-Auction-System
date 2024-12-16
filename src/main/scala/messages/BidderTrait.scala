package messages

import akka.actor.typed.ActorRef
import scala.collection.mutable

trait BidderTrait
// Messages for Bidder
case class BidRejected(item: String, reason: String) extends BidderTrait
case class BidAccepted(item: String) extends BidderTrait
case class BidWithdrawn(item: String) extends BidderTrait
case class BidSurpassed(item: String, amount: Double) extends BidderTrait
case class AvailableAuctions(auctions: mutable.Map[ActorRef[AuctionTrait], (String, Double)]) extends BidderTrait
case class AuctionDeleted(item: String) extends BidderTrait
case class NewWinner(message: String) extends BidderTrait
case class NotifyBidder(item: String, amount: Double, auction: ActorRef[AuctionTrait], bank: ActorRef[BankTrait], seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends BidderTrait
case class AuctionBought(item: String, auction: ActorRef[AuctionTrait]) extends BidderTrait
case class ReturnedSuccessfully(item: String) extends BidderTrait
case class NotReturned(item: String) extends BidderTrait
case class BidCanceled(item: String) extends BidderTrait