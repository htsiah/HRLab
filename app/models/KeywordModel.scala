package models

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import scala.util.{Success, Failure,Try}
import scala.concurrent.Await
import org.joda.time.DateTime

import utilities.{System, SystemDataStore, Tools}

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
    def read(doc: BSONDocument): System = {
      System(
          doc.getAs[String]("eid").map(v => v),
          doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          doc.getAs[String]("mby").map(v => v),
          doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          doc.getAs[String]("dby").map(v => v),
          doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object KeywordBSONReader extends BSONDocumentReader[Keyword] {
    def read(doc: BSONDocument): Keyword = {
      Keyword(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("n").get,
          doc.getAs[List[String]]("v").map(v => v),
          doc.getAs[Boolean]("s").get,
          doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(system: System): BSONDocument = {
      BSONDocument(
          "eid" -> system.eid,
          "cdat" -> system.cdat.map(date => BSONDateTime(date.getMillis)),
          "mdat" -> system.mdat.map(date => BSONDateTime(date.getMillis)),
          "mby" -> system.mby,
          "ddat" -> system.ddat.map(date => BSONDateTime(date.getMillis)),
          "dby" -> system.dby,
          "ll" -> system.ll.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  implicit object KeywordBSONWriter extends BSONDocumentWriter[Keyword] {
    def write(keyword: Keyword): BSONDocument = {
      BSONDocument(
          "_id" -> keyword._id,
          "n" -> keyword.n,
          "v" -> keyword.v,
          "s" -> keyword.s,
          "sys" -> keyword.sys
      )     
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_admin").getOrElse("admin")
  private val uri = Play.current.configuration.getString("mongodb_admin_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("keyword")
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
        val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
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
    col.find(p_query).cursor[Keyword].collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Keyword].collect[List]()
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