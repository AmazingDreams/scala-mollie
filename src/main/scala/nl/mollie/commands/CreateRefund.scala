package nl.mollie.commands

case class CreateRefund(paymentId: String,
                        data: CreateRefundData = CreateRefundData())

case class CreateRefundData(amount: Option[Double] = None,
                            description: Option[String] = None)
