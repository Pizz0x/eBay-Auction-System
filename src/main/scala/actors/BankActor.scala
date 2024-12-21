package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import scala.collection.mutable
import commands.*
import classes.*

object BankActor:
  def apply(eBay: ActorRef[eBayCommand]): Behavior[BankCommand] =
    Behaviors.setup { context =>
      val accounts = mutable.Map[Bidder, Double]()
      val transactions = mutable.Map[String, State]()
      Behaviors.receiveMessage{

        case RegisterAccount(name, bankaccount, money) =>
          accounts += Bidder(name, bankaccount) -> money
          Behaviors.same

        case AuctionFinished(bid, seller, item, auction) =>
          if accounts.contains(Bidder(bid.name, bid.bankaccount)) && accounts(Bidder(bid.name, bid.bankaccount))>=bid.value then
            accounts(Bidder(bid.name, bid.bankaccount)) -= bid.value
            bid.bidder ! NotifyBidder(item, bid.value, auction, context.self, seller, bid.bidder)
            seller ! NotifySeller(item, bid.name, bid.value, auction, context.self, seller, bid.bidder)
            eBay ! FinishAuction(auction)
          else
            auction ! ReBid(bid.bidder, eBay)
            bid.bidder ! BidCanceled(item)

          Behaviors.same

        case BidderAcknowledge(auction, item, amount, seller, bidder) =>
          if transactions.contains(auction.path.name) && transactions(auction.path.name)._2 && !transactions(auction.path.name)._1 then
            transactions(auction.path.name) = State(true, true, amount)
            seller ! AuctionSold(item, auction)
            bidder ! AuctionBought(item, auction)
          else
            transactions += auction.path.name -> State(true, false, amount)
          Behaviors.same

        case SellerAcknowledge(auction, item, amount, seller, bidder) =>
          if transactions.contains(auction.path.name) && !transactions(auction.path.name)._2 && transactions(auction.path.name)._1 then
            transactions(auction.path.name) = State(true, true, amount)
            seller ! AuctionSold(item, auction)
            bidder ! AuctionBought(item, auction)
          else
            transactions += auction.path.name -> State(false, true, amount)
          Behaviors.same

        case RefoundBidder(auction, bidder) =>
          val amount = transactions(auction.path.name)._3
          accounts(bidder) += amount
          context.log.info(s"Bidder ${bidder.name} refounded of $amount, now your amount is ${accounts(bidder)}")
          Behaviors.same
      }
    }