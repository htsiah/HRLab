package controllers

import scala.concurrent.{Future}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import java.time.LocalTime;

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{EventModel, Event, PersonModel, OfficeModel, CompanyHolidayModel}
import utilities.{System, Tools}

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
  
  def edit(p_id:String) = TODO
    
  def update(p_id:String) = TODO
  
  def delete(p_id:String) = TODO
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for { 
       maybe_event <- EventModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
     } yield {
       maybe_event.map( event  => {
         val selectedLRR = event.lrr.map { lrr => lrr.split("@|@").head }
         Ok(views.html.event.view(event, selectedLRR))
       }).getOrElse(NotFound)
     }
  }}
  
  def myprofileedit(p_id:String) = TODO
  
  def myprofileupdate(p_id:String) = TODO
  
  def myprofileview(p_id:String) = withAuth { username => implicit request => {
    for { 
       maybe_event <- EventModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
     } yield {
       maybe_event.map( event  => {
         val selectedLRR = event.lrr.map { lrr => lrr.split("@|@").head }
         Ok(views.html.event.myprofileview(event, selectedLRR))
       }).getOrElse(NotFound)
     }
  }}
  
  def getEvent(p_withLink:String, p_page:String) = withAuth { username => implicit request => {
    for {
      events <- EventModel.find(BSONDocument(), request)
    } yield {
      render {
        case Accepts.Html() => {Ok(views.html.error.unauthorized())}
        case Accepts.Json() => {
          val datFmt = ISODateTimeFormat.date()
          val dTFmt = ISODateTimeFormat.dateTimeNoMillis()
          val tFmt = ISODateTimeFormat.timeNoMillis()
          val eventJSONStr = events.zipWithIndex.map {  case (event, c) => {
            event.lrr.sorted.foreach { name => name.split("@|@").head + ", " }
            val start = if (event.aday) { datFmt.print(event.fdat.get) } else { dTFmt.print(event.fdat.get) }
            val end =if (event.aday) { datFmt.print(event.tdat.get.plusDays(1)) } else { dTFmt.print(event.tdat.get) }
            val url = if (p_withLink=="y") {
              if (p_page=="company") { "/event/view/" + event._id.stringify } else {"/event/myprofile/view/" + event._id.stringify}
            } else { "" }
            "{\"id\":"+ c + ",\"title\":\"" + event.n + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"color\":\""+ event.c + "\",\"tip\":\"" + event.n + "\"}"
          } }
          Ok(Json.parse("[" + eventJSONStr.mkString(",") + "]")).as("application/json")
        }
       }
    }
  }}
  
}