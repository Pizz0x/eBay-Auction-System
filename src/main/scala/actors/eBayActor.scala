package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import messages.*
import classes.*

import scala.collection.mutable
import scala.concurrent.ExecutionContext


object eBayActor:
  def apply(): Behavior[eBayTrait] =
    Behaviors.setup { context =>
      val auctions = mutable.Map[String, Auction]()
      implicit val ec: ExecutionContext = context.system.executionContext

      Behaviors.receiveMessage {
        case RegisterAuction(auction, item, startingPrice, seller) =>
          auctions += auction.path.name -> Auction(auction, item, startingPrice, seller, true)
          context.log.info(s"Auction ${auction.path.name} added to eBay")
          Behaviors.same
        case FinishAuction(auction) =>
          context.log.info(s"Auction Removed or Expired: ${auction.path.name}")
          auctions(auction.path.name).active = false
          Behaviors.same
        case UpdateAuction(auction, item, amount) =>
          if (auctions.contains(auction.path.name) && auctions(auction.path.name).active) {
            auctions(auction.path.name).amount = amount
            context.log.info(s"The price of the auction ${auction.path.name} has been update to $amount")
          }
          Behaviors.same
        case GetAvailableAuctions(bidder) =>
          bidder ! AvailableAuctions(auctions.filter(_._2.active))
          Behaviors.same
        case PassBid(name, bankaccount, amount, auction, bidder) =>
          if (auctions(auction).active) then
            auctions(auction).auction ! PlaceBid(name, bankaccount, amount, bidder, context.self)
          else
            context.log.info(s"You cannot place a bid because the auction ${auction} is closed")
          Behaviors.same
        case PassWithdraw(auction, bidder) =>
          if (auctions(auction).active) then
            auctions(auction).auction ! WithdrawBid(bidder, context.self)
          else
            context.log.info(s"You cannot withdraw a bid because the auction $auction is already closed")
          Behaviors.same
        case PassReturn(auction, bidder, name, bankaccount) =>
          if (!auctions(auction).active) then
            val item = auctions(auction).item
            val a = auctions(auction).auction
            context.log.info(s"The bidder $name is trying to return the auction $auction")
            auctions(auction).seller ! AuctionReturned(item, a, bidder, name, bankaccount)
          else
            context.log.info(s"You cannot return the auction $auction because it's still active")
          Behaviors.same
        case RetryAuction(auction, item, startingPrice, duration, seller) =>
          if (auctions.contains(auction) && !auctions(auction).active && auctions(auction).seller == seller) then
            seller ! AuctionExisted(item, startingPrice, duration)
          else
            context.log.info(s"The auction $auction doesn't exist or it's already active")
          Behaviors.same
        case ReregisterAuction(auction, item, startingPrice, seller) =>
          auctions(auction.path.name).active = true
          auctions(auction.path.name).amount = startingPrice
          context.log.info(s"Auction ${auction.path.name} added again to eBay")
          Behaviors.same
      }
    }
