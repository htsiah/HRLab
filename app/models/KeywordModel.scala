package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import scala.util.{Success, Failure}
import scala.concurrent.Await
import org.joda.time.DateTime

import utilities.{System, SystemDataStore, Tools, DbConnUtility}

case class Keyword (
     _id: BSONObjectID,
     n: String,
     v: Option[List[String]],
     s: Boolean,
     sys: Option[System]
)

object KeywordModel {

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
  
  implicit object KeywordBSONReader extends BSONDocumentReader[Keyword] {
    def read(p_doc: BSONDocument): Keyword = {
      Keyword(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("n").get,
          p_doc.getAs[List[String]]("v").map(v => v),
          p_doc.getAs[Boolean]("s").get,
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
  
  implicit object KeywordBSONWriter extends BSONDocumentWriter[Keyword] {
    def write(p_doc: Keyword): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "n" -> p_doc.n,
          "v" -> p_doc.v,
          "s" -> p_doc.s,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.admin_db.collection("keyword")
  
  val doc = Keyword(
      _id = BSONObjectID.generate,
      n = "",
      v = Some(List("")),
      s = false,
      sys = None
  )
  
  private def updateSystem(p_doc:Keyword) = {
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
    
  // Insert new document
  def insert(p_doc:Keyword, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  	
  // Update document
  def update(p_query:BSONDocument, p_doc:Keyword, p_request:RequestHeader) = {
    val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }

  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.map { doc => 
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
    col.find(p_query).cursor[Keyword](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Keyword](ReadPreference.primary).collect[List]()
  }
	
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Keyword]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Keyword]
  }
  
  /** Custom Model Methods **/ 
  
  def getProtectedKey(p_doc:Keyword, p_request:RequestHeader) = {
    
    def findProtectDepartmentKey() : List[String] = {
      var protectedKeyList = List[String]()
      p_doc.v.get.foreach { value => {
        val person = Await.result(PersonModel.findOne(BSONDocument("p.dpm" -> value), p_request), Tools.db_timeout)
        if (person.isDefined) protectedKeyList = value :: protectedKeyList
      } }
      protectedKeyList
    }
    
    def findProtectLeaveTypeKey() : List[String] = {
      var protectedKeyList = List[String]()
      p_doc.v.get.foreach { value => {
        val leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt" -> value), p_request), Tools.db_timeout)
        if (leavepolicy.isDefined) protectedKeyList = value :: protectedKeyList
      } }
      protectedKeyList
    }
    
    def findProtectPositionTypeKey() : List[String] = {
      var protectedKeyList = List[String]()
      p_doc.v.get.foreach { value => {
        val person = Await.result(PersonModel.findOne(BSONDocument("p.pt" -> value), p_request), Tools.db_timeout)
        if (person.isDefined) protectedKeyList = value :: protectedKeyList
        val leavetype = Await.result(LeavePolicyModel.findOne(BSONDocument("lt" -> value), p_request), Tools.db_timeout)
        if (leavetype.isDefined) protectedKeyList = value :: protectedKeyList
      } }
      protectedKeyList
    }
    
    p_doc.n match {
      case "Department" => findProtectDepartmentKey()
      case "Position Type" => findProtectPositionTypeKey()
      case "Leave Type" => findProtectLeaveTypeKey()
      case _ => List("")
    }
    
  }
  
}