package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class ConfigHolidays (
     _id: BSONObjectID,
     ctr: String,
     yr: String,
     nm: String,
     dat: String,
     day: String,
     sys: Option[System]
)

object ConfigHolidaysModel {
  
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
  
  implicit object ConfigKeywordBSONReader extends BSONDocumentReader[ConfigHolidays] {
    def read(p_doc: BSONDocument): ConfigHolidays = {
      ConfigHolidays(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("ctr").get,
          p_doc.getAs[String]("yr").get,
          p_doc.getAs[String]("nm").get,
          p_doc.getAs[String]("dat").get,
          p_doc.getAs[String]("day").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("holidays")
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ConfigHolidays](ReadPreference.primary).collect[List]()
  }
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ConfigHolidays]
  }
  
}