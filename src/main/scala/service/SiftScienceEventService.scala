package service

import Entity.CreateOrderResponse
import com.siftscience.exception.SiftException
import com.siftscience.model.{Address, CreateOrderFieldSet, Item, PaymentMethod}
import com.siftscience.{EventRequest, EventResponse, SiftClient}
import model.OrderModels.{CartEntry, Order, PaymentGateway}
import net.liftweb.json.{NoTypeHints, Serialization}
import utils.UUIDSerializerDeserializer
import java.text.DecimalFormat
import java.util
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.SeqHasAsJava


class SiftScienceEventService {

  val apiKey = "e482be6bf53ee733"
  val accountKey = "5982f7bfe4b0769cbcc96d50"
  implicit var formats = Serialization.formats(NoTypeHints) + new UUIDSerializerDeserializer()
  val df2 = new DecimalFormat("###.##")
  // PAYMENT RELATED METHODS WHILE CALLING SIFT API
  /* Payment Related */
  val creditCardMethodType = "$credit_card"
  val paypalMethodType = "$electronic_fund_transfer"
  val creditCardGateway = "$authorizenet"
  val paypalGateway = "$paypal"

  def createOrderEvent(order: Order): CreateOrderResponse = {
    val client: SiftClient = new SiftClient(apiKey, accountKey)
    val payableAmount = Math.round(order.total_payble * 100)
    var orderResponse = CreateOrderResponse(order.orderNumber, 0.0, 0.0, null, null)
    // SETTING REQUIRED FIELDS
    var createOrder: CreateOrderFieldSet = new CreateOrderFieldSet()
    createOrder.setUserId(order.user_id)
    createOrder.setSessionId(order.httpData.sessionID)
    createOrder.setOrderId(order.orderNumber)
    createOrder.setUserEmail(order.emailId)
    createOrder.setAmount(payableAmount)
    createOrder.setCurrencyCode("USD")

    // SETTING BILLING ADDRESS
    createOrder.setBillingAddress(new Address()
      setName (order.billingAddress.name)
      setPhone (order.billingAddress.phoneNumber.toString)
      setAddress1 (order.billingAddress.address)
      setAddress2 (order.billingAddress.address2)
      setCity (order.billingAddress.city)
      setRegion (order.billingAddress.state)
      setCountry (order.billingAddress.country)
      setZipCode (order.billingAddress.zip))


    // SETTING UP THE BILLING ADDRESS
    createOrder.setShippingAddress(new Address()
      setName (order.listOfShippingAddressPerItem(0).shipping_address.name)
      setPhone (order.listOfShippingAddressPerItem(0).shipping_address.phoneNumber.toString)
      setAddress1 (order.listOfShippingAddressPerItem(0).shipping_address.address)
      setAddress2 (order.listOfShippingAddressPerItem(0).shipping_address.address2)
      setCity (order.listOfShippingAddressPerItem(0).shipping_address.city)
      setRegion (order.listOfShippingAddressPerItem(0).shipping_address.state)
      setCountry (order.listOfShippingAddressPerItem(0).shipping_address.country)
      setZipCode (order.listOfShippingAddressPerItem(0).shipping_address.zip))

    createOrder.setExpeditedShipping(true)
    createOrder.setShippingMethod("$physical")

    val paymentMethod: PaymentMethod = preparePaymentMethod(order)

    if (paymentMethod != null)
      createOrder.setPaymentMethods(util.Arrays.asList(paymentMethod))
    else
      println("Payment information not provided");

    // Items List
    createOrder.setItems(prepareItemsList(order.cart.cart_entries).asJava)

    createOrder.setSellerUserId("the_auto_parts_shop")
    createOrder.setCustomField("avs_status", "$pending")

    if (order.httpData.userIP == null || order.httpData.userIP.trim.isEmpty)
      createOrder.setIp("0.0.0.0")
    else if (isValidIpAddress(order.httpData.userIP))
      createOrder.setIp(order.httpData.userIP)
    else {
      try {
        val ip = order.httpData.userIP.substring(0, order.httpData.userIP.lastIndexOf(","))
        if (isValidIpAddress(ip))
          createOrder.setIp(ip)
        else
          createOrder.setIp("0.0.0.0")
      } catch {
        case exp: Exception => {
          println(s"Invalid Ip address exception. OrderNum: ${order.orderNumber}")
          createOrder.setIp("0.0.0.0")
        }
      }
    }

    val request: EventRequest = client.buildRequest(createOrder)


    var response: EventResponse = null;
    try {
      request.withWorkflowStatus()
      request.withScores("payment_abuse")
      response = request.send();
      println("REQUEST IS SENT")
      println(response.toString())
      orderResponse.siftResponse = Serialization.write(response)
      println("WRITTEN RESPONSE")
    } catch {
      case exp: SiftException => {
        println(exp.getApiErrorMessage())
        //        finalResponse.exception = exp.getApiErrorMessage()
      }


    }
    orderResponse
  }


  def isValidIpAddress(ip: String): Boolean = {
    val ipv4 = """^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$""".r
    val ipv6 = """^[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}\:[0-9abcdef]{1,4}$""".r

    if ((ipv4 findFirstIn ip).isDefined) {
      true
    } else if ((ipv6 findFirstIn ip).isDefined) {
      true
    } else {
      false
    }
  }


  def preparePaymentMethod(payload: Order): PaymentMethod = {
    var paymenthod: PaymentMethod = null
    if (payload.paymentResponse.paymentGateway.getOrElse("no_provided").equals(PaymentGateway.AUTHORISE_NET)) {
      paymenthod = new PaymentMethod()
        .setPaymentType(creditCardMethodType)
        .setPaymentGateway(creditCardGateway)
        .setCardLast4(payload.creditCardLastFourDigit)
        .setCardBin(payload.creditCardFirstSixDigit)
    } else if (payload.paymentResponse.paymentGateway.getOrElse("no_provided").equals(PaymentGateway.PAYPAL)) {
      paymenthod = new PaymentMethod()
        .setPaymentType(paypalMethodType)
        .setPaymentGateway(paypalGateway)
        .setPaypalPayerId(payload.paymentResponse.paypalPayerId.getOrElse("not_provided"))
        .setPaypalPayerEmail(payload.emailId)
        .setPaypalPaymentStatus(payload.paymentResponse.paymentTransactionStatus)
    }

    paymenthod;
  }


  def prepareItemsList(items: Map[String, CartEntry]): List[Item] = {
    var cartItems = ListBuffer[Item]()

    for (itemInfo <- items) {
      try {
        var item = new Item()
        item.setItemId(itemInfo._2.primaryId)
        item.setProductTitle(itemInfo._2.title)
        // Prepare Parice
        var totalPrice = df2.format(itemInfo._2.price * itemInfo._2.quantity).toDouble
        totalPrice = ((totalPrice * 100) * 10000)
        item.setPrice(totalPrice.toLong)
        item.setCurrencyCode("USD")
        item.setSku(itemInfo._2.sku_id)
        item.setBrand(itemInfo._2.brand)
        item.setManufacturer(itemInfo._2.distributor_group)
        item.setCategory(itemInfo._2.partType)
        item.setQuantity(itemInfo._2.quantity.toLong)
        cartItems += item
      } catch {
        case exp: Exception => {
          println("Unable to log item for sift. Exception - " + exp)
        }
      }
    }
    if (cartItems.isEmpty)
      null
    else
      cartItems.toList
  }

}
