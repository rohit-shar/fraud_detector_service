package database

import database.DatabaseConnection._

import java.sql.{Connection, Date, PreparedStatement, ResultSet}

import model.OrderModels.Order

class FraudDetectionDatabaseService {

  /**
   * Checks if an order with the given order number already exists in the database.
   *
   * @param orderNumber The order number to check.
   * @return True if the order already exists, false otherwise.
   */
  def isOrderAlreadyExists(orderNumber: String): Boolean = {
    val connection = getConnection()
    // Prepare the SQL query with a parameterized statement to prevent SQL injection
    val preparedStatement: PreparedStatement =
      connection.prepareStatement("SELECT * FROM fraud_detector_table WHERE orderNum = ?")
    preparedStatement.setString(1, orderNumber)
    val resultSet: ResultSet = preparedStatement.executeQuery()
    resultSet.next() // If the result set contains any rows, return true; the order already exists
  }

  /**
   * Saves a fraud detection record into the database.
   *
   * @param order The order to be saved as a fraud detection record.
   * @throws RuntimeException if there is an error while saving the record.
   */
  def saveFraudDetectionRecord(order: Order): Unit = {
    val connection = getConnection()
    // Prepare the SQL query with a parameterized statement to prevent SQL injection
    val preparedStatement: PreparedStatement =
      connection.prepareStatement(
        "INSERT INTO fraud_detector_table (userId, orderId, orderNum, emailId, siftStatus, siftScore, hitCount, createdDate, editDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
      )
    preparedStatement.setString(1, order.user_id)
    preparedStatement.setString(2, order.order_id.toString)
    preparedStatement.setString(3, order.orderNumber)
    preparedStatement.setString(4, order.emailId)
    preparedStatement.setString(5, "")
    preparedStatement.setDouble(6, 0.0)
    preparedStatement.setInt(7, 1)
    preparedStatement.setDate(8, new Date(System.currentTimeMillis()))
    preparedStatement.setDate(9, new Date(System.currentTimeMillis()))

    // Execute the insert query and check if any row was updated
    val updateCount: Int = preparedStatement.executeUpdate()
    if (updateCount == 0) {
      throw new RuntimeException("Error while saving fraud entity in the database")
    }
  }
}
