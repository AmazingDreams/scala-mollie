package nl.mollie

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import nl.mollie.config.MollieConfig
import nl.mollie.connection.HttpServer
import nl.mollie.queries.{GetPayment, ListPaymentIssuers, ListPaymentMethods}
import nl.mollie.responses.{MollieFailure, PaymentIssuers, PaymentMethods, PaymentResponse}
import org.json4s.{DefaultFormats, Formats, Serialization, jackson}

import scala.util.Success

class MollieQueryActor(
    connection: HttpServer,
    config: MollieConfig
) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val system: ActorSystem = context.system
  implicit val materializer = ActorMaterializer()
  implicit val formats: Formats = DefaultFormats
  implicit val jacksonSerialization: Serialization = jackson.Serialization

  log.info("Mollie query client started")

  def receive: Receive = getPayment orElse listMethods orElse listIssuers

  private[this] def getPayment: Receive = {
    case qry: GetPayment =>
      val qrySender = sender()

      connection
        .sendRequest(
          request = HttpRequest(
            uri = s"/${config.apiBasePath}/payments/" + qry.id,
            method = HttpMethods.GET
          )
        )
        .onComplete {
          case Success(resp @ HttpResponse(StatusCodes.OK, headers, entity, _)) =>
            Unmarshal(entity).to[PaymentResponse]
              .recover {
                case e: Throwable =>
                  log.error(e, "Response: {}, failed to get payment: {}", resp, qry)
                  MollieFailure(s"failed to get payment: $qry")
              }
              .foreach(qrySender ! _)
          case msg =>
            log.error("Response: {}, failed to get payment: {}", msg, qry)
            qrySender ! MollieFailure(s"failed to get payment: $qry")
        }
  }

  private[this] def listMethods: Receive = {
    case qry: ListPaymentMethods =>
      val qrySender = sender()

      val params = Seq(
        qry.count.map(c => "count" -> c),
        qry.offset.map(o => "offset" -> o)
      ).flatten

      val queryString = if(params.nonEmpty)
        "?" + params.map(p => p._1 + "=" + p._2).mkString("&")
      else ""

      connection
        .sendRequest(
          request = HttpRequest(
            uri = s"/${config.apiBasePath}/methods" + queryString,
            method = HttpMethods.GET
          )
        )
        .onComplete {
          case Success(resp @ HttpResponse(StatusCodes.OK, headers, entity, _)) =>
            Unmarshal(entity).to[PaymentMethods]
              .recover {
                case e: Throwable =>
                  log.error(e, "Response: {}, failed to get payment methods: {}", resp, qry)
                  MollieFailure(s"Failed to get payment methods: $qry")
              }
              .foreach(qrySender ! _)
          case msg =>
            log.error("Response: {}, failed to get payment methods: {}", msg, qry)
            qrySender ! MollieFailure(s"Failed to get payment methods: $qry")
        }
  }

  private[this] def listIssuers: Receive = {
    case qry: ListPaymentIssuers =>
      val qrySender = sender()

      val params = Seq(
        qry.count.map(c => "count" -> c),
        qry.offset.map(o => "offset" -> o)
      ).flatten

      val queryString = if(params.nonEmpty)
        "?" + params.map(p => p._1 + "=" + p._2).mkString("&")
      else ""

      connection
        .sendRequest(
          request = HttpRequest(
            uri = s"/${config.apiBasePath}/issuers" + queryString,
            method = HttpMethods.GET
          )
        )
        .onComplete {
          case Success(resp @ HttpResponse(StatusCodes.OK, headers, entity, _)) =>
            Unmarshal(entity).to[PaymentIssuers]
              .recover {
                case e: Throwable =>
                  log.error(e, "Response: {}, failed to list payment issuers: {}", resp, qry)
                  MollieFailure(s"Failed to list payment issuers: $qry")
              }
              .foreach(qrySender ! _)
          case msg =>
            log.error("Response: {}, failed to list payment issuers: {}", msg, qry)
            qrySender ! MollieFailure(s"Failed to list payment issuers: $qry")
        }
  }

}

object MollieQueryActor {

  final val name: String = "query"

  def props(
      connection: HttpServer,
      config: MollieConfig
  ): Props = Props(
    classOf[MollieQueryActor],
    connection,
    config
  )

}