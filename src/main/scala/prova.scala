package prova

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

// Messages
trait Validity
case object ValidPassport extends Validity
case object InvalidPassport extends Validity
case object VisaGranted extends Validity
case object VisaRefused extends Validity
case object ValidTickets extends Validity
case object InvalidTickets extends Validity

case class Visa(passport: Passport, replyTo: ActorRef[Validity]) extends Validity
case class Passport(name: String, endYear: Int, replyTo: ActorRef[Validity]) extends Validity
case class PlaneTicket(passport: Passport, dayOfWeekToPlane: String, replyTo: ActorRef[Validity]) extends Validity
case class RequestTrip(username: String, replyTo: ActorRef[Validity]) extends Validity
case class ResultProcess(message: String) extends Validity

// Actor handling the visas
object VisaActor:
  def apply(): Behavior[Validity] = Behaviors.receive { (context, message) =>
    message match
      case Visa(passport, replyTo) =>
        context.log.info("Processing Visa ...")
        context.log.info(s"The person ${passport.name} requested a Visa ...")

        if passport.endYear > 2024 then
          replyTo ! VisaGranted
        else
          replyTo ! VisaRefused

    Behaviors.same
  }

// Actor handling the passports
object PassportActor:
  def apply(): Behavior[Validity] = Behaviors.receive { (context, message) =>
    message match
      case Passport(name, _, replyTo) =>
        context.log.info("Processing Passport ...")
        context.log.info(s"The person $name is requesting a passport ...")

        if name.nonEmpty then
          replyTo ! ValidPassport
        else
          replyTo ! InvalidPassport

    Behaviors.same
  }

// Actor handling the plane tickets
object PlaneTicketActor:
  def apply(): Behavior[Validity] = Behaviors.receive { (context, message) =>
    message match
      case PlaneTicket(_, dayOfWeekToPlane, replyTo) =>
        context.log.info("Processing Tickets ...")

        if dayOfWeekToPlane.equals("Friday") then
          replyTo ! InvalidTickets
        else
          replyTo ! ValidTickets

    Behaviors.same
  }

// Actor as middle-man to 'aggregate' all missing parts
object TravelRequest:
  def apply(visa: ActorRef[Validity], passport: ActorRef[Validity], ticketActor: ActorRef[Validity]): Behavior[Validity] = Behaviors.receive { (context, message) =>
    message match
      case RequestTrip(username, replyTo) =>
        val aggregator = context.spawn(ProcessingAggregator(username, replyTo), "Processing")

        val passportObject = Passport(username, 2028, aggregator)

        // Using Thread.sleep only for demonstration purposes
        passport ! passportObject
        Thread.sleep(3000)

        visa ! Visa(passportObject, aggregator)
        Thread.sleep(3000)

        ticketActor ! PlaneTicket(passportObject, "Tuesday", aggregator)
        Thread.sleep(3000)

    Behaviors.same
  }

// Child actor that will process the documents
object ProcessingAggregator:
  var validDocuments: Int = 0

  def receiveResult(username: String, replyTo: ActorRef[Validity]): Behavior[Validity] = Behaviors.receive { (context, message) =>
    message match
      case ValidPassport =>
        context.log.info("Passport received and valid!")
        validDocuments += 1
      case InvalidPassport =>
        context.log.error("Passport received but not valid")
      case VisaGranted =>
        context.log.info("Visa granted!")
        validDocuments += 1
      case VisaRefused =>
        context.log.info("Sorry the country refused your request :(")
      case ValidTickets =>
        context.log.info("Tickets received and valid!")
        validDocuments += 1
      case InvalidTickets =>
        context.log.info("There's something wrong with the tickets")
    checkCompleteProcess(username, replyTo)
  }

  def checkCompleteProcess(username: String, replyTo: ActorRef[Validity]): Behavior[Validity] = Behaviors.setup {
    context =>
      context.log.info("Receiving parts ...")
      if validDocuments == 3 then
        validDocuments = 0
        context.log.info("Documents approved!")
        replyTo ! ResultProcess(s"The user $username is allowed to travel")
        Behaviors.stopped
      else
        context.log.info("Not all documents have been approved!")
        receiveResult(username, replyTo)
  }

  def apply(username: String, replyTo: ActorRef[Validity]): Behavior[Validity] = receiveResult(username, replyTo)

// Website 'frontpage' requesting a trip
object WebsiteTrip:
  def apply(): Behavior[Validity] = Behaviors.setup { context =>
    val visaActor = context.spawnAnonymous(VisaActor())
    val passportActor = context.spawnAnonymous(PassportActor())
    val planeTicketActor = context.spawnAnonymous(PlaneTicketActor())

    val travelRequest = context.spawnAnonymous(TravelRequest(visaActor, passportActor, planeTicketActor))
    travelRequest ! RequestTrip("Ahmed", context.self)

    Behaviors.receiveMessage { message =>
      message match
        case ResultProcess(msg) => context.log.info(msg)

      Behaviors.same
    }
  }

object Aggregator extends App:
  val system: ActorSystem[Validity] = ActorSystem(WebsiteTrip(), "Aggregator")

  // Using Thread.sleep only for demonstration purposes
  Thread.sleep(9000)
  system.terminate()
