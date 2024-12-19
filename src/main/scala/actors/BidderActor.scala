package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import classes.*
import messages.*

import scala.util.Random


object BidderActor:
  def apply(name: String, bankaccount: String, eBay: ActorRef[eBayTrait]): Behavior[BidderTrait] =
    Behaviors.receive { (context, message) =>
      message match
        case BidAccepted(item) =>
          context.log.info(s"$name: Bid Accepted for $item")
        case BidRejected(item, reason) =>
          context.log.info(s"$name: Bid Rejected for $item: $reason")
        case BidWithdrawn(item) =>
          context.log.info(s"$name: Bid Withdrawn for $item")
        case BidSurpassed(auctionId, amount) =>
          context.log.info(s"$name: Bid for the auction $auctionId has been surpassed, the bid now is of $amount")
        case AvailableAuctions(auctions) =>
          context.log.info("The available auctions are:")
          auctions.foreach(m => context.log.info(s"auction: ${m._1} for item ${m._2.item} with max bid: ${m._2.amount} "))
          if auctions.nonEmpty then
            val auction = auctions.toSeq(Random.nextInt(auctions.size))._2
            context.log.info(s"$name: places a bid of ${auction.amount + 5.0} on ${auction.item}")
            auction.auction ! PlaceBid(name, bankaccount, auction.amount + 5.0, context.self, eBay)
          else  
            context.log.info(s"There are no auctions available to bid on")
        case AuctionDeleted(item) =>
          context.log.info(s"$name: The Auction for $item has been removed from the seller")
        case NewWinner(message) =>
          context.log.info(message)
        case NotifyBidder(item, amount, auction, bank, seller, bidder) =>
          context.log.info(s"$name: You won the auction for $item with an amount of $amount")
          bank ! BidderAcknowledge(auction, item, amount, seller, bidder)
        case AuctionBought(item, auction) =>
          context.log.info(s"$name: The Bank has confirmed the correctly closing of the auction, you won the item $item")
        case ReturnedSuccessfully(item) =>
          context.log.info(s"$name: The item $item has been returned successfully")
        case NotReturned(item) =>
          context.log.info(s"$name The $item has not been returned successfully because the maximum time to return it already pass")
        case BidCanceled(item) =>
          context.log.info(s"$name: Your bid for the item $item has been canceled due to insufficient balance")
        case CreateBid(auction, amount, item) =>
          eBay ! PassBid(name, bankaccount, amount, auction, context.self)
          context.log.info(s"$name: places a bid of ${amount} on ${item}")
        case RemoveBid(auction) =>
          eBay ! PassWithdraw(auction, context.self)
        case ReturnAuction(auction) =>
          eBay ! PassReturn(auction, context.self, name, bankaccount)
      Behaviors.same
    }