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
          Ok(views.html.error.unauthorized())
        }
        case Accepts.Json() => {
          if (auditlogs.length == 0) {
            Ok(Json.parse("""{"auditlog":"No audit log."}""")).as("application/json")
          } else {
            val dtf = DateTimeFormat.forPattern("d-MMM-yyyy HH:mm:ss");
            val htmlheader = "<table class='table  table-bordered table-hover'><tr><th>Date</th><th>By</th><th>Message</th></tr>"
            val htmlbody = auditlogs.map( auditlog => {
              "<tr><td>" + auditlog.sys.get.cdat.get.toDateTime().toString(dtf) + "</td><td>" + auditlog.pn + "</td><td>" + auditlog.c + "</td></tr>"
            })
            val htmlcloser = "</table>"
            Ok(Json.parse("""{"auditlog":""""+ htmlheader + htmlbody.mkString("") + htmlcloser + """"}""")).as("application/json")
          }
        }
      }
    }
  }}

}