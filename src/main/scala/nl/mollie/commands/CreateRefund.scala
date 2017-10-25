package nl.mollie.commands

case class CreateRefund(paymentId: String,
                        amount: Option[Double] = None,
                        description: Option[String] = None)
