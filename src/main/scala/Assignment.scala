package solutions

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.util.Random
import scala.collection.mutable
import scala.collection.mutable.*
import scala.concurrent.duration.*


trait SellerTrait
trait AuctionTrait
trait BidderTrait
trait BankTrait
trait eBayTrait
// Messages for Seller
case class CreateAuction(item: String, startingPrice: Double, duration: Int, replyTo: ActorRef[eBayTrait]) extends SellerTrait
case class NotifySeller(item: String, amount: Double, auction: ActorRef[AuctionTrait], bank: ActorRef[BankTrait], seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends SellerTrait
case class AuctionSold(item: String, auction: ActorRef[AuctionTrait]) extends SellerTrait
case class AuctionReturned(item: String, auction: ActorRef[AuctionTrait], bidder: ActorRef[BidderTrait], name: String, bankaccount: String) extends SellerTrait

// Messages for Auction
case class PlaceBid(name: String, bankaccount: String, amount: Double, bidder: ActorRef[BidderTrait], eBay: ActorRef[eBayTrait]) extends AuctionTrait
case class WithdrawBid(bidder: ActorRef[BidderTrait], eBay: ActorRef[eBayTrait]) extends AuctionTrait
case class ReBid(bidder: ActorRef[BidderTrait], eBay: ActorRef[eBayTrait]) extends AuctionTrait
case class AuctionRemoved(replyTo: ActorRef[eBayTrait]) extends AuctionTrait
case object AuctionEnded extends AuctionTrait

// Messages for Ebay Service
case class GetAvailableAuctions(bidder: ActorRef[BidderTrait]) extends eBayTrait
case class RegisterAuction(auction: ActorRef[AuctionTrait], item: String, startingPrice: Double) extends eBayTrait
case class FinishAuction(auction: ActorRef[AuctionTrait]) extends eBayTrait
case class UpdateAuction(auction: ActorRef[AuctionTrait], item: String, amount: Double) extends eBayTrait

// Messages for Bidder
case class BidRejected(item: String, reason: String) extends BidderTrait
case class BidAccepted(item: String) extends BidderTrait
case class BidWithdrawn(item: String) extends BidderTrait
case class BidSurpassed(item: String, amount: Double) extends BidderTrait
case class AvailableAuctions(auctions: mutable.Map[ActorRef[AuctionTrait], (String, Double)]) extends BidderTrait
case class AuctionDeleted(item: String) extends BidderTrait
case class NewWinner(message: String) extends BidderTrait
case class NotifyBidder(item: String, amount: Double, auction: ActorRef[AuctionTrait], bank: ActorRef[BankTrait], seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends BidderTrait
case class AuctionBought(item: String, auction: ActorRef[AuctionTrait]) extends BidderTrait
case class ReturnedSuccessfully(item: String) extends BidderTrait
case class NotReturned(item: String) extends BidderTrait
case class BidCanceled(item: String) extends BidderTrait

// Messages for Bank
case class RegisterAccount(name: String, bankaccount: String, money: Double) extends BankTrait
case class AuctionFinished(bid: Bid, seller: ActorRef[SellerTrait], item: String, replyTo: ActorRef[AuctionTrait]) extends BankTrait
case class SellerAcknowledge(auction: ActorRef[AuctionTrait], item: String, amount: Double, seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends BankTrait
case class BidderAcknowledge(auction: ActorRef[AuctionTrait], item: String, amount: Double, seller: ActorRef[SellerTrait], bidder: ActorRef[BidderTrait]) extends BankTrait
case class RefoundBidder(auction: ActorRef[AuctionTrait], bidder: Bidder) extends BankTrait

// Main class
case class Bid(bidder: ActorRef[BidderTrait], name: String, bankaccount: String, value: Double)
case class Auction(auctionId: String, item: String, startingPrice: Double, duration: Int) extends AuctionTrait
case class Bidder(name: String, bankaccount: String) extends AuctionTrait
case class State(bidder: Boolean, seller: Boolean, value: Double)

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


// Actor handling the visas
object SellerActor:
  def apply(eBay: ActorRef[eBayTrait], bank: ActorRef[BankTrait]): Behavior[SellerTrait] =
    Behaviors.setup{ context =>
      val auctionsSold = mutable.Map[String, Long]()
      Behaviors.receiveMessage {
        case CreateAuction(item, startingPrice, duration, replyTo) =>
          val auction = context.spawn(AuctionActor(item, startingPrice, duration, context.self, bank), s"Auction$item")
          context.log.info(s"Auction for $item created with starting price: $startingPrice and duration of $duration seconds.")
          replyTo ! RegisterAuction(auction, item, startingPrice)
          Behaviors.same
        case NotifySeller(item, amount, auction, bank, seller, bidder) =>
          context.log.info(s"Auction for item $item concluded with an offer of $amount")
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
      }
  }


object BidderActor:
  def apply(name: String, bankaccount: String, eBay: ActorRef[eBayTrait]): Behavior[BidderTrait] =
    Behaviors.receive { (context, message) =>
      message match
        case BidAccepted(item) =>
          context.log.info(s"Bid Accepted for $item")
        case BidRejected(item, reason) =>
          context.log.info(s"Bid Rejected for $item: $reason")
        case BidWithdrawn(item) =>
          context.log.info(s"Bid Withdrawn for $item")
        case BidSurpassed(auctionId, amount) =>
          context.log.info(s"Bid for the auction $auctionId has been surpassed, the bid now is of $amount")
        case AvailableAuctions(auctions) =>
          context.log.info("The available auctions are:")
          auctions.foreach(m => context.log.info(s"auction: ${m._1.path.name} for item ${m._2._1} with max bid: ${m._2._2} "))
          Random.shuffle(auctions).headOption match{
            case Some((auction, (item, price))) =>
              context.log.info(s"$name places a bid of ${price + 5.0} on $item")
              auction ! PlaceBid(name, bankaccount, price + 5.0, context.self, eBay)
            case None =>
              context.log.info(s"There are no auctions available to bid on")
          }
        case AuctionDeleted(item) =>
          context.log.info(s"The Auction for $item has been removed from the seller")
        case NewWinner(message) =>
          context.log.info(message)
        case NotifyBidder(item, amount, auction, bank, seller, bidder) =>
          context.log.info(s"You won the auction for $item with an amount of $amount")
          bank ! BidderAcknowledge(auction, item, amount, seller, bidder)
        case AuctionBought(item, auction) =>
          context.log.info(s"The Bank has confirmed the correctly closing of the auction, you won the item $item")
        case ReturnedSuccessfully(item) =>
          context.log.info(s"The item $item has been returned successfully")
        case NotReturned(item) =>
          context.log.info(s"The $item has not been returned successfully because the maximum time to return it already pass")
        case BidCanceled(item) =>
          context.log.info(s"Your bid for the item $item has been canceled due to insufficient balance")
        Behaviors.same
    }

object BankActor:
  def apply(eBay: ActorRef[eBayTrait]): Behavior[BankTrait] =
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
            seller ! NotifySeller(bid.name, bid.value, auction, context.self, seller, bid.bidder)
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
          context.log.info(s"Bidder ${bidder.name} refounded of $amount")
          Behaviors.same
      }
    }

object eBayActor:
  def apply(): Behavior[eBayTrait] =
    Behaviors.setup { context =>
      val auctions = mutable.Map[ActorRef[AuctionTrait], (String, Double)]()

      Behaviors.receiveMessage{
        case RegisterAuction(auction, item, startingPrice) =>
          auctions += auction -> (item, startingPrice)
          context.log.info(s"Auction ${auction.path.name} added to eBay")
          Behaviors.same
        case FinishAuction(auction) =>
          context.log.info(s"Auction Removed: ${auction.path.name}")
          auctions -= auction
          Behaviors.same
        case UpdateAuction(auction, item, amount) =>
          if(auctions.contains(auction)){
            auctions(auction) = (item, amount)
            context.log.info(s"The price of the auction ${auction.path.name} has been update to $amount")
          }
          Behaviors.same
        case GetAvailableAuctions(bidder) =>
          bidder ! AvailableAuctions(auctions)
          Behaviors.same
      }
    }



object Assignment extends App:
  val system: ActorSystem[eBayTrait] = ActorSystem(eBayActor(), "eBay Auction System")

  // Using Thread.sleep only for demonstration purposes
  Thread.sleep(9000)
  system.terminate()
