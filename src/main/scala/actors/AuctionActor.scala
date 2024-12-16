package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.collection.mutable.*
import scala.concurrent.duration.*
import messages.*
import classes.*


object AuctionActor:

  def apply(item: String, startingPrice: Double, duration: Int, seller: ActorRef[SellerTrait], bank: ActorRef[BankTrait]): Behavior[AuctionTrait] =
    Behaviors.setup { context =>
      implicit val bidOrdering: Ordering[Bid] = Ordering.by(_.value)
      var bids = ListBuffer[Bid]()
      var highestBid: Option[Bid] = None
      var active = true
      //val auctionId = s"$item-$startingPrice"
      val endTime = System.currentTimeMillis() + duration * 1000
      Behaviors.withTimers { timers =>
        timers.startSingleTimer(AuctionEnded, duration.seconds)

        Behaviors.receiveMessage {
          case PlaceBid(name, account, amount, bidder, eBay) if active =>
            if System.currentTimeMillis() > endTime then
              bidder ! BidRejected(item, "The auction is expired")
            else highestBid match {
              case Some(bid) if amount <= bid.value =>
                bidder ! BidRejected(item, s"The current price for the auction is ${bid.value}, so your bid was rejected")
                bids.foreach { bid =>
                  bid.bidder ! BidSurpassed(item, amount)
                }
              case None if amount < startingPrice =>
                bidder ! BidRejected(item, s"The starting price $startingPrice is higher than what you offer")
            }
            bids += Bid(bidder, account, name, amount)
            highestBid = Some(Bid(bidder, account, name, amount))
            context.log.info(s"New Highest bid for $item of $amount made by $name")
            bidder ! BidAccepted(item)
            eBay ! UpdateAuction(context.self, item, amount)
            Behaviors.same

          case WithdrawBid(bidder, eBay) if active =>
            val bidderbid = bids.filter(_.bidder == bidder)
            bids = bids.filterNot(_.bidder == bidder)
            bidder ! BidWithdrawn(item)
            highestBid match {
              case Some(bid) =>
                val x = bids.maxBy(_.value)
                highestBid = Some(x)
                x.bidder ! NewWinner(s"The max bid of the auction for $item has been withdraw, now you are again winning the auction with ${x.value}")
                bids.foreach { bid =>
                  if bid.bidder != x._1 then
                    bid.bidder ! NewWinner(s"The max bid of the auction for $item has been withdraw, now the max bid is: ${x.value}")
                }
                eBay ! UpdateAuction(context.self, item, x.value)
            }
            Behaviors.same

          case AuctionRemoved(replyTo) =>
            bids.foreach(bid =>
              bid.bidder ! AuctionDeleted(item)
            )
            replyTo ! FinishAuction(context.self)
            Behaviors.stopped

          case AuctionEnded =>
            active = false
            highestBid match {
              case Some(bid) =>
                context.log.info(s"Auction for $item ended, the winner is ${bid.name} with a bid of ${bid.value}")
                bank ! AuctionFinished(bid, seller, item, context.self)
              case None =>
                context.log.info(s"The auction for $item ended with no bids")
            }
            Behaviors.stopped

          case ReBid(bidder, eBay) if !active =>
            val bidderbid = bids.filter(_.bidder == bidder)
            bids = bids.filterNot(_.bidder == bidder)
            highestBid match {
              case Some(bid) =>
                val x = bids.maxBy(_.value)
                highestBid = Some(x)
                x.bidder ! NewWinner(s"The max bid of the auction for $item has been canceled for bank problems, so you have won the auction with ${x.value}")
                eBay ! UpdateAuction(context.self, item, x.value)
                context.self ! AuctionEnded
            }
            Behaviors.same
        }
      }
    }
