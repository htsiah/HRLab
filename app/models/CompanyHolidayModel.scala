package models

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore}

import scala.util.{Success, Failure,Try}
import org.joda.time.DateTime

case class CompanyHoliday (
     _id: BSONObjectID,
     n: String,
     d: String,
     ct: String,
     st: List[String],
     fdat: Option[DateTime],
     tdat: Option[DateTime],
     sys: Option[System]
)

object CompanyHolidayModel {

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
  
  implicit object CompanyHolidayBSONReader extends BSONDocumentReader[CompanyHoliday] {
    def read(doc: BSONDocument): CompanyHoliday = {
      CompanyHoliday(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("n").get,
          doc.getAs[String]("d").get,
          doc.getAs[String]("ct").get,
          doc.getAs[List[String]]("st").get,
          doc.getAs[BSONDateTime]("fdat").map(dt => new DateTime(dt.value )),
          doc.getAs[BSONDateTime]("tdat").map(dt => new DateTime(dt.value )),
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
  
  implicit object CompanyHolidayBSONWriter extends BSONDocumentWriter[CompanyHoliday] {
    def write(companyholiday: CompanyHoliday): BSONDocument = {
      BSONDocument(
          "_id" -> companyholiday._id,
          "n" -> companyholiday.n,
          "d" -> companyholiday.d,
          "ct" -> companyholiday.ct,
          "st" -> companyholiday.st,
          "fdat" -> companyholiday.fdat.map(date => BSONDateTime(date.getMillis)),
          "tdat" -> companyholiday.tdat.map(date => BSONDateTime(date.getMillis)),
          "sys" -> companyholiday.sys
      )     
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_calendar").getOrElse("calendar")
  private val uri = Play.current.configuration.getString("mongodb_calendar_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("companyholiday")
  val doc = CompanyHoliday(
      _id = BSONObjectID.generate,
      n = "",
      d = "",
      ct = "Malaysia",
      st = List("Johor","Kedah", "Kelantan", "Kuala Lumpur", "Labuan", "Melaka", "Negeri Sembilan", "Pahang", "Penang", "Perak", "Perlis", "Putrajaya", "Sabah", "Sarawak", "Selangor", "Terengganu"),
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
	
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[CompanyHoliday].collect[List]()
  }
  
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[CompanyHoliday].collect[List]()
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