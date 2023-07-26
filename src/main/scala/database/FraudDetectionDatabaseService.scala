package database

import database.DatabaseConnection._
import model.OrderModels.Order

import java.sql.{Connection, Date}

class FraudDetectionDatabaseService {


  def isOrderAlreadyExists(orderNumber: String): Boolean = {
    val connection = getConnection()
    val preparedStatement = connection.prepareStatement("select * from fraud_detector_table where orderNum = ?")
    preparedStatement.setString(1, orderNumber)
    val resultSet = preparedStatement.executeQuery()
    resultSet.next() // If result set contains some element, then return true, because the order with given order number already exists
  }

  def saveFraudDetectionRecord(order: Order): Unit = {
    val connection = getConnection()
    val preparedStatement = connection.prepareStatement("insert into fraud_detector_table values (?,?,?,?,?,?,?,?,?)")
    preparedStatement.setString(1, order.user_id)
    preparedStatement.setString(2, order.order_id.toString)
    preparedStatement.setString(3, order.orderNumber)
    preparedStatement.setString(4, order.emailId)
    preparedStatement.setString(5, "")
    preparedStatement.setDouble(6, 0.0)
    preparedStatement.setInt(7, 1)
    preparedStatement.setDate(8, new Date(System.currentTimeMillis()))
    preparedStatement.setDate(9, new Date(System.currentTimeMillis()))
    val updateCount = preparedStatement.executeUpdate()
    if (updateCount == 0)
      throw new RuntimeException("Error while saving fraud entity in database")
  }
}
