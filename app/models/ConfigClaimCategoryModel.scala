package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class ConfigClaimCategory (
    _id: BSONObjectID,
    cat: String,      // Category
    all: Boolean,    // All, everyone
    app: List[String],     // Applicable
    tlim: Int,      // Transaction Limit
    hlp: String,     // Help Text
    sys: Option[System]
)

object ConfigClaimCategoryModel {
  
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
  
  implicit object ConfigClaimCategoryBSONReader extends BSONDocumentReader[ConfigClaimCategory] {
    def read(p_doc: BSONDocument): ConfigClaimCategory = {
      ConfigClaimCategory(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("cat").get,
          p_doc.getAs[Boolean]("all").getOrElse(false),
          p_doc.getAs[List[String]]("app").get,
          p_doc.getAs[BSONInteger]("tlim").get.value,
          p_doc.getAs[String]("hlp").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("claimcategory")
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ConfigClaimCategory](ReadPreference.primary).collect[List]()
  }
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ConfigClaimCategory]
  }
  
}