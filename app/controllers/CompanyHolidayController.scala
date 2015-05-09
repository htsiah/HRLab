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

import models.{CompanyHolidayModel, CompanyHoliday}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

object CompanyHolidayController extends Controller with Secured {
  
  val companyholidayform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "n" -> nonEmptyText,
          "d" -> text,
          "ct" -> nonEmptyText,
          "st" -> text,
          "fdat" -> optional(jodaDate),
          "tdat" -> optional(jodaDate),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id, n, d, ct, st, fdat, tdat, sys)=>CompanyHoliday(_id, n, d, ct, st.split(",").toList, fdat, tdat, sys)}
      {companyholiday:CompanyHoliday=>Some(companyholiday._id, companyholiday.n, companyholiday.d, companyholiday.ct, companyholiday.st.mkString(","), companyholiday.fdat, companyholiday.tdat, companyholiday.sys)}
  ) 
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Future.successful(Ok(views.html.companyholiday.form(companyholidayform.fill(CompanyHolidayModel.doc), CompanyHolidayModel.doc.st)))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      companyholidayform.bindFromRequest.fold(
          formWithError => {
            formWithError.forField("st")(states => {
              Future.successful(Ok(views.html.companyholiday.form(formWithError, states.value.get.split(",").toList))) 
            }) 
          },
          formWithData => {
            CompanyHolidayModel.insert(formWithData.copy(_id=BSONObjectID.generate), p_request=request)
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
      } yield {
        maybe_companyholiday.map( companyholiday  => {
          Ok(views.html.companyholiday.form(companyholidayform.fill(companyholiday), companyholiday.st, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      companyholidayform.bindFromRequest.fold(
        formWithError => {
          formWithError.forField("st")(states => {
            Future.successful(Ok(views.html.companyholiday.form(formWithError, states.value.get.split(",").toList))) 
          }) 
        },
        formWithData => {
          CompanyHolidayModel.update(BSONDocument("_id" -> BSONObjectID(id)), formWithData.copy(_id=BSONObjectID(id)), request)
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
      Future.successful(Redirect(routes.CalendarController.company))
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
      } yield {
        maybe_companyholiday.map( companyholiday  => {
          Ok(views.html.companyholiday.myprofileform(companyholidayform.fill(companyholiday), companyholiday.st, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileupdate(id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      companyholidayform.bindFromRequest.fold(
          formWithError => {
            formWithError.forField("st")(states => {
              Future.successful(Ok(views.html.companyholiday.form(formWithError, states.value.get.split(",").toList))) 
            }) 
          },
          formWithData => {
            CompanyHolidayModel.update(BSONDocument("_id" -> BSONObjectID(id)), formWithData.copy(_id=BSONObjectID(id)), request)
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
   
  def getCompanyHolidayJSON = withAuth { username => implicit request => {
    for {
      companyholidays <- CompanyHolidayModel.find(BSONDocument(), request)
    } yield {
      var companyholidayJSONStr = ""
      var count = 0
      val fmt = ISODateTimeFormat.dateTime()
      companyholidays.foreach(companyholiday => {
        var title = companyholiday.n 
        var url = "/companyholiday/view/" + companyholiday._id.stringify
        var start = fmt.print(companyholiday.fdat.get) //choliday.sd.getYear() + "-" + choliday.sd.getMonthOfYear() + "-" + choliday.sd.getDayOfMonth()
        var end = fmt.print(companyholiday.tdat.get) //choliday.ed.getYear() + "-" + choliday.ed.getMonthOfYear() + "-" + choliday.ed.getDayOfMonth()
        if (count > 0) companyholidayJSONStr = companyholidayJSONStr + ","
        companyholidayJSONStr = companyholidayJSONStr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"
        count = count + 1
      })
      Ok(Json.parse("[" + companyholidayJSONStr + "]")).as("application/json")
    }
  }}

  def getCompanyHolidayMyProfileJSON = withAuth { username => implicit request => {
    for {
      companyholidays <- CompanyHolidayModel.find(BSONDocument(), request)
    } yield {
      var companyholidayJSONStr = ""
      var count = 0
      val fmt = ISODateTimeFormat.dateTime()
      companyholidays.foreach(companyholiday => {
        var title = companyholiday.n 
        var url = "/companyholiday/myprofile/view/" + companyholiday._id.stringify
        var start = fmt.print(companyholiday.fdat.get) //choliday.sd.getYear() + "-" + choliday.sd.getMonthOfYear() + "-" + choliday.sd.getDayOfMonth()
        var end = fmt.print(companyholiday.tdat.get) //choliday.ed.getYear() + "-" + choliday.ed.getMonthOfYear() + "-" + choliday.ed.getDayOfMonth()
        if (count > 0) companyholidayJSONStr = companyholidayJSONStr + ","
        companyholidayJSONStr = companyholidayJSONStr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"
        count = count + 1
      })
      Ok(Json.parse("[" + companyholidayJSONStr + "]")).as("application/json")
    }
  }}
  
}