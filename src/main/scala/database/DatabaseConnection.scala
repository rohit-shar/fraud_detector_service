package database

import java.sql.{Connection, DriverManager}

object DatabaseConnection {
  val url = "jdbc:mysql://localhost/fraud_detector_database"
  val driver = "com.mysql.cj.jdbc.Driver"
  val username = "root"
  val password = "password"
  var connection: Connection = _

  def getConnection(): Connection = {
    if (connection != null) {
      connection
    }
    else {
      try {
        Class.forName(driver)
        connection = DriverManager.getConnection(url, username, password)
        connection
      } catch {
        case e: Exception => e.printStackTrace
          null
      }
    }
  }

}
