package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import commands.*
import classes.*

import scala.collection.mutable


object SellerActor:
  def apply(eBay: ActorRef[eBayCommand], bank: ActorRef[BankCommand]): Behavior[SellerCommand] =
    Behaviors.setup{ context =>
      val auctionsSold = mutable.Map[String, Long]()
      Behaviors.receiveMessage {
        case CreateAuction(item, startingPrice, duration) =>
          val auction = context.spawn(AuctionActor(item, startingPrice, duration, context.self, bank, eBay), s"Auction$item")
          context.log.info(s"Auction for $item created with starting price: $startingPrice and duration of $duration seconds.")
          eBay ! RegisterAuction(auction, item, startingPrice, context.self)
          Behaviors.same
        case NotifySeller(item, name, amount, auction, bank, seller, bidder) =>
          context.log.info(s"Auction for item $item concluded with an offer of $amount by $name")
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
        case RecreateAuction(item, startingPrice, duration) =>
          eBay ! RetryAuction(s"Auction$item", item, startingPrice, duration, context.self)
          Behaviors.same
        case AuctionExisted(item, startingPrice, duration, auction) =>
          auction ! BeActive(startingPrice, duration)
          context.log.info(s"Auction for $item recreated with starting price: $startingPrice and duration of $duration seconds.")
          eBay ! ReregisterAuction(auction, item, startingPrice, context.self)
          Behaviors.same
      }
    }
