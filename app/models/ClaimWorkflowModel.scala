package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore,DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class ClaimWorkflow (
    _id: BSONObjectID,
    n: String,
    d: Boolean,
    app: List[String],
    s: ClaimWorkflowStatus,
    at: ClaimWorkflowAssigned,
    caa: ClaimWorkflowApprovedAmount,
    cg: ClaimWorkflowAmountGreater,
    sys: Option[System]
)
    
case class ClaimWorkflowStatus (
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

case class ClaimWorkflowAssigned (
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

case class ClaimWorkflowApprovedAmount (
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

case class ClaimWorkflowAmountGreater (
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


object ClaimWorkflowModel {

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
  
  implicit object ClaimWorkflowStatusBSONReader extends BSONDocumentReader[ClaimWorkflowStatus] {
    def read(p_doc: BSONDocument): ClaimWorkflowStatus = {
      ClaimWorkflowStatus(
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
  
  implicit object ClaimWorkflowAssignedBSONReader extends BSONDocumentReader[ClaimWorkflowAssigned] {
    def read(p_doc: BSONDocument): ClaimWorkflowAssigned = {
      ClaimWorkflowAssigned(
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
  
  implicit object ClaimWorkflowApprovedAmountBSONReader extends BSONDocumentReader[ClaimWorkflowApprovedAmount] {
    def read(p_doc: BSONDocument): ClaimWorkflowApprovedAmount = {
      ClaimWorkflowApprovedAmount(
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
  
  implicit object ClaimWorkflowAmountGreaterBSONReader extends BSONDocumentReader[ClaimWorkflowAmountGreater] {
    def read(p_doc: BSONDocument): ClaimWorkflowAmountGreater = {
      ClaimWorkflowAmountGreater(
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
  
  implicit object ClaimWorkflowBSONReader extends BSONDocumentReader[ClaimWorkflow] {
    def read(p_doc: BSONDocument): ClaimWorkflow = {
      ClaimWorkflow(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("n").get,
          p_doc.getAs[Boolean]("d").getOrElse(false),
          p_doc.getAs[List[String]]("app").get,
          p_doc.getAs[ClaimWorkflowStatus]("s").get,
          p_doc.getAs[ClaimWorkflowAssigned]("at").get,
          p_doc.getAs[ClaimWorkflowApprovedAmount]("caa").get,
          p_doc.getAs[ClaimWorkflowAmountGreater]("cg").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(p_doc: System): BSONDocument = {
      BSONDocument(
          "eid" -> p_doc.eid,
          "cdat" -> p_doc.cdat.map(date => BSONDateTime(date.getMillis)),
          "mdat" -> p_doc.mdat.map(date => BSONDateTime(date.getMillis)),
          "mby" -> p_doc.mby,
          "ddat" -> p_doc.ddat.map(date => BSONDateTime(date.getMillis)),
          "dby" -> p_doc.dby,
          "ll" -> p_doc.ll.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  implicit object ClaimWorkflowStatusBSONWriter extends BSONDocumentWriter[ClaimWorkflowStatus] {
    def write(p_doc: ClaimWorkflowStatus): BSONDocument = {
      BSONDocument(
          "s1" -> p_doc.s1,
          "s2" -> p_doc.s2,
          "s3" -> p_doc.s3,
          "s4" -> p_doc.s4,
          "s5" -> p_doc.s5,
          "s6" -> p_doc.s6,
          "s7" -> p_doc.s7,
          "s8" -> p_doc.s8,
          "s9" -> p_doc.s9,
          "s10" -> p_doc.s10
      )     
    }
  }
  
  implicit object ClaimWorkflowAssignedBSONWriter extends BSONDocumentWriter[ClaimWorkflowAssigned] {
    def write(p_doc: ClaimWorkflowAssigned): BSONDocument = {
      BSONDocument(
          "at1" -> p_doc.at1,
          "at2" -> p_doc.at2,
          "at3" -> p_doc.at3,
          "at4" -> p_doc.at4,
          "at5" -> p_doc.at5,
          "at6" -> p_doc.at6,
          "at7" -> p_doc.at7,
          "at8" -> p_doc.at8,
          "at9" -> p_doc.at9,
          "at10" -> p_doc.at10
      )     
    }
  }
  
  implicit object ClaimWorkflowApprovedAmountBSONWriter extends BSONDocumentWriter[ClaimWorkflowApprovedAmount] {
    def write(p_doc: ClaimWorkflowApprovedAmount): BSONDocument = {
      BSONDocument(
          "caa1" -> p_doc.caa1,
          "caa2" -> p_doc.caa2,
          "caa3" -> p_doc.caa3,
          "caa4" -> p_doc.caa4,
          "caa5" -> p_doc.caa5,
          "caa6" -> p_doc.caa6,
          "caa7" -> p_doc.caa7,
          "caa8" -> p_doc.caa8,
          "caa9" -> p_doc.caa9,
          "caa10" -> p_doc.caa10
      )     
    }
  }
  
  implicit object ClaimWorkflowAmountGreaterBSONWriter extends BSONDocumentWriter[ClaimWorkflowAmountGreater] {
    def write(p_doc: ClaimWorkflowAmountGreater): BSONDocument = {
      BSONDocument(
          "cg1" -> p_doc.cg1,
          "cg2" -> p_doc.cg2,
          "cg3" -> p_doc.cg3,
          "cg4" -> p_doc.cg4,
          "cg5" -> p_doc.cg5,
          "cg6" -> p_doc.cg6,
          "cg7" -> p_doc.cg7,
          "cg8" -> p_doc.cg8,
          "cg9" -> p_doc.cg9,
          "cg10" -> p_doc.cg10
      )     
    }
  }
  
  implicit object ClaimWorkflowBSONWriter extends BSONDocumentWriter[ClaimWorkflow] {
    def write(p_doc: ClaimWorkflow): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "n" -> p_doc.n,
          "d" -> p_doc.d,
          "app" -> p_doc.app,
          "s" -> p_doc.s,
          "at" -> p_doc.at,
          "caa" -> p_doc.caa,
          "cg" -> p_doc.cg,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.claim_db.collection("claimworkflow")
  
    val doc = ClaimWorkflow(
      _id = BSONObjectID.generate,
      n = "",
      d = false,
      app = List(""),
      s = ClaimWorkflowStatus(s1="", s2="", s3="", s4="", s5="", s6="", s7="", s8="", s9="", s10=""),
      at = ClaimWorkflowAssigned(at1="", at2="", at3="", at4="", at5="", at6="", at7="", at8="", at9="", at10=""),
      caa = ClaimWorkflowApprovedAmount(caa1=false, caa2=false, caa3=false, caa4=false, caa5=false, caa6=false, caa7=false, caa8=false, caa9=false, caa10=false),
      cg = ClaimWorkflowAmountGreater(cg1=0, cg2=0, cg3=0, cg4=0, cg5=0, cg6=0, cg7=0, cg8=0, cg9=0, cg10=0),
      sys = None
  )
    
  private def updateSystem(p_doc:ClaimWorkflow) = {
    val eid = p_doc.sys.get.eid.getOrElse(None)
    val cdat = p_doc.sys.get.cdat.getOrElse(None)
    val mdat = p_doc.sys.get.mdat.getOrElse(None)
    val mby = p_doc.sys.get.mby.getOrElse(None)
    val ddat = p_doc.sys.get.ddat.getOrElse(None)
    val dby = p_doc.sys.get.dby.getOrElse(None)
    val ll = p_doc.sys.get.ll.getOrElse(None)
    val sys_doc = System(
        eid = if (eid!=None) {Some(p_doc.sys.get.eid.get)} else {None},
        cdat = if (cdat!=None) {Some(p_doc.sys.get.cdat.get)} else {None},
        mdat = if (mdat!=None) {Some(p_doc.sys.get.mdat.get)} else {None},
        mby = if (mby!=None) {Some(p_doc.sys.get.mby.get)} else {None},
        ddat = if (ddat!=None) {Some(p_doc.sys.get.ddat.get)} else {None},
        dby = if (dby!=None) {Some(p_doc.sys.get.dby.get)} else {None},
        ll= if (ll!=None) {Some(p_doc.sys.get.ll.get)} else {None}
    ) 
    sys_doc
  }
   		
  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.foreach { doc => 
        val future = col.update(BSONDocument("_id" -> doc._id, "sys.ddat"->BSONDocument("$exists"->false)), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
        }
      }
    }
  }
  
  // Delete document
  def removePermanently(p_query:BSONDocument) = {
    val future = col.remove(p_query)
  }
	
  // Find all documents
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ClaimWorkflow](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[ClaimWorkflow](ReadPreference.primary).collect[List]()
  }
	
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ClaimWorkflow]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[ClaimWorkflow]
  }
  
  /** Custom Model Methods **/ 

  def insert(p_doc:ClaimWorkflow, p_eid:String="", p_request:RequestHeader=null)= {
    if (p_doc.d) {
      for {
        default_workflow <- this.findOne(BSONDocument("d"->true), p_request)
      } yield {
        val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {
            // Change default to false
            col.update(BSONDocument("_id" -> default_workflow.get._id), default_workflow.get.copy(d=false, sys = SystemDataStore.modifyWithSystem(this.updateSystem(default_workflow.get), p_request)))
          }
        } 
      }
    } else {
      val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  def update(p_query:BSONDocument, p_doc:ClaimWorkflow, p_request:RequestHeader) = {
    if (p_doc.d) {
      for {
        default_workflow <- this.findOne(BSONDocument("d"->true), p_request)
      } yield {
        val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {
            // Change default to false
            if (default_workflow.get._id != p_doc._id) {
              col.update(BSONDocument("_id" -> default_workflow.get._id), default_workflow.get.copy(d=false, sys = SystemDataStore.modifyWithSystem(this.updateSystem(default_workflow.get), p_request)))
            }
          }
        } 
      }
    } else {
      val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      } 
    }
  }
  
  def getSelectedApplicable(p_request:RequestHeader) = {
    for {
      workflows <- this.find(BSONDocument("d" -> false), p_request)
    } yield {
      val appliablesList = workflows.map(workflow => {
        workflow.app
      })
      appliablesList.flatten
    }
  }
  
}