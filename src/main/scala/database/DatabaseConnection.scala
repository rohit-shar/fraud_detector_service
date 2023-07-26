package database

import java.sql.{Connection, DriverManager}

object DatabaseConnection {
  // Database connection parameters
  private val url = "jdbc:mysql://localhost/fraud_detector_database"
  private val driver = "com.mysql.cj.jdbc.Driver"
  private val username = "root"
  private val password = "password"
  private var connection: Connection = _

  /**
   * Get a connection to the database.
   *
   * @return A connection to the database.
   */
  def getConnection(): Connection = {
    if (connection != null) {
      // If a connection already exists, return it
      connection
    } else {
      try {
        // Load the JDBC driver and create a new connection
        Class.forName(driver)
        connection = DriverManager.getConnection(url, username, password)
        connection
      } catch {
        case e: Exception =>
          e.printStackTrace()
          null
      }
    }
  }
}
