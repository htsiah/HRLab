package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class ConfigCurrencyCode (
     _id: BSONObjectID,
     ct: String,
     ccyn: String,
     ccyc: String,
     sys: Option[System]
)

object ConfigCurrencyCodeModel {
  
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
  
  implicit object ConfigCurrencyCodeBSONReader extends BSONDocumentReader[ConfigCurrencyCode] {
    def read(p_doc: BSONDocument): ConfigCurrencyCode = {
      ConfigCurrencyCode(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("ct").get,
          p_doc.getAs[String]("ccyn").get,
          p_doc.getAs[String]("ccyc").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("currencycode")
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ConfigCurrencyCode](ReadPreference.primary).collect[List]()
  }
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ConfigCurrencyCode]
  }
  
}