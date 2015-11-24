package utilities

import scala.util.{Success, Failure,Try}
import play.api.Play
import play.api.Logger
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import reactivemongo.api._
import reactivemongo.bson._

case class DbLogger (
     _id: BSONObjectID,
     a: Option[String],
     r: Option[String],
     pe: Option[String],
     pn: Option[String],
     mh: Option[String],
     p: Option[String],
     ra: Option[String],
     b: Option[String],
     m: Option[String],
     sys: Option[System]
)

object DbLoggerUtility {
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(system: System): BSONDocument = {
      BSONDocument(
          "_eid" -> system.eid,
          "_cdat" -> system.cdat.map(date => BSONDateTime(date.getMillis)),
          "_mdat" -> system.mdat.map(date => BSONDateTime(date.getMillis)),
          "_mby" -> system.mby,
          "_ddat" -> system.ddat.map(date => BSONDateTime(date.getMillis)),
          "_dby" -> system.dby
      )     
    }
  }
    
  implicit object DbLoggerBSONWriter extends BSONDocumentWriter[DbLogger] {
    def write(dblogger: DbLogger): BSONDocument = {
      BSONDocument(
          "_id" -> dblogger._id,
          "a" -> dblogger.a,
          "r" -> dblogger.r,
          "pe" -> dblogger.pe,
          "pn" -> dblogger.pn,
          "mh" -> dblogger.mh,
          "p" -> dblogger.p,
          "ra" -> dblogger.ra,
          "b" -> dblogger.b,
          "m" -> dblogger.m,
          "sys" -> dblogger.sys 
      )     
    }
  }
    
  private val appname = Play.current.configuration.getString("application_name").getOrElse("hrsifu")
  private val apprelease = Play.current.configuration.getString("application_release").getOrElse("")
  private val dbloggerApp = Play.current.configuration.getString("dblogger_application").getOrElse("OFF")
  private val dbloggerSec = Play.current.configuration.getString("dblogger_security").getOrElse("OFF")
  private val dbname = Play.current.configuration.getString("mongodb_dblogger").getOrElse("dblogger")
  private val uri = Play.current.configuration.getString("mongodb_dblogger_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val authenticationCol = db.collection("authentication")
  private val applicationCol = db.collection("application")
  
  def init() = {
    Logger.info("Initialized Db Collection: " + authenticationCol.name)
    Logger.info("Initialized Db Collection: " + applicationCol.name)
  }
  
  def close() = {
    driver.close()
  }
  
  def debug(p_msg:String, p_request:RequestHeader=null) = {
    if (dbloggerApp=="DEBUG") {
      val future = if (p_request != null && !(p_request.session.isEmpty)) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=Some(p_request.session.get("email").get),
	                pn=Some(p_request.session.get("username").get),
	                mh=Some(p_request.method),
	                p=Some(p_request.path),
	                ra=Some(p_request.remoteAddress),
	                b=Some(p_request.headers.get("user-agent").get),
	                m=Some(p_msg),
	                sys=SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      } else if (p_request != null) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=None,
	                pn=None,
	                mh=Some(p_request.method),
	                p=Some(p_request.path),
	                ra=Some(p_request.remoteAddress),
	                b=Some(p_request.headers.get("user-agent").get),
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      } else {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=None,
	                pn=None,
	                mh=None,
	                p=None,
	                ra=None,
	                b=None,
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      }
      future.onComplete {
	  	case Failure(e) => throw e
	  	case Success(lastError) => {}
	  }
    }    
  }
  
  def info(p_msg:String, p_request:RequestHeader=null) = {
    if (dbloggerApp=="INFO" || dbloggerApp=="DEBUG") {
      val future = if (p_request != null && !(p_request.session.isEmpty)) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=Some(p_request.session.get("email").get),
	                pn=Some(p_request.session.get("username").get),
	                mh=Some(p_request.method),
	                p=Some(p_request.path),
	                ra=Some(p_request.remoteAddress),
	                b=Some(p_request.headers.get("user-agent").get),
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      } else if (p_request != null) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=None,
	                pn=None,
	                mh=Some(p_request.method),
	                p=Some(p_request.path),
	                ra=Some(p_request.remoteAddress),
	                b=Some(p_request.headers.get("user-agent").get),
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      } else {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=None,
	                pn=None,
	                mh=None,
	                p=None,
	                ra=None,
	                b=None,
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      }
      future.onComplete {
	  	case Failure(e) => throw e
	  	case Success(lastError) => {}
	  }
    }    
  }
    
  def error(p_msg:String, p_request:RequestHeader=null) = {
    if (dbloggerApp=="ERROR" || dbloggerApp=="INFO" || dbloggerApp=="DEBUG") {
      val future = if (p_request != null && !(p_request.session.isEmpty)) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=Some(p_request.session.get("email").get),
	                pn=Some(p_request.session.get("username").get),
	                mh=Some(p_request.method),
	                p=Some(p_request.path),
	                ra=Some(p_request.remoteAddress),
	                b=Some(p_request.headers.get("user-agent").get),
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      } else if (p_request != null) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=None,
	                pn=None,
	                mh=Some(p_request.method),
	                p=Some(p_request.path),
	                ra=Some(p_request.remoteAddress),
	                b=Some(p_request.headers.get("user-agent").get),
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      } else {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=None,
	                pn=None,
	                mh=None,
	                p=None,
	                ra=None,
	                b=None,
	                m=Some(p_msg),
	                sys = SystemDataStore.creation(p_request=p_request)
	            )
	        )
	      }
      future.onComplete {
	  	case Failure(e) => throw e
	  	case Success(lastError) => {}
	  }
    }    
  }
  
  def auth(p_login:Boolean,p_request:RequestHeader=null) = {
     if (dbloggerSec=="ON") {
       val msg = if (p_login == true) "Login" else "Logout"
       val future = if (p_request != null) {
	         authenticationCol.insert(
	             DbLogger(
	                 _id=BSONObjectID.generate,
	                 a=Some(appname),
	                 r=Some(apprelease),
	                 pe=Some(p_request.session.get("email").get),
	                 pn=Some(p_request.session.get("username").get),
	                 mh=Some(p_request.method),
	                 p=Some(p_request.path),
	                 ra=Some(p_request.remoteAddress),
	                 b=Some(p_request.headers.get("user-agent").get),
	                 m=Some(msg),
	                 sys = SystemDataStore.creation(p_request=p_request)
	             )
	         )
	       } else {
	         authenticationCol.insert(
	             DbLogger(
	                 _id=BSONObjectID.generate,
	                 a=Some(appname),
	                 r=Some(apprelease),
	                 pe=None,
	                 pn=None,
	                 mh=None,
	                 p=None,
	                 ra=None,
	                 b=None,
	                 m=Some(msg),
	                 sys = SystemDataStore.creation(p_request=p_request)
	             )
	         )
	       }
       future.onComplete {
         case Failure(e) => throw e
         case Success(lastError) => {}
       }
     }
  }
  
}