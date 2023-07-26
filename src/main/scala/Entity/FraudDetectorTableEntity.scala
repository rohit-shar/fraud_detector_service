package Entity

import java.sql.Date


case class FraudDetectorTableEntity(userId: String, orderId: String, orderNum: String, emailId: String, var siftStatus: String, var siftScore: Double, var hitCount: Int, createdDate: Date, var editDate: Date)