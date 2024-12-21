package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import scala.collection.mutable.*
import scala.concurrent.duration.*
import commands.*
import classes.*
import events.*


object AuctionActor:

  def apply(item: String, startingPrice: Double, duration: Int, seller: ActorRef[SellerCommand], bank: ActorRef[BankCommand], eBay: ActorRef[eBayCommand]): Behavior[AuctionCommand] =
    Behaviors.setup { context =>
      implicit val bidOrdering: Ordering[Bid] = Ordering.by(_.value)

      Behaviors.withTimers { timers =>
        timers.startSingleTimer(AuctionEnded, duration.seconds)
        EventSourcedBehavior[AuctionCommand, AuctionEvent, AuctionState](
          persistenceId = PersistenceId.ofUniqueId(s"Auction$item"),
          emptyState = AuctionState(item, startingPrice, duration, seller, bank, eBay, ListBuffer.empty, None, true, System.currentTimeMillis() + duration * 1000),
          commandHandler = {(state, command) =>
            command match
              case PlaceBid(name, account, amount, bidder, eBay) if state.active =>
                if System.currentTimeMillis() > state.endTime then
                  Effect.none.thenRun(_ => bidder ! BidRejected(item, "The auction is expired"))
                else state.highestBid match {
                  case Some(bid) if amount <= bid.value =>
                    Effect.none.thenRun { _ =>
                      bidder ! BidRejected(item, s"The current price for the auction is ${bid.value}, so your bid was rejected")
                      state.bids.foreach { bid =>
                        bid.bidder ! BidSurpassed(item, amount)
                      }
                    }
                  case None if amount < startingPrice =>
                    Effect.none.thenRun(_ => bidder ! BidRejected(item, s"The starting price $startingPrice is higher than what you offer"))
                  case _ =>
                    val newbid = Bid(bidder, name, account, amount)
                    context.log.info(s"New Highest bid for $item of $amount made by $name")
                    Effect.persist(PlaceBidEvent(newbid)).thenRun { updateState =>
                      bidder ! BidAccepted(item)
                      eBay ! UpdateAuction(context.self, item, amount)
                    }
                }

              case WithdrawBid(bidder, eBay)
                if state.active =>
                if (state.bids.exists(_.bidder == bidder)) {
                  Effect.persist(WithdrawBidEvent(bidder)).thenRun{ updateState =>
                    bidder ! BidWithdrawn(state.item)
                    state.highestBid match {
                      case Some(bid) =>
                        val x = state.bids.maxBy(_.value)
                        x.bidder ! NewWinner(s"The max bid of the auction for $item has been withdraw, now you are again winning the auction with ${x.value}")
                        state.bids.foreach { bid =>
                          if bid.bidder != x._1 then
                            bid.bidder ! NewWinner(s"The max bid of the auction for $item has been withdraw, now the max bid is: ${x.value}")
                        }
                        eBay ! UpdateAuction(context.self, item, x.value)
                    }
                  }
                }
                else
                  Effect.none

              case AuctionRemoved(replyTo) =>
                Effect.persist(AuctionRemovedEvent).thenRun{ updateState =>
                  state.bids.foreach(bid =>
                    bid.bidder ! AuctionDeleted(state.item)
                  )
                  replyTo ! FinishAuction(context.self)
                }

              case AuctionEnded =>
                Effect.persist(AuctionEndedEvent).thenRun{ updateState =>
                  eBay ! FinishAuction(context.self)
                  state.highestBid match{
                    case Some(bid) =>
                      context.log.info(s"Auction for $item ended, the winner is ${bid.name} with a bid of ${bid.value}")
                      bank ! AuctionFinished(bid, seller, item, context.self)
                    case None =>
                      context.log.info(s"The auction for $item ended with no bids")
                  }
                }

              case ReBid(bidder, eBay) if !state.active =>
                Effect.persist(WithdrawBidEvent(bidder)).thenRun{ updateState =>
                  state.highestBid match {
                    case Some(bid) =>
                      val x = state.bids.maxBy(_.value)
                      x.bidder ! NewWinner(s"The max bid of the auction for $item has been canceled for bank problems, so you have won the auction with ${x.value}")
                      eBay ! UpdateAuction(context.self, item, x.value)
                      context.self ! AuctionEnded
                  }
                }

              case BeActive(newprice, newduration) if !state.active =>
                timers.startSingleTimer(AuctionEnded, duration.seconds)
                Effect.persist(BeActiveEvent(newprice, newduration)).thenRun(_ =>
                  context.log.info(s"Auction for $item has been re-started by his seller")
                )
          },
          eventHandler = {(state, event) =>
            event match
              case PlaceBidEvent(bid) =>
                state.copy(bids = state.bids += bid, highestBid = Some(bid))
              case WithdrawBidEvent(bidder) =>
                val x = state.bids.maxBy(_.value)
                state.copy(bids = state.bids.filterNot(_.bidder == bidder), highestBid = Some(x))
              case AuctionRemovedEvent =>
                state.copy(active = false)
              case AuctionEndedEvent =>
                state.copy(active = false)
              case BeActiveEvent(newprice, newduration) =>
                state.copy(active = true, startingPrice = newprice, duration = newduration)
          }
        )
      }
    }
