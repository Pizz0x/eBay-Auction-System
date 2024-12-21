

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.util.Random
import scala.collection.mutable
import scala.collection.mutable.*
import scala.concurrent.duration.*
import commands.*
import classes.*
import actors.*

import scala.concurrent.ExecutionContext

object AuctionSystem:
  def apply(): Behavior[SystemTrait] = Behaviors.setup { context =>
    
    implicit val ec: ExecutionContext = context.system.executionContext
    var idseller = 1
    val ebay = context.spawn(eBayActor(), "eBay")
    val bank = context.spawn(BankActor(ebay), "Bank")
    
    val seller1 = context.spawn(SellerActor(idseller, ebay, bank), "Seller1")
    val seller2 = context.spawn(SellerActor(idseller, ebay, bank), "Seller2")
    
    val bidder1 = context.spawn(BidderActor(name = "Frank", bankaccount = "BE6753", eBay = ebay), "Bidder1")
    bank ! RegisterAccount("Frank", "BE6753", 5000)
    val bidder2 = context.spawn(BidderActor("Anne", "BE2257", ebay), "Bidder2")
    bank ! RegisterAccount("Anne", "BE2257", 7000)
    
    context.scheduleOnce(2.seconds, seller1, CreateAuction("Small_Sculpture2", 500, 18))
    context.scheduleOnce(2.seconds, seller1, CreateAuction("History_Book2", 30, 15))
    context.scheduleOnce(2.seconds, seller2, CreateAuction("Mobile_Phone2", 350, 17))
    context.scheduleOnce(2.seconds, seller2, CreateAuction("Computer2", 250, 16))
    
    context.scheduleOnce(4.seconds, ebay, GetAvailableAuctions(bidder1))
    context.scheduleOnce(5.seconds, ebay, GetAvailableAuctions(bidder2))
    context.scheduleOnce(6.seconds, ebay, GetAvailableAuctions(bidder1))
    context.scheduleOnce(7.seconds, ebay, GetAvailableAuctions(bidder2))
    
    context.scheduleOnce(9.seconds, bidder1, CreateBid("AuctionSmall_Sculpture2", 550, "Small_Sculpture2"))
    context.scheduleOnce(10.seconds, bidder2, CreateBid("AuctionSmall_Sculpture2", 580, "Small_Sculpture2"))
    context.scheduleOnce(11.seconds, bidder2, RemoveBid("AuctionSmall_Sculpture2"))
    context.scheduleOnce(17.seconds, bidder1, ReturnAuction("AuctionSmall_Sculpture2"))
    context.scheduleOnce(21.seconds, bidder1, ReturnAuction("AuctionSmall_Sculpture2"))
    context.scheduleOnce(22.seconds, seller1, RecreateAuction("History_Book2", 36, 6))
    context.scheduleOnce(24.seconds, ebay, GetAvailableAuctions(bidder2))


    Behaviors.same
  }
object Assignment extends App:
  
  val system: ActorSystem[SystemTrait] = ActorSystem(AuctionSystem(), "AuctionSystem")
  

  // Using Thread.sleep only for demonstration purposes
  Thread.sleep(30000)
  system.terminate()
