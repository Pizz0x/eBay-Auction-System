package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import commands.*
import classes.*
import events.*

import scala.collection.mutable


object SellerActor:
  def apply(id: Int, eBay: ActorRef[eBayCommand], bank: ActorRef[BankCommand]): Behavior[SellerCommand] =
    Behaviors.setup{ context =>
      EventSourcedBehavior[SellerCommand, SellerEvent, SellerState](
        persistenceId = PersistenceId.ofUniqueId(s"Seller-$id"),
        emptyState = SellerState(id, eBay, bank, mutable.Map.empty),
        commandHandler = {(state, command) =>
          command match
            case CreateAuction(item, startingPrice, duration) =>
              Effect.none.thenRun{ _ =>
                val auction = context.spawn(AuctionActor(item, startingPrice, duration, context.self, bank, eBay), s"Auction$item")
                context.log.info(s"Auction for $item created with starting price: $startingPrice and duration of $duration seconds.")
                eBay ! RegisterAuction(auction, item, startingPrice, context.self)
              }
            case NotifySeller(item, name, amount, auction, bank, seller, bidder) =>
              Effect.none.thenRun{ _ =>
                context.log.info(s"Auction for item $item concluded with an offer of $amount by $name")
                bank ! SellerAcknowledge(auction, item, amount, seller, bidder)
              }
            case AuctionSold(item, auction) =>
              Effect.persist(AuctionSoldEvent(auction.path.name)).thenRun{ updateState =>
                context.log.info(s"The Bank has confirmed the correctly closing of the auction, you sold the item $item")
              }
            case AuctionReturned(item, auction, bidder, name, bankaccount) =>
              Effect.none.thenRun{ _ =>
                if System.currentTimeMillis() < (state.auctionsSold(auction.path.name) + 5000) then
                  context.log.info(s"The item $item has been returned")
                  bidder ! ReturnedSuccessfully(item)
                  bank ! RefoundBidder(auction, Bidder(name, bankaccount))
                else
                  bidder ! NotReturned(item)
              }
            case RecreateAuction(item, startingPrice, duration) =>
              Effect.none.thenRun{ _ =>
                eBay ! RetryAuction(s"Auction$item", item, startingPrice, duration, context.self)
              }
            case AuctionExisted(item, startingPrice, duration, auction) =>
              Effect.none.thenRun{ _ =>
                auction ! BeActive(startingPrice, duration)
                context.log.info(s"Auction for $item recreated with starting price: $startingPrice and duration of $duration seconds.")
                eBay ! ReregisterAuction(auction, item, startingPrice, context.self)
              } 
        },
        eventHandler = {(state, event) =>
          event match
            case AuctionSoldEvent(auction) =>
              state.copy(auctionsSold = state.auctionsSold += auction -> System.currentTimeMillis())
        }
      )
    }
