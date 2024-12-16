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
          context.log.info(s"Bid Accepted for $item")
        case BidRejected(item, reason) =>
          context.log.info(s"Bid Rejected for $item: $reason")
        case BidWithdrawn(item) =>
          context.log.info(s"Bid Withdrawn for $item")
        case BidSurpassed(auctionId, amount) =>
          context.log.info(s"Bid for the auction $auctionId has been surpassed, the bid now is of $amount")
        case AvailableAuctions(auctions) =>
          context.log.info("The available auctions are:")
          auctions.foreach(m => context.log.info(s"auction: ${m._1.path.name} for item ${m._2._1} with max bid: ${m._2._2} "))
          Random.shuffle(auctions).headOption match{
            case Some((auction, (item, price))) =>
              context.log.info(s"$name places a bid of ${price + 5.0} on $item")
              auction ! PlaceBid(name, bankaccount, price + 5.0, context.self, eBay)
            case None =>
              context.log.info(s"There are no auctions available to bid on")
          }
        case AuctionDeleted(item) =>
          context.log.info(s"The Auction for $item has been removed from the seller")
        case NewWinner(message) =>
          context.log.info(message)
        case NotifyBidder(item, amount, auction, bank, seller, bidder) =>
          context.log.info(s"You won the auction for $item with an amount of $amount")
          bank ! BidderAcknowledge(auction, item, amount, seller, bidder)
        case AuctionBought(item, auction) =>
          context.log.info(s"The Bank has confirmed the correctly closing of the auction, you won the item $item")
        case ReturnedSuccessfully(item) =>
          context.log.info(s"The item $item has been returned successfully")
        case NotReturned(item) =>
          context.log.info(s"The $item has not been returned successfully because the maximum time to return it already pass")
        case BidCanceled(item) =>
          context.log.info(s"Your bid for the item $item has been canceled due to insufficient balance")
      Behaviors.same
    }