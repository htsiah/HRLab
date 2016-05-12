package utilities

import org.joda.time.DateTime
import scala.util.{Success, Failure}

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

case class Alert (
     _id: BSONObjectID,
     k: Int,
     m: String,
     js: Option[String],
     sys: Option[System]
)

object AlertUtility {

  // Use Reader to deserialize document automatically
  implicit object SystemBSONReader extends BSONDocumentReader[System] {
    def read(p_doc: BSONDocument): System = {
      System(
          p_doc.getAs[String]("eid").map(v => v),
          p_doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("mby").map(v => v),
          p_doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("dby").map(v => v),
          p_doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object AuthenticationBSONReader extends BSONDocumentReader[Alert] {
    def read(p_doc: BSONDocument): Alert = {
      Alert(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[BSONInteger]("k").get.value,
          p_doc.getAs[String]("m").get,
          p_doc.getAs[String]("js").map(v => v),
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("alert")
      
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Alert]
  }
  
}