package database

import Entity.FraudDetectorTableEntity
import database.DatabaseConnection._
import model.FraudResponse
import model.OrderModels.Order
import utils.DatabaseUtil._

import java.sql.{Connection, Date}

class FraudDetectionDatabaseService {

  /**
   * Checks if an order with the given order number already exists in the database.
   *
   * @param orderNumber The order number to check.
   * @return True if the order already exists, false otherwise.
   */
  def isOrderAlreadyExists(orderNumber: String): Boolean = {
    val connection = getConnection()
    val preparedStatement = connection.prepareStatement(FRAUD_DETECTOR_RECORD_BY_ORDER_NUMBER)
    preparedStatement.setString(1, orderNumber)
    val resultSet = preparedStatement.executeQuery()
    resultSet.next() // If result set contains some element, then return true, because the order with given order number already exists
  }

  def getFraudRecordByOrderNumber(orderNumber: String): FraudDetectorTableEntity = {
    val connection = getConnection()
    val preparedStatement = connection.prepareStatement(FRAUD_DETECTOR_RECORD_BY_ORDER_NUMBER)
    preparedStatement.setString(1, orderNumber)
    val resultSet = preparedStatement.executeQuery()
    if (resultSet.next()) {
      val fraudDetectorTableEntity = FraudDetectorTableEntity(resultSet.getNString("userId"),
        resultSet.getNString("orderId"),
        resultSet.getNString("orderNum"),
        resultSet.getNString("emailId"),
        resultSet.getNString("siftStatus"),
        resultSet.getDouble("siftScore"),
        resultSet.getInt("hitCount"),
        resultSet.getDate("createdDate"),
        resultSet.getDate("editDate"))
      fraudDetectorTableEntity
    }
    else {
      throw new RuntimeException("Not able to find the fraud record with given orderNumber" + orderNumber)
    }
  }

  /**
   * Saves a fraud detection record into the database.
   *
   * @param order The order to be saved as a fraud detection record.
   * @throws RuntimeException if there is an error while saving the record.
   */
  def saveFraudDetectionRecord(order: Order): Unit = {
    val connection = getConnection()
    val preparedStatement = connection.prepareStatement(FRAUD_DETECTOR_RECORD_INSERT_QUERY)
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

  // Below methode is used for updating sift status and sift score of existing fraud record
  def updateExistingFraudEntryRecord(orderNumber: String, fraudResponse: FraudResponse): Int = {
    try {
      val connection = getConnection()
      val preparedStatement = connection.prepareStatement(FRAUD_RECORD_SIFT_STATUS_UPDATE_QUERY)
      preparedStatement.setDouble(1, fraudResponse.avg_score)
      preparedStatement.setString(2, fraudResponse.orderStatus)
      preparedStatement.setDate(3, new Date(System.currentTimeMillis()))
      preparedStatement.setString(4, orderNumber)
      val updatedCount = preparedStatement.executeUpdate()
      updatedCount
    }
    catch {
      case ex: RuntimeException => {
        ex.printStackTrace()
        0 // means 0 record is updated
      }
    }
  }


}
