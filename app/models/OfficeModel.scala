package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore,DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class Office (
    _id: BSONObjectID,
    n: String,
    ad1: Option[String],
    ad2: Option[String],
    ad3: Option[String],
    pc: Option[String],
    ct: String,
    st: Option[String],
    df: Boolean,
    sys: Option[System]
)

object OfficeModel {

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
  
  implicit object OfficeBSONReader extends BSONDocumentReader[Office] {
    def read(p_doc: BSONDocument): Office = {
      Office(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("n").get,
          p_doc.getAs[String]("ad1").map(v => v),
          p_doc.getAs[String]("ad2").map(v => v),
          p_doc.getAs[String]("ad3").map(v => v),
          p_doc.getAs[String]("pc").map(v => v),
          p_doc.getAs[String]("ct").get,
          p_doc.getAs[String]("st").map(v => v),
          p_doc.getAs[Boolean]("df").get,
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
    
  implicit object OfficeBSONWriter extends BSONDocumentWriter[Office] {
    def write(p_doc: Office): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "n" -> p_doc.n,
          "ad1" -> p_doc.ad1,
          "ad2" -> p_doc.ad2,
          "ad3" -> p_doc.ad3,
          "pc" -> p_doc.pc,
          "ct" -> p_doc.ct,
          "st" -> p_doc.st,
          "df" -> p_doc.df,
          "sys" -> p_doc.sys
      )     
    }
  }

  private val col = DbConnUtility.dir_db.collection("office")
  
  val doc = Office(
      _id = BSONObjectID.generate,
      n = "",
      ad1 = None,
      ad2 = None,
      ad3 = None,
      pc = None,
      ct = "",
      st = None,
      df = false,
      sys = None
  )
  
  private def updateSystem(p_doc:Office) = {
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
  def insert(p_doc:Office, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
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
    col.find(p_query).cursor[Office](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Office](ReadPreference.primary).collect[List]()
  }
	
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Office]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Office]
  }
  
  /** Custom Model Methods **/ 
  // Update document
  def update(p_query:BSONDocument,p_doc:Office,p_request:RequestHeader) = {    
    for {
      oldoffice <- this.findOne(BSONDocument("_id"->p_doc._id), p_request)
    } yield {
      val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          // Update name on leave and leave profile
          if (oldoffice.get.n != p_doc.n) {
            PersonModel.updateUsingBSON(BSONDocument("p.off"->p_doc.n), BSONDocument("$set"->BSONDocument("p.off"->p_doc.n)))
          }
        }
      }
    }
    
  }
  
  def getAllOfficeName(p_request:RequestHeader) = {
    for {
      maybe_offices <- this.find(BSONDocument(), p_request)
    } yield {
      var officelist = List[String]()
      maybe_offices.foreach(office => officelist = officelist :+ office.n)
      officelist
    }
  }
  
}