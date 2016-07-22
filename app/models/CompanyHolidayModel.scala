package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class CompanyHoliday (
     _id: BSONObjectID,
     n: String,
     d: String,
     off: List[String],
     fdat: Option[DateTime],
     tdat: Option[DateTime],
     sys: Option[System]
)

object CompanyHolidayModel {

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
  
  implicit object CompanyHolidayBSONReader extends BSONDocumentReader[CompanyHoliday] {
    def read(p_doc: BSONDocument): CompanyHoliday = {
      CompanyHoliday(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("n").get,
          p_doc.getAs[String]("d").get,
          p_doc.getAs[List[String]]("off").get,
          p_doc.getAs[BSONDateTime]("fdat").map(dt => new DateTime(dt.value )),
          p_doc.getAs[BSONDateTime]("tdat").map(dt => new DateTime(dt.value )),
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
  
  implicit object CompanyHolidayBSONWriter extends BSONDocumentWriter[CompanyHoliday] {
    def write(p_doc: CompanyHoliday): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "n" -> p_doc.n,
          "d" -> p_doc.d,
          "off" -> p_doc.off,
          "fdat" -> p_doc.fdat.map(date => BSONDateTime(date.getMillis)),
          "tdat" -> p_doc.tdat.map(date => BSONDateTime(date.getMillis)),
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.calendar_db.collection("companyholiday")
  
  val doc = CompanyHoliday(
      _id = BSONObjectID.generate,
      n = "",
      d = "",
      off = List(),
      fdat = Some(new DateTime()),
      tdat = Some(new DateTime()),
      sys = None
  )
  
  private def updateSystem(p_doc:CompanyHoliday) = {
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
    
  def insert(p_doc:CompanyHoliday, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  def update(p_query:BSONDocument, p_doc:CompanyHoliday, p_request:RequestHeader) = {
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
	
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[CompanyHoliday](ReadPreference.primary).collect[List]()
  }
  
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[CompanyHoliday](ReadPreference.primary).collect[List]()
  }
  
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[CompanyHoliday]
  }
  
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[CompanyHoliday]
  }
  
  /** Custom Model Methods **/ 
  
  def isCompanyHoliday(p_Date:DateTime, p_country:String, p_state:String, p_request:RequestHeader) = {
    for {
      companyholiday <- this.findOne(
          BSONDocument(
              "ct"->p_country, 
              "st"->BSONDocument("$in"->List(p_state)), 
              "fdat"->BSONDocument("$lte" -> BSONDateTime(p_Date.getMillis())), 
              "tdat"->BSONDocument("$gte" -> BSONDateTime(p_Date.getMillis()))
              ), 
              p_request
      )
    } yield {
      if (companyholiday.isDefined) true else false
    }
  }
  
}