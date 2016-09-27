package controllers

import scala.concurrent.{Future}

import org.joda.time.DateTime
import java.time.LocalTime;

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models.{EventModel, Event}
import utilities.{System}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

class EventController extends Controller with Secured {
  
  val eventform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "n" -> nonEmptyText,
          "fdat" -> optional(jodaDate("d-MMM-yyyy h:mm a")),
          "tdat" -> optional(jodaDate("d-MMM-yyyy h:mm a")),
          "aday" -> boolean,
          "w" -> text,
          "c" -> nonEmptyText,
          "d" -> text,
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id, n, fdat, tdat, aday, w, c, d, sys)=>Event(_id, n, fdat, tdat, aday, w, c, d, sys)}
      {event:Event=>Some(event._id, event.n, event.fdat, event.tdat, event.aday, event.w, event.c, event.d, event.sys)}
  ) 
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      val filledForm = eventform.fill(EventModel.doc.copy(
          fdat = Some(new DateTime().withTimeAtStartOfDay()),
          tdat = Some(new DateTime().withTimeAtStartOfDay())
      ))
      Future.successful(Ok(views.html.event.form(filledForm)))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      eventform.bindFromRequest.fold(
          formWithError => {
            Future.successful(Ok(views.html.event.form(formWithError)))
          },
          formWithData => {
            val doc_objectID = BSONObjectID.generate
            EventModel.insert(formWithData.copy(_id=doc_objectID), p_request=request)
            // AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."), p_request=request)
            Future.successful(Redirect(routes.CalendarController.company))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = TODO
  
}