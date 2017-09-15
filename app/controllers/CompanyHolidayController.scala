package controllers

import scala.concurrent.{Future, Await}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{CompanyHolidayModel, AuditLogModel, OfficeModel, CompanyHoliday}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONDateTime}

class CompanyHolidayController extends Controller with Secured {
  
  val companyholidayform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "n" -> nonEmptyText,
          "d" -> text,
          "off" -> text,
          "fdat" -> optional(jodaDate("d-MMM-yyyy")),
          "tdat" -> optional(jodaDate("d-MMM-yyyy")),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id, n, d, off, fdat, tdat, sys)=>CompanyHoliday(_id, n, d, off.split(",").toList, fdat, tdat, sys)}
      {companyholiday:CompanyHoliday=>Some(companyholiday._id, companyholiday.n, companyholiday.d, companyholiday.off.mkString(","), companyholiday.fdat, companyholiday.tdat, companyholiday.sys)}
  ) 
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val filledForm = companyholidayform.fill(CompanyHolidayModel.doc.copy(
            fdat = Some(new DateTime()),
            tdat = Some(new DateTime())
        ))
        Ok(views.html.companyholiday.form(filledForm, CompanyHolidayModel.doc.off, offices))
      }  
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      companyholidayform.bindFromRequest.fold(
          formWithError => {
            for {
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              formWithError.forField("off")(officesval => {
                Ok(views.html.companyholiday.form(formWithError, officesval.value.get.split(",").toList, offices))
              }) 
            }
          },
          formWithData => {
            val doc_objectID = BSONObjectID.generate
            CompanyHolidayModel.insert(formWithData.copy(_id=doc_objectID), p_request=request)
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
        maybe_companyholiday <- CompanyHolidayModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        maybe_companyholiday.map( companyholiday  => {
          Ok(views.html.companyholiday.form(companyholidayform.fill(companyholiday), companyholiday.off, offices, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      companyholidayform.bindFromRequest.fold(
        formWithError => {
          for {
            offices <- OfficeModel.getAllOfficeName(request)
          } yield {
            formWithError.forField("off")(officesval => {
              Ok(views.html.companyholiday.form(formWithError, officesval.value.get.split(",").toList, offices)) 
            }) 
          } 
        },
        formWithData => {
          CompanyHolidayModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
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
      Await.result(CompanyHolidayModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(request.session.get("path").getOrElse(routes.DashboardController.index).toString))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for { 
       maybe_companyholiday <- CompanyHolidayModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
     } yield {
       maybe_companyholiday.map( companyholiday  => {
         Ok(views.html.companyholiday.view(companyholiday))
       }).getOrElse(NotFound)
     }
  }}
  
  def myprofileedit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_companyholiday <- CompanyHolidayModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        maybe_companyholiday.map( companyholiday  => {
          Ok(views.html.companyholiday.myprofileform(companyholidayform.fill(companyholiday), companyholiday.off, offices, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileupdate(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      companyholidayform.bindFromRequest.fold(
          formWithError => {
            for {
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              formWithError.forField("off")(officesval => {
                Ok(views.html.companyholiday.myprofileform(formWithError, officesval.value.get.split(",").toList, offices))
              }) 
            }
          },
          formWithData => {
            CompanyHolidayModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
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
      maybe_companyholiday <- CompanyHolidayModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybe_companyholiday.map( companyholiday  => {
        Ok(views.html.companyholiday.myprofileview(companyholiday))
      }).getOrElse(NotFound)
    }
  }}
   
  def getCompanyHoliday(p_withLink:String, p_page:String, p_sdat:String, p_edat:String) = withAuth { username => implicit request => {
    for {
      companyholidays <- {
        if (p_sdat!="" || p_edat!="") {
          CompanyHolidayModel.find(BSONDocument("fdat"->BSONDocument("$lte"->BSONDateTime(new DateTime(p_edat).getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(new DateTime(p_sdat).getMillis()))), request)
        } else {
          CompanyHolidayModel.find(BSONDocument(), request)
        }
      }
    } yield {
      render {
        case Accepts.Html() => {Ok(views.html.error.unauthorized())}
        case Accepts.Json() => {
          val fmt = ISODateTimeFormat.date()
          val companyholidayJSONStr = companyholidays.zipWithIndex.map{ case (companyholiday, c) => {
            val title = companyholiday.n.replace("\t", "") // Temporary solution 
            val url = if (p_withLink=="y") { 
              if (p_page=="company") { "\"url\":\"/companyholiday/view/" + companyholiday._id.stringify + "\","  } else { "\"url\":\"/companyholiday/myprofile/view/" + companyholiday._id.stringify + "\"," }
            } else { "" }
            val end = if (companyholiday.fdat.get == companyholiday.tdat.get) { "" } else { "\"end\":\"" + fmt.print(companyholiday.tdat.get.plusDays(1)) + "\"," }
            "{\"id\":"+ c + ",\"title\":\"" + title + "\"," + url + "\"start\":\"" + fmt.print(companyholiday.fdat.get) + "\"," + end + "\"tip\":\"" + title + " (" + companyholiday.off.mkString(", ") + ")" + "\"}"
          }}
          Ok(Json.parse("[" + companyholidayJSONStr.mkString(",") + "]")).as("application/json")
        }
      }
    }
  }}
  
  def importholidays = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      println("Okay")
      Future.successful(Redirect(routes.CalendarController.company))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}