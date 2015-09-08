package models

import scala.util.{Success, Failure,Try}
import org.joda.time.DateTime

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore}

case class Authentication (
    _id: BSONObjectID,
    em: String,
    p: String,
    r: String,
    sys: Option[System]
)

object AuthenticationModel {
  
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
    
  implicit object AuthenticationBSONReader extends BSONDocumentReader[Authentication] {
    def read(doc: BSONDocument): Authentication = {
      Authentication(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("em").get,
          doc.getAs[String]("p").get,
          doc.getAs[String]("r").get,
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
    
  implicit object AuthenticationBSONWriter extends BSONDocumentWriter[Authentication] {
    def write(authentication: Authentication): BSONDocument = {
      BSONDocument(
          "_id" -> authentication._id,
          "em" -> authentication.em,
          "p" -> authentication.p,
          "r" -> authentication.r,
          "sys" -> authentication.sys 
      )     
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_directory").getOrElse("directory")
  private val uri = Play.current.configuration.getString("mongodb_directory_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("authentication")
  val doc = Authentication(
      _id = BSONObjectID.generate,
      em = "",
      p = "",
      r = "",
      sys = None
  )
  
  private def updateSystem(p_doc:Authentication) = {
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
  
  def init() = {
    println("Initialized Db Collection: " + col.name)
  }
  
  def close() = {
    driver.close()
  }
  
  def insert(p_doc:Authentication, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  def update(p_query:BSONDocument, p_doc:Authentication, p_request:RequestHeader) = {
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
  
  def removePermanently(p_query:BSONDocument) = {
    val future = col.remove(p_query)
  }
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[Authentication](ReadPreference.primary).collect[List]()
  }
    
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Authentication](ReadPreference.primary).collect[List]()
  }

  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Authentication]
  }
  
  
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Authentication]
  }
  
  /** Custom Model Methods **/ 
  
  def findOneByEmail(p_email:String) = {
    col.find(BSONDocument("em" -> p_email, "sys.ddat"->BSONDocument("$exists"->false))).one[Authentication]
  }
  
  def findReset(p_email:String, p_resetkey:String) = {
    col.find(BSONDocument("em"->p_email, "r"->p_resetkey, "sys.ddat"->BSONDocument("$exists"->false))).one[Authentication]
  }
  
  def logon(p_email:String) = {
    val future = this.findOneByEmail(p_email)
    future.map( doc => {
      val future = col.update(BSONDocument("em"->p_email, "sys.ddat"->BSONDocument("$exists"->false)), doc.get.copy(r="",sys=SystemDataStore.logonWithSystem(doc.get.sys.get)))
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    })
  }
  
  def resetPassword(p_doc:Authentication, p_newpassword:String) = {
    val future = col.update(BSONDocument("em"->p_doc.em, "r"->p_doc.r), p_doc.copy(p=p_newpassword, r="", sys=SystemDataStore.modifyWithSystem(this.updateSystem(p_doc))))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // Notes:
  // 1 p_modifier format: 
  //   1.1 Replace - BSONDocument
  //   1.2 Update certain field - BSONDocument("$set"->BSONDocument("[FIELD]"->VALUE))
  // 2 No SystemDataStore update
  def updateUsingBSON(p_query:BSONDocument,p_modifier:BSONDocument) = {
    val future = col.update(p_query, p_modifier)
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
}