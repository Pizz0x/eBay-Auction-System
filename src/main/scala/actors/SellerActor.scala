package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import messages.*
import classes.*

import scala.collection.mutable


object SellerActor:
  def apply(eBay: ActorRef[eBayTrait], bank: ActorRef[BankTrait]): Behavior[SellerTrait] =
    Behaviors.setup{ context =>
      val auctionsSold = mutable.Map[String, Long]()
      Behaviors.receiveMessage {
        case CreateAuction(item, startingPrice, duration, replyTo) =>
          val auction = context.spawn(AuctionActor(item, startingPrice, duration, context.self, bank), s"Auction$item")
          context.log.info(s"Auction for $item created with starting price: $startingPrice and duration of $duration seconds.")
          replyTo ! RegisterAuction(auction, item, startingPrice)
          Behaviors.same
        case NotifySeller(item, amount, auction, bank, seller, bidder) =>
          context.log.info(s"Auction for item $item concluded with an offer of $amount")
          bank ! SellerAcknowledge(auction, item, amount, seller, bidder)
          Behaviors.same
        case AuctionSold(item, auction) =>
          context.log.info(s"The Bank has confirmed the correctly closing of the auction, you sold the item $item")
          auctionsSold += auction.path.name -> System.currentTimeMillis()
          Behaviors.same
        case AuctionReturned(item, auction, bidder, name, bankaccount) =>
          if System.currentTimeMillis() < (auctionsSold(auction.path.name)+5000) then
            context.log.info(s"The item $item has been returned")
            bidder ! ReturnedSuccessfully(item)
            bank ! RefoundBidder(auction, Bidder(name, bankaccount))
          else
            bidder ! NotReturned(item)
          Behaviors.same
      }
    }
