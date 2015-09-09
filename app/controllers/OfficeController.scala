package controllers

import scala.concurrent.{Future, Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{OfficeModel, PersonModel, Profile, Office}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

object OfficeController extends Controller with Secured {
  
  val officeform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "n" -> text,
          "ad1" -> optional(text),
          "ad2" -> optional(text),
          "ad3" -> optional(text),
          "pc" -> optional(text),
          "ct" -> text,
          "st" -> text,
          "df" -> boolean,
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id,n,ad1,ad2,ad3,pc,ct,st,df,sys)=>Office(_id,n,ad1,ad2,ad3,pc,ct,st,df,sys)}
      {office:Office=>Some(office._id, office.n, office.ad1, office.ad2, office.ad3, office.pc, office.ct, office.st, office.df, office.sys)}
   )   
    
  def create = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      val NewObjectID = BSONObjectID.generate
      val office_doc = Office(
          _id = NewObjectID,
          n = "",
          ad1 = None,
          ad2 = None,
          ad3 = None,
          pc = None,
          ct = "Malaysia",
          st = "",
          df = false,
          sys = None
      )
      Future.successful(Ok(views.html.office.form(officeform.fill(office_doc))))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      officeform.bindFromRequest.fold(
          formWithError => {
            Future.successful(Ok(views.html.office.form(formWithError)))  
          },
          formWithData => {
            OfficeModel.insert(formWithData.copy(_id=BSONObjectID.generate), p_request=request)
            Future.successful(Redirect(routes.CompanyController.index))
          }
      )  
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybedoc <- OfficeModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
      } yield {
        maybedoc.map( doc  => {
          Ok(views.html.office.form(officeform.fill(doc), p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      officeform.bindFromRequest.fold(
          formWithError => {
            Future.successful(Ok(views.html.office.form(formWithError,p_id)))
          },
          formWithData => {
            for {
              maybe_office <- OfficeModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
            } yield {
              // Update person's office name if any change 
              val office = maybe_office.getOrElse(OfficeModel.doc)
              if (office.n != "" && !(office.n.equals(formWithData.n))) {
                val maybe_persons = PersonModel.find(BSONDocument("p.off" -> office.n), request)
                maybe_persons.map( persons => 
                  persons.map( person => {
                    PersonModel.update(
                        BSONDocument("_id"->person._id), 
                        person.copy(p=person.p.copy(off=formWithData.n)), 
                        request
                    )
                  })
                )
              }
              
              // Update Office
              OfficeModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
              Redirect(routes.CompanyController.index)
            }
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def delete(p_officename:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(OfficeModel.remove(BSONDocument("n" -> p_officename), request), Tools.db_timeout)
      Future.successful(Redirect(routes.CompanyController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for { 
      maybedoc <- OfficeModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybedoc.map( doc  => {
        Ok(views.html.office.view(doc))
      }).getOrElse(NotFound)
    }
  }}
  
  def isUsedJSON(p_officename:String) = withAuth { username => implicit request => {
    val f_office = PersonModel.findOne(BSONDocument("p.off" -> p_officename), request)
    f_office.map { office => 
      office.isDefined match {
        case true => Ok(Json.parse("""{"status":true}""")).as("application/json")
        case _ =>Ok(Json.parse("""{"status":false}""")).as("application/json")
      }
    }
  }}
    
  def checkoffice(p_id:String, p_officename:String) = withAuth { username => implicit request => {
    println(p_id);
    println(p_officename);
    val f_office = if (p_id == "") {
      OfficeModel.findOne(BSONDocument("n" -> p_officename), request)
    } else {
      OfficeModel.findOne(BSONDocument("_id" -> BSONDocument("$ne" -> BSONObjectID(p_id)), "n" -> p_officename), request)
    }
    f_office.map( office =>
      office.isDefined match {
        case true => Ok("false").as("text/plain")
        case _ => Ok("true").as("text/plain")
      }
    )
  }}

}