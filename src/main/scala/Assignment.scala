package solutions

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
    val seller3 = context.spawn(SellerActor(idseller, ebay, bank), "Seller3")
    val seller4 = context.spawn(SellerActor(idseller, ebay, bank), "Seller4")
    val seller5 = context.spawn(SellerActor(idseller, ebay, bank), "Seller5")
    val seller6 = context.spawn(SellerActor(idseller, ebay, bank), "Seller6")
    
    val bidder1 = context.spawn(BidderActor(name = "Frank", bankaccount = "BE6753", eBay = ebay), "Bidder1")
    bank ! RegisterAccount("Frank", "BE6753", 5000)
    val bidder2 = context.spawn(BidderActor("Anne", "BE2257", ebay), "Bidder2")
    bank ! RegisterAccount("Anne", "BE2257", 7000)
    val bidder3 = context.spawn(BidderActor(name = "Paul", bankaccount = "BE6722", eBay = ebay), "Bidder3")
    bank ! RegisterAccount("Paul", "BE6722", 5500)
    val bidder4 = context.spawn(BidderActor("Luke", "BE2337", ebay), "Bidder4")
    bank ! RegisterAccount("Luke", "BE2337", 7400)
    val bidder5 = context.spawn(BidderActor(name = "James", bankaccount = "BE1111", eBay = ebay), "Bidder5")
    bank ! RegisterAccount("James", "BE1111", 4000)
    val bidder6 = context.spawn(BidderActor("Mary", "BE2344", ebay), "Bidder6")
    bank ! RegisterAccount("Mary", "BE2344", 7000)
    val bidder7 = context.spawn(BidderActor(name = "Jenny", bankaccount = "BE9953", eBay = ebay), "Bidder7")
    bank ! RegisterAccount("Jenny", "BE9953", 5000)
    val bidder8 = context.spawn(BidderActor("Gloria", "BE7462", ebay), "Bidder8")
    bank ! RegisterAccount("Gloria", "BE7462", 7600)
    val bidder9 = context.spawn(BidderActor(name = "Mark", bankaccount = "BE9382", eBay = ebay), "Bidder9")
    bank ! RegisterAccount("Mark", "BE9382", 5000)
    val bidder10 = context.spawn(BidderActor("Macy", "BE2111", ebay), "Bidder10")
    bank ! RegisterAccount("Macy", "BE2111", 7000)
    
    context.scheduleOnce(2.seconds, seller1, CreateAuction("Small_Sculpture", 500, 18))
    context.scheduleOnce(2.seconds, seller1, CreateAuction("History_Book", 30, 13))
    context.scheduleOnce(2.seconds, seller2, CreateAuction("Mobile_Phone", 350, 13))
    context.scheduleOnce(2.seconds, seller2, CreateAuction("Computer", 250, 12))
    context.scheduleOnce(2.seconds, seller3, CreateAuction("Notebook", 30, 26))
    context.scheduleOnce(2.seconds, seller3, CreateAuction("Paint", 300, 13))
    context.scheduleOnce(2.seconds, seller4, CreateAuction("Chair", 50, 9))
    context.scheduleOnce(2.seconds, seller4, CreateAuction("Vase", 250, 12))
    context.scheduleOnce(2.seconds, seller6, CreateAuction("Jacket", 60, 19))
    context.scheduleOnce(2.seconds, seller2, CreateAuction("French_Book", 30, 13))
    context.scheduleOnce(2.seconds, seller5, CreateAuction("Wallet", 90, 16))
    context.scheduleOnce(2.seconds, seller5, CreateAuction("Shoes", 50, 20))
    
    context.scheduleOnce(4.seconds, bidder1, RandomBid)
    context.scheduleOnce(4.seconds, bidder2, RandomBid)
    context.scheduleOnce(4.seconds, bidder3, RandomBid)
    context.scheduleOnce(4.seconds, bidder4, RandomBid)
    context.scheduleOnce(5.seconds, bidder5, RandomBid)
    context.scheduleOnce(5.seconds, bidder6, RandomBid)
    context.scheduleOnce(5.seconds, bidder7, RandomBid)
    context.scheduleOnce(6.seconds, bidder8, RandomBid)
    context.scheduleOnce(7.seconds, bidder9, RandomBid)
    context.scheduleOnce(7.seconds, bidder10, RandomBid)
    context.scheduleOnce(7.seconds, bidder4, RandomBid)
    context.scheduleOnce(8.seconds, bidder5, RandomBid)
    context.scheduleOnce(8.seconds, bidder6, RandomBid)
    context.scheduleOnce(8.seconds, bidder8, RandomBid)
    context.scheduleOnce(9.seconds, bidder3, RandomBid)
    context.scheduleOnce(9.seconds, bidder2, RandomBid)
    context.scheduleOnce(10.seconds, bidder1, RandomBid)
    context.scheduleOnce(11.seconds, bidder9, RandomBid)
    context.scheduleOnce(11.seconds, bidder10, RandomBid)
    context.scheduleOnce(11.seconds, bidder6, RandomBid)
    context.scheduleOnce(12.seconds, bidder4, RandomBid)
    context.scheduleOnce(13.seconds, bidder5, RandomBid)
    context.scheduleOnce(13.seconds, bidder8, RandomBid)
    context.scheduleOnce(14.seconds, bidder2, RandomBid)
    context.scheduleOnce(15.seconds, bidder1, RandomBid)
    context.scheduleOnce(16.seconds, bidder5, RandomBid)
    context.scheduleOnce(17.seconds, bidder4, RandomBid)


    
    context.scheduleOnce(9.seconds, bidder1, CreateBid("AuctionSmall_Sculpture", 550, "Small_Sculpture"))
    context.scheduleOnce(10.seconds, bidder2, CreateBid("AuctionSmall_Sculpture", 580, "Small_Sculpture"))
    context.scheduleOnce(11.seconds, bidder2, RemoveBid("AuctionSmall_Sculpture"))
    context.scheduleOnce(17.seconds, bidder1, ReturnAuction("AuctionSmall_Sculpture"))
    context.scheduleOnce(25.seconds, seller3, RemoveAuction("AuctionNotebook"))
    context.scheduleOnce(21.seconds, bidder1, ReturnAuction("AuctionSmall_Sculpture"))
    context.scheduleOnce(22.seconds, seller1, RecreateAuction("History_Book", 36, 6))
    context.scheduleOnce(24.seconds, bidder2, RandomBid)


    Behaviors.same
  }
object Assignment extends App:
  
  val system: ActorSystem[SystemTrait] = ActorSystem(AuctionSystem(), "AuctionSystem")
  

  // Using Thread.sleep only for demonstration purposes
  Thread.sleep(32000)
  system.terminate()
