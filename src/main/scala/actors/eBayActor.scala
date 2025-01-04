package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import commands.*
import classes.*
import events.*

import scala.collection.mutable
import scala.concurrent.ExecutionContext


object eBayActor:
  def apply(): Behavior[eBayCommand] =
    Behaviors.setup { context =>
      EventSourcedBehavior[eBayCommand, eBayEvent, eBayState](
        persistenceId = PersistenceId.ofUniqueId("eBay"),
        emptyState = eBayState(mutable.Map.empty),
        commandHandler = {(state, command) =>
          command match
            case RegisterAuction(auction, item, startingPrice, seller) =>
              val auc = Auction(auction, item, startingPrice, seller, true)
              Effect.persist(RegisterAuctionEvent(auction.path.name, auc)).thenRun{ updateState =>
                context.log.info(s"Auction ${auction.path.name} added to eBay")
              }

            case FinishAuction(auction) =>
              val auction_name = auction.path.name
              if(state.auctions.contains(auction_name)){
                Effect.persist(FinishAuctionEvent(auction_name)).thenRun(_ =>
                  context.log.info(s"Auction Removed or Expired: ${auction.path.name}")
                )
              }
              else
                context.log.info(s"Auction $auction_name not found")
                Effect.none

            case SuccessfullyFinished(auction) =>
              Effect.persist(FinishAuctionEvent(auction.path.name)).thenRun(_ =>
                context.log.info(s"Auction ${auction.path.name} is concluded")
              )

            case UpdateAuction(auction, item, amount) =>
              if (state.auctions.contains(auction.path.name) && state.auctions(auction.path.name).active) {
                Effect.persist(UpdateAuctionEvent(auction.path.name, amount)).thenRun{ updateState =>
                  context.log.info(s"The price of the auction ${auction.path.name} has been update to $amount")
                }
              }
              else
                Effect.none

            case GetAvailableAuctions(bidder) =>
              Effect.none.thenRun(_ =>
                bidder ! AvailableAuctions(state.auctions.filter(_._2.active))
              )

            case PassRemove(auction, seller) =>
              Effect.none.thenRun(_ =>
                state.auctions(auction).auction ! AuctionRemoved(seller)
              )

            case PassBid(name, bankaccount, amount, auction, bidder) =>
              Effect.none.thenRun { _ =>
                if (state.auctions(auction).active) then
                  state.auctions(auction).auction ! PlaceBid(name, bankaccount, amount, bidder, context.self)
                else
                  context.log.info(s"You cannot place a bid because the auction ${auction} is closed")
              }

            case PassWithdraw(auction, bidder) =>
              Effect.none.thenRun{ _ =>
                if (state.auctions(auction).active) then
                  state.auctions(auction).auction ! WithdrawBid(bidder, context.self)
                else
                  context.log.info(s"You cannot withdraw a bid because the auction $auction is already closed")
              }

            case PassReturn(auction, bidder, name, bankaccount) =>
              if (state.auctions.contains(auction) && !state.auctions(auction).active) then
                val item = state.auctions(auction).item
                val a = state.auctions(auction).auction
                Effect.none.thenRun{ _ =>
                  context.log.info(s"The bidder $name is trying to return the auction $auction")
                  state.auctions(auction).seller ! AuctionReturned(item, a, bidder, name, bankaccount)
                }
              else
                Effect.none.thenRun{ _ =>
                  bidder ! NotReturned(s"You cannot return the auction $auction because it doesn't exist or is still active")
                }

            case RetryAuction(auction, item, startingPrice, duration, seller) =>
              if (state.auctions.contains(auction) && !state.auctions(auction).active && state.auctions(auction).seller == seller) then
                Effect.none.thenRun(_ =>
                  seller ! AuctionExisted(item, startingPrice, duration, state.auctions(auction).auction)
                )
              else
                Effect.none.thenRun(_ =>
                  seller ! NoRecreation(s"You cannot recreate the auction: the auction $auction doesn't exist or it's already active")
                )

            case ReregisterAuction(auction, item, startingPrice, seller) =>
              Effect.persist(ReregisterAuctionEvent(auction.path.name, startingPrice)).thenRun(_ =>
                context.log.info(s"Auction ${auction.path.name} added again to eBay")
              )
        },
        eventHandler = {(state, event) =>
          event match
            case RegisterAuctionEvent(auction_name, auction) =>
              state.copy(auctions = state.auctions += auction_name -> auction)

            case FinishAuctionEvent(auction_name) =>
              state.copy(auctions = state.auctions.map{
                case(name, auction) if name == auction_name => name -> auction.copy(active = false)
                case other => other
              })

            case UpdateAuctionEvent(auction_name, amount) =>
              state.copy(auctions = state.auctions.map {
                case (name, auction) if name == auction_name => name -> auction.copy(amount = amount)
                case other => other
              })

            case ReregisterAuctionEvent(auction_name, startingPrice) =>
              state.copy(auctions = state.auctions.map {
                case (name, auction) if name == auction_name => name -> auction.copy(amount = startingPrice, active = true)
                case other => other
              })
        }
      )
    }
