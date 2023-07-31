package utils

object DatabaseUtil {
  final val FRAUD_DETECTOR_RECORD_INSERT_QUERY = "insert into fraud_detector_table (userId,orderId,orderNum,emailId,siftStatus,siftScore,hitCount,createdDate,editDate) values (?,?,?,?,?,?,?,?,?)"
  final val FRAUD_DETECTOR_RECORD_BY_ORDER_NUMBER = "select * from fraud_detector_table where orderNum = ?"
  final val FRAUD_RECORD_SIFT_STATUS_UPDATE_QUERY = "update fraud_detector_table set siftScore=?, siftStatus=?, editDate=? where orderNum=?"
}
