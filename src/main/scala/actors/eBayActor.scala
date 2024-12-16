package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import messages.*
import classes.*
import scala.collection.mutable


object eBayActor:
  def apply(): Behavior[eBayTrait] =
    Behaviors.setup { context =>
      val auctions = mutable.Map[ActorRef[AuctionTrait], (String, Double)]()

      Behaviors.receiveMessage{
        case RegisterAuction(auction, item, startingPrice) =>
          auctions += auction -> (item, startingPrice)
          context.log.info(s"Auction ${auction.path.name} added to eBay")
          Behaviors.same
        case FinishAuction(auction) =>
          context.log.info(s"Auction Removed: ${auction.path.name}")
          auctions -= auction
          Behaviors.same
        case UpdateAuction(auction, item, amount) =>
          if(auctions.contains(auction)){
            auctions(auction) = (item, amount)
            context.log.info(s"The price of the auction ${auction.path.name} has been update to $amount")
          }
          Behaviors.same
        case GetAvailableAuctions(bidder) =>
          bidder ! AvailableAuctions(auctions)
          Behaviors.same
      }
    }