package classes

import akka.actor.typed.ActorRef
import commands.*

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

case class BidderState(name: String, bankaccount: String, eBay: ActorRef[eBayCommand])
case class AuctionState(item: String, startingPrice: Double, duration: Int, seller: ActorRef[SellerCommand], bank: ActorRef[BankCommand], eBay: ActorRef[eBayCommand], bids: ListBuffer[Bid] , highestBid: Option[Bid], active: Boolean, endTime: Long)
case class SellerState(eBay: ActorRef[eBayCommand], bank: ActorRef[BankCommand], auctionsSold: mutable.Map[String, Long])
case class BankState(eBay: ActorRef[eBayCommand], accounts: mutable.Map[Bidder, Double], transactions: mutable.Map[String, State])
case class eBayState(auctions: mutable.Map[String, Auction])
