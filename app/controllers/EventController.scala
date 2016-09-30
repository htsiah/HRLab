package controllers

import scala.concurrent.{Future}

import org.joda.time.DateTime
import java.time.LocalTime;

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

import models.{EventModel, Event, PersonModel, OfficeModel}
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
          "lrr" -> list(text),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id, n, fdat, tdat, aday, w, c, d, lrr, sys)=>Event(_id, n, fdat, tdat, aday, w, c, d, lrr, sys)}
      {event:Event=>Some(event._id, event.n, event.fdat, event.tdat, event.aday, event.w, event.c, event.d, event.lrr, event.sys)}
  ) 
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val restrictionSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
        val filledForm = eventform.fill(EventModel.doc.copy(
            fdat = Some(new DateTime().withTimeAtStartOfDay()),
            tdat = Some(new DateTime().withTimeAtStartOfDay())
        ))
        Ok(views.html.event.form(filledForm, restrictionSelection.sorted))
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      eventform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              val restrictionSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
              Ok(views.html.event.form(formWithError, restrictionSelection.sorted))
            }
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