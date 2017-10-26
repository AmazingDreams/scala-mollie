package nl.mollie

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import nl.mollie.commands.{CreatePayment, CreateRefund}
import nl.mollie.config.MollieConfig
import nl.mollie.connection.HttpServer
import nl.mollie.responses.{MollieFailure, PaymentResponse, RefundResponse}
import org.json4s.{DefaultFormats, FieldSerializer, Formats, Serialization, jackson}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import scala.util.Success

private case class CreateRefundInternal(amount: Option[Double] = None,
                                        description: Option[String] = None)

class MollieCommandActor(
    connection: HttpServer,
    config: MollieConfig
) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val system: ActorSystem = context.system
  implicit val materializer = ActorMaterializer()
  implicit val formats: Formats = DefaultFormats + FieldSerializer[CreatePayment]()
  implicit val jacksonSerialization: Serialization = jackson.Serialization

  log.info("Mollie command client started")

  def receive: Receive = {
    case cmd: CreatePayment => handleCreatePayment(cmd)
    case cmd: CreateRefund => handleCreateRefund(cmd)
  }

  private def handleCreatePayment(cmd: CreatePayment) = {
    val cmdSender = sender()

    Marshal(cmd).to[RequestEntity].flatMap { requestEntity =>
      connection.sendRequest(
        request = HttpRequest(
          uri = s"/${config.apiBasePath}/payments",
          method = HttpMethods.POST,
          entity = requestEntity
        )
      )
    }.onComplete {
      case Success(resp @ HttpResponse(StatusCodes.Created, headers, entity, _)) =>
        Unmarshal(entity).to[PaymentResponse]
          .recover {
            case e: Throwable =>
              log.error(s"Response: $resp, failed to create payment: {}", e)
              MollieFailure(s"Failed to create payment: $cmd")
          }
          .map(cmdSender ! _)
      case msg =>
        log.error("Response: {}, failed to create payment: {}", msg, cmd)
        cmdSender ! MollieFailure(s"Failed to create payment: $cmd")
    }
  }

  private def handleCreateRefund(cmd: CreateRefund) = {
    val cmdSender = sender()
    val refundWithoutPaymentId = CreateRefundInternal(
      amount = cmd.amount,
      description = cmd.description
    )

    Marshal(refundWithoutPaymentId).to[RequestEntity].flatMap { requestEntity =>
      connection.sendRequest(
        request = HttpRequest(
          uri = s"/${config.apiBasePath}/payments/${cmd.paymentId}/refunds",
          method = HttpMethods.POST,
          entity = requestEntity
        )
      )
    }.onComplete {
      case Success(resp @ HttpResponse(StatusCodes.Created, header, entity, _)) =>
        Unmarshal(entity).to[RefundResponse]
          .recover {
            case e: Throwable =>
              log.error(s"Response: $resp, failed to create refund: {}", e)
              MollieFailure(s"Failed to create refund: $cmd")
          }
          .map(cmdSender ! _)
      case msg =>
        log.error("response: {}, failed to create refund: {}", msg, cmd)
        cmdSender ! MollieFailure(s"Failed to create refund: $cmd")
    }
  }
}

object MollieCommandActor {

  final val name: String = "command"

  def props(
      connection: HttpServer,
      config: MollieConfig
  ): Props = Props(
    classOf[MollieCommandActor],
    connection,
    config
  )

}
