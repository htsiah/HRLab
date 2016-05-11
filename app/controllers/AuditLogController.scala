package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import org.joda.time.format.DateTimeFormat

import reactivemongo.bson.{BSONObjectID,BSONDocument}

import scala.concurrent.Future

import models.AuditLogModel

class AuditLogController extends Controller with Secured {
  
  def getAuditlog(p_lk: String) = withAuth { username => implicit request => {
    for {
      auditlogs <- AuditLogModel.find(BSONDocument("lk" -> p_lk), BSONDocument("sys.cdat" -> -1), 10, request)
    } yield {
      render {
        case Accepts.Html() => {
          
          //Ok(views.html.error.unauthorized())
          val dtf = DateTimeFormat.forPattern("d-MMM-yyyy HH:mm:ss");
          val htmlheader = "<table class='table  table-bordered table-hover'><tr><th>Date</th><th>By</th><th>Message</th></tr>"
          val htmlbody = auditlogs.map( auditlog => {
            "<tr><td>" + auditlog.sys.get.cdat.get.dayOfMonth().getAsText + "-" +
            auditlog.sys.get.cdat.get.monthOfYear().getAsShortText + "-" +
            auditlog.sys.get.cdat.get.getYear().toString + " " +
            auditlog.sys.get.cdat.get +
            auditlog.sys.get.cdat.get.dayOfMonth().getAsText +
            "</td><td>" + auditlog.pn + "</td><td>" + auditlog.c + "</td></tr>"
          })
          val htmlcloser = "</table>"

          Ok(Json.parse("""{"auditlog":""""+ htmlheader + htmlbody.mkString("") + htmlcloser + """"}""")).as("application/json")
        }
        case Accepts.Json() => {
          val htmlheader = "<table><tr><th>Date</th><th>By</th><th>Message</th></tr>"
          val htmlbody = ""
          val htmlcloser = "</table>"
                 
          Ok(Json.parse("""{"auditlog":" + htmlheader + htmlbody + htmlcloser + "}""")).as("application/json")
        }
      }
    }
  }}

}