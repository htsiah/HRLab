package controllers

import scala.concurrent.{Future, Await}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{EventModel, Event, PersonModel, OfficeModel, CompanyHolidayModel, AuditLogModel}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONDateTime}

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
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."), p_request=request)
            Future.successful(Redirect(routes.CalendarController.company))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_event <- EventModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val restrictionSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
        maybe_event.map( event  => {
          Ok(views.html.event.form(eventform.fill(event), restrictionSelection.sorted, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
  def update(p_id:String) = withAuth { username => implicit request => {
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
          EventModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
          AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)  
          Future.successful(Redirect(routes.CalendarController.company))
        }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def delete(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(EventModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(request.session.get("path").getOrElse(routes.DashboardController.index).toString))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  } }
  
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
  
  def myprofileedit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_event <- EventModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val restrictionSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
        maybe_event.map( event  => {
          Ok(views.html.event.myprofileform(eventform.fill(event), restrictionSelection.sorted, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileupdate(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      eventform.bindFromRequest.fold(
        formWithError => {
          for {
            persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
            offices <- OfficeModel.getAllOfficeName(request)
          } yield {
            val restrictionSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
            Ok(views.html.event.myprofileform(formWithError, restrictionSelection.sorted))
          } 
        },
        formWithData => {
          EventModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
          AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)  
          Future.successful(Redirect(routes.DashboardController.index))
        }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileview(p_id:String) = withAuth { username => implicit request => {
    for { 
       maybe_event <- {EventModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)}
     } yield {
       maybe_event.map( event  => {
         val selectedLRR = event.lrr.map { lrr => lrr.split("@|@").head }
         Ok(views.html.event.myprofileview(event, selectedLRR))
       }).getOrElse(NotFound)
     }
  }}
    
  def getEvent(p_withLink:String, p_page:String, p_sdat:String, p_edat:String) = withAuth { username => implicit request => {
    for {
      events <- {
       if (p_sdat!="" || p_edat!="") {
         EventModel.find(BSONDocument("fdat"->BSONDocument("$lte"->BSONDateTime(new DateTime(p_edat).getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(new DateTime(p_sdat).getMillis()))), request)
       } else {
         EventModel.find(BSONDocument(), request)
       } 
      }
    } yield {
      render {
        case Accepts.Html() => {Ok(views.html.error.unauthorized())}
        case Accepts.Json() => {
          val datFmt = ISODateTimeFormat.date()
          val dTFmt = ISODateTimeFormat.dateTimeNoMillis()
          val tFmt = ISODateTimeFormat.timeNoMillis()
          val eventJSONStr = events.zipWithIndex.map {  case (event, c) => {
            val start = if (event.aday) { datFmt.print(event.fdat.get) } else { dTFmt.print(event.fdat.get) }
            val end =if (event.aday) { 
              if (event.fdat.get == event.tdat.get) { "" } else { "\"end\":\"" + datFmt.print(event.tdat.get.plusDays(1)) + "\"," }
              } else { "\"end\":\"" + dTFmt.print(event.tdat.get) + "\"," }
            val url = if (p_withLink=="y") {
              if (p_page=="company") { "\"url\":\"/event/view/" + event._id.stringify + "\"," } else {"\"url\":\"/event/myprofile/view/" + event._id.stringify + "\","}
              } else { "" }
            "{\"id\":"+ c + ",\"title\":\"" + event.n + "\"," + url + "\"start\":\"" + start + "\"," + end + "\"color\":\""+ event.c + "\",\"tip\":\"" + event.n + "\"}"
          } }
          Ok(Json.parse("[" + eventJSONStr.mkString(",") + "]")).as("application/json") 
        }
       }
    }
  }}
  
}