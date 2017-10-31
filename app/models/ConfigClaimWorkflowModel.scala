package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class ConfigClaimWorkflow (
    _id: BSONObjectID,
    n: String,
    d: Boolean,
    app: List[String],
    s: ConfigClaimWorkflowStatus,
    at: ConfigClaimWorkflowAssigned,
    caa: ConfigClaimWorkflowApprovedAmount,
    cg: ConfigClaimWorkflowAmountGreater,
    sys: Option[System]
)
    
case class ConfigClaimWorkflowStatus (
    s1: String,
    s2: String,
    s3: String,
    s4: String,
    s5: String,
    s6: String,
    s7: String,
    s8: String,
    s9: String,
    s10: String
)

case class ConfigClaimWorkflowAssigned (
    at1: String,
    at2: String,
    at3: String,
    at4: String,
    at5: String,
    at6: String,
    at7: String,
    at8: String,
    at9: String,
    at10: String
)

case class ConfigClaimWorkflowApprovedAmount (
    caa1: Boolean,
    caa2: Boolean,
    caa3: Boolean,
    caa4: Boolean,
    caa5: Boolean,
    caa6: Boolean,
    caa7: Boolean,
    caa8: Boolean,
    caa9: Boolean,
    caa10: Boolean
)

case class ConfigClaimWorkflowAmountGreater (
    cg1: Int,
    cg2: Int,
    cg3: Int,
    cg4: Int,
    cg5: Int,
    cg6: Int,
    cg7: Int,
    cg8: Int,
    cg9: Int,
    cg10: Int 
)


object ConfigClaimWorkflowModel {
  
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
  
  implicit object ConfigClaimWorkflowStatusBSONReader extends BSONDocumentReader[ConfigClaimWorkflowStatus] {
    def read(p_doc: BSONDocument): ConfigClaimWorkflowStatus = {
      ConfigClaimWorkflowStatus(
          p_doc.getAs[String]("s1").get,
          p_doc.getAs[String]("s2").get,
          p_doc.getAs[String]("s3").get,
          p_doc.getAs[String]("s4").get,
          p_doc.getAs[String]("s5").get,
          p_doc.getAs[String]("s6").get,
          p_doc.getAs[String]("s7").get,
          p_doc.getAs[String]("s8").get,
          p_doc.getAs[String]("s9").get,
          p_doc.getAs[String]("s10").get
      )
    }
  }
  
  implicit object ConfigClaimWorkflowAssignedBSONReader extends BSONDocumentReader[ConfigClaimWorkflowAssigned] {
    def read(p_doc: BSONDocument): ConfigClaimWorkflowAssigned = {
      ConfigClaimWorkflowAssigned(
          p_doc.getAs[String]("at1").get,
          p_doc.getAs[String]("at2").get,
          p_doc.getAs[String]("at3").get,
          p_doc.getAs[String]("at4").get,
          p_doc.getAs[String]("at5").get,
          p_doc.getAs[String]("at6").get,
          p_doc.getAs[String]("at7").get,
          p_doc.getAs[String]("at8").get,
          p_doc.getAs[String]("at9").get,
          p_doc.getAs[String]("at10").get
      )
    }
  }
  
  implicit object ConfigClaimWorkflowApprovedAmountBSONReader extends BSONDocumentReader[ConfigClaimWorkflowApprovedAmount] {
    def read(p_doc: BSONDocument): ConfigClaimWorkflowApprovedAmount = {
      ConfigClaimWorkflowApprovedAmount(
          p_doc.getAs[Boolean]("caa1").getOrElse(false),
          p_doc.getAs[Boolean]("caa2").getOrElse(false),
          p_doc.getAs[Boolean]("caa3").getOrElse(false),
          p_doc.getAs[Boolean]("caa4").getOrElse(false),
          p_doc.getAs[Boolean]("caa5").getOrElse(false),
          p_doc.getAs[Boolean]("caa6").getOrElse(false),
          p_doc.getAs[Boolean]("caa7").getOrElse(false),
          p_doc.getAs[Boolean]("caa8").getOrElse(false),
          p_doc.getAs[Boolean]("caa9").getOrElse(false),
          p_doc.getAs[Boolean]("caa10").getOrElse(false)
      )
    }
  }
  
  implicit object ConfigClaimWorkflowAmountGreaterBSONReader extends BSONDocumentReader[ConfigClaimWorkflowAmountGreater] {
    def read(p_doc: BSONDocument): ConfigClaimWorkflowAmountGreater = {
      ConfigClaimWorkflowAmountGreater(
          p_doc.getAs[BSONInteger]("cg1").get.value,
          p_doc.getAs[BSONInteger]("cg2").get.value,
          p_doc.getAs[BSONInteger]("cg3").get.value,
          p_doc.getAs[BSONInteger]("cg4").get.value,
          p_doc.getAs[BSONInteger]("cg5").get.value,
          p_doc.getAs[BSONInteger]("cg6").get.value,
          p_doc.getAs[BSONInteger]("cg7").get.value,
          p_doc.getAs[BSONInteger]("cg8").get.value,
          p_doc.getAs[BSONInteger]("cg9").get.value,
          p_doc.getAs[BSONInteger]("cg10").get.value
      )
    }
  }
  
  implicit object ConfigClaimWorkflowBSONReader extends BSONDocumentReader[ConfigClaimWorkflow] {
    def read(p_doc: BSONDocument): ConfigClaimWorkflow = {
      ConfigClaimWorkflow(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("n").get,
          p_doc.getAs[Boolean]("d").getOrElse(false),
          p_doc.getAs[List[String]]("app").get,
          p_doc.getAs[ConfigClaimWorkflowStatus]("s").get,
          p_doc.getAs[ConfigClaimWorkflowAssigned]("at").get,
          p_doc.getAs[ConfigClaimWorkflowApprovedAmount]("caa").get,
          p_doc.getAs[ConfigClaimWorkflowAmountGreater]("cg").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("claimworkflow")
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ConfigClaimWorkflow](ReadPreference.primary).collect[List]()
  }
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ConfigClaimWorkflow]
  }

}