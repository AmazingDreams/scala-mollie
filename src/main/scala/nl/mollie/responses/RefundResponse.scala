package nl.mollie.responses

/**
  * Example response from https://www.mollie.com/nl/docs/reference/refunds/create:
  *
  * {
  *    "id": "re_4qqhO89gsT",
  *    "payment": {
  *        "id": "tr_WDqYK6vllg",
  *        "mode": "test",
  *        "createdDatetime": "2017-10-24T05:04:49.0Z",
  *        "status": "refunded",
  *        "amount": "35.07",
  *        "amountRefunded": "5.95",
  *        "amountRemaining": "54.12",
  *        "description": "Order",
  *        "method": "ideal",
  *        "metadata": {
  *            "order_id": "33"
  *        },
  *        "details": {
  *            "consumerName": "Hr E G H K\u00fcppers en\/of MW M.J. K\u00fcppers-Veeneman",
  *            "consumerAccount": "NL53INGB0654422370",
  *            "consumerBic": "INGBNL2A"
  *        },
  *        "locale": "nl",
  *        "links": {
  *            "webhookUrl": "https://webshop.example.org/payments/webhook",
  *            "redirectUrl": "https://webshop.example.org/order/33/",
  *            "refunds": "https://api.mollie.nl/v1/payments/tr_WDqYK6vllg/refunds"
  *        }
  *    },
  *    "amount": "5.95",
  *    "description: "Order",
  *    "refundedDatetime": "2017-10-25T10:02:54.0Z"
  *  }
  */
case class RefundResponse(id: String,
                          payment: PaymentResponse,
                          amount: Double,
                          description: String,
                          refundedDatetime: String)
