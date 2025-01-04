package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.collection.mutable
import commands.*
import classes.*
import events.*

object BankActor:
  def apply(eBay: ActorRef[eBayCommand]): Behavior[BankCommand] =
    Behaviors.setup { context =>
      EventSourcedBehavior[BankCommand, BankEvent, BankState](
        persistenceId = PersistenceId.ofUniqueId("IBank"),
        emptyState = BankState(eBay, mutable.Map.empty, mutable.Map.empty),
        commandHandler = { (state, command) =>
          command match
            case RegisterAccount(name, bankaccount, money) =>
              val bidder = Bidder(name, bankaccount)
              Effect.persist(RegisterAccountEvent(bidder, money)).thenRun{ _ =>
                context.log.info(s"Bank has registered the account of $name")
              }

            case AuctionFinished(bid, seller, item, auction) =>
              val bidder = Bidder(bid.name, bid.bankaccount)
              if state.accounts.contains(bidder) && state.accounts(bidder)>=bid.value then
                //accounts(Bidder(bid.name, bid.bankaccount)) -= bid.value
                Effect.persist(AuctionFinishedEvent(bidder, bid.value)).thenRun{ _ =>
                  bid.bidder ! NotifyBidder(item, bid.value, auction, context.self, seller, bid.bidder)
                  seller ! NotifySeller(item, bid.name, bid.value, auction, context.self, seller, bid.bidder)
                  //eBay ! FinishAuction(auction)
                }
              else
                Effect.none.thenRun{ _ =>
                  auction ! ReBid(bid.bidder, eBay)
                  bid.bidder ! BidCanceled(item)
                }

            case BidderAcknowledge(auction, item, amount, seller, bidder) =>
              if state.transactions.contains(auction.path.name) && state.transactions(auction.path.name)._2 && !state.transactions(auction.path.name)._1 then
                Effect.persist(AcknowledgeEvent(auction.path.name, amount)).thenRun { _ =>
                  seller ! AuctionSold(item, auction)
                  bidder ! AuctionBought(item, auction)
                }
              else
                Effect.persist(BidderAcknowledgeEvent(auction.path.name, amount))

            case SellerAcknowledge(auction, item, amount, seller, bidder) =>
              if state.transactions.contains(auction.path.name) && !state.transactions(auction.path.name)._2 && state.transactions(auction.path.name)._1 then
                //state.transactions(auction.path.name) = State(true, true, amount)
                Effect.persist(AcknowledgeEvent(auction.path.name, amount)).thenRun { _ =>
                  seller ! AuctionSold(item, auction)
                  bidder ! AuctionBought(item, auction)
                }
              else
                Effect.persist(SellerAcknowledgeEvent(auction.path.name, amount))
                //state.transactions += auction.path.name -> State(false, true, amount)

            case RefoundBidder(auction, bidder, bidder_address) =>
              val amount = state.transactions(auction.path.name)._3
              Effect.persist(RefoundBidderEvent(bidder, amount)).thenRun( _ =>
                bidder_address ! Refounded(s"Bidder ${bidder.name} refounded of $amount, now your amount is ${state.accounts(bidder) + amount}")
              )

        },
        eventHandler = {(state, event) =>
          event match
            case RegisterAccountEvent(bidder, money) =>
              state.copy(accounts = state.accounts += bidder -> money)

            case AuctionFinishedEvent(bidder, value) =>
              state.copy(accounts = state.accounts.map {
                case account if account._1 == bidder => account.copy(_2 = account._2 - value)
                case other => other
              })

            case AcknowledgeEvent(auction_name, amount) =>
              state.copy(transactions = state.transactions.map {
                case transaction if transaction._1 == auction_name => transaction.copy(_2 = State(true, true, amount))
                case other => other
              })

            case BidderAcknowledgeEvent(auction_name, amount) =>
              state.copy(transactions = state.transactions += auction_name -> State(true, false, amount))

            case SellerAcknowledgeEvent(auction_name, amount) =>
              state.copy(transactions = state.transactions += auction_name -> State(false, true, amount))

            case RefoundBidderEvent(bidder, amount) =>
              state.copy(accounts = state.accounts.map {
                case account if account._1 == bidder => account.copy(_2 = account._2 + amount)
                case other => other
              })
        }
      )
    }