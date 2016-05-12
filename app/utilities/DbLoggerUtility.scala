package utilities

import play.api.Play
import scala.util.{Success, Failure}
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
    def write(p_doc: System): BSONDocument = {
      BSONDocument(
          "_eid" -> p_doc.eid,
          "_cdat" -> p_doc.cdat.map(date => BSONDateTime(date.getMillis)),
          "_mdat" -> p_doc.mdat.map(date => BSONDateTime(date.getMillis)),
          "_mby" -> p_doc.mby,
          "_ddat" -> p_doc.ddat.map(date => BSONDateTime(date.getMillis)),
          "_dby" -> p_doc.dby
      )     
    }
  }
    
  implicit object DbLoggerBSONWriter extends BSONDocumentWriter[DbLogger] {
    def write(p_doc: DbLogger): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "a" -> p_doc.a,
          "r" -> p_doc.r,
          "pe" -> p_doc.pe,
          "mh" -> p_doc.mh,
          "p" -> p_doc.p,
          "ra" -> p_doc.ra,
          "b" -> p_doc.b,
          "m" -> p_doc.m,
          "sys" -> p_doc.sys 
      )     
    }
  }
    
  private val appname = Play.current.configuration.getString("application_name").getOrElse("hrsifu")
  private val apprelease = Play.current.configuration.getString("application_release").getOrElse("")
  private val dbloggerApp = Play.current.configuration.getString("dblogger_application").getOrElse("OFF")
  private val dbloggerSec = Play.current.configuration.getString("dblogger_security").getOrElse("OFF")
  private val authenticationCol = DbConnUtility.logger_db.collection("authentication")
  private val applicationCol = DbConnUtility.logger_db.collection("application")
    
  def debug(p_msg:String, p_request:RequestHeader=null) = {
    if (dbloggerApp=="DEBUG") {
      val future = if (p_request != null && !(p_request.session.isEmpty)) {
	        applicationCol.insert(
	            DbLogger(
	                _id=BSONObjectID.generate,
	                a=Some(appname),
	                r=Some(apprelease),
	                pe=Some(p_request.session.get("email").get),
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
  
  def auth(p_eid:String, p_email:String, p_request:RequestHeader=null) = {
    if (dbloggerSec=="ON") {
      val future = authenticationCol.insert(
          DbLogger(
              _id=BSONObjectID.generate,
              a=Some(appname),
              r=Some(apprelease),
              pe=Some(p_email),
              mh=Some(p_request.method),
              p=Some(p_request.path),
              ra=Some(p_request.remoteAddress),
              b=Some(p_request.headers.get("user-agent").get),
              m=Some("Login"),
              sys = SystemDataStore.creation(p_eid=p_eid)
          ) 
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
}