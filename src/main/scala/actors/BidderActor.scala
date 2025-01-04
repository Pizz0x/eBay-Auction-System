package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import classes.*
import commands.*

import scala.util.Random


object BidderActor:
  def apply(name: String, bankaccount: String, eBay: ActorRef[eBayCommand]): Behavior[BidderCommand] =
    Behaviors.setup{ context =>
      EventSourcedBehavior[BidderCommand, BidderEvent, BidderState](
        persistenceId = PersistenceId.ofUniqueId(s"Bidder-$bankaccount"),
        emptyState = BidderState(name, bankaccount, eBay),
        commandHandler = {(state, command) =>
          command match
            case BidAccepted(item) =>
              context.log.info(s"${name}: Bid Accepted for $item")
              Effect.none
            case BidRejected(item, reason) =>
              context.log.info(s"$name: Bid Rejected for $item: $reason")
              Effect.none
            case BidWithdrawn(item) =>
              context.log.info(s"$name: Bid Withdrawn for $item")
              Effect.none
            case BidSurpassed(auctionId, amount) =>
              context.log.info(s"$name: Bid for the auction $auctionId has been surpassed, the bid now is of $amount")
              Effect.none
            case AvailableAuctions(auctions) =>
              context.log.info("The available auctions are:")
              auctions.foreach(m => context.log.info(s"auction: ${m._1} for item ${m._2.item} with max bid: ${m._2.amount} "))
              Effect.none.thenRun { _ =>
                if auctions.nonEmpty then
                  val auction = auctions.toSeq(Random.nextInt(auctions.size))._2
                  context.log.info(s"$name: places a bid of ${auction.amount + 5.0} on ${auction.item}")
                  auction.auction ! PlaceBid(name, bankaccount, auction.amount + 5.0, context.self, eBay)
                else
                  context.log.info(s"There are no auctions available to bid on")
              }
            case AuctionDeleted(item) =>
              context.log.info(s"$name: The Auction for $item has been removed from the seller")
              Effect.none
            case NewWinner(message) =>
              context.log.info(message)
              Effect.none
            case NotifyBidder(item, amount, auction, bank, seller, bidder) =>
              Effect.none.thenRun { _ =>
                context.log.info(s"$name: You won the auction for $item with an amount of $amount")
                bank ! BidderAcknowledge(auction, item, amount, seller, bidder)
              }
            case AuctionBought(item, auction) =>
              context.log.info(s"$name: The Bank has confirmed the correctly closing of the auction, you won the item $item")
              Effect.none
            case ReturnedSuccessfully(item) =>
              context.log.info(s"$name: The item $item has been returned successfully")
              Effect.none
            case NotReturned(msg) =>
              context.log.info(msg)
              Effect.none
            case BidCanceled(item) =>
              context.log.info(s"$name: Your bid for the item $item has been canceled due to insufficient balance")
              Effect.none
            case CreateBid(auction, amount, item) =>
              Effect.none.thenRun { _ =>
                eBay ! PassBid(name, bankaccount, amount, auction, context.self)
                context.log.info(s"$name: places a bid of ${amount} on ${item}")
              }
            case RemoveBid(auction) =>
              Effect.none.thenRun(_ =>
                eBay ! PassWithdraw(auction, context.self)
              )
            case ReturnAuction(auction) =>
              Effect.none.thenRun(_ =>
                eBay ! PassReturn(auction, context.self, name, bankaccount)
              )
            case RandomBid =>
              Effect.none.thenRun(_ =>
                eBay ! GetAvailableAuctions(context.self)
              )
            case Refounded(msg) =>
              context.log.info(msg)
              Effect.none
            case NotWithdrawn(msg) =>
              context.log.info(msg)
              Effect.none
        },
        eventHandler = {(state, event) =>
          event match
            case _ =>
              state.copy(name = state.name)
        }
      )
    }