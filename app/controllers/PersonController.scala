package controllers

import scala.util.{Success, Failure,Try,Random}
import scala.concurrent.{Future, Await}
import org.joda.time.DateTime

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.cache.Cache

import play.api.libs.concurrent.Execution.Implicits._

import models.{PersonModel, AuthenticationModel, KeywordModel, OfficeModel, Authentication, Person, Profile, Workday, LeaveProfileModel, LeaveModel}
import utilities.{System, MailUtility, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

object PersonController extends Controller with Secured{
  
  val personform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "p" -> mapping(
              "fn" -> text,
              "ln" -> text,
              "em" -> text,
              "pt" -> text,
              "mgrid" -> text,
              "g" -> text,
              "ms" -> text,
              "dpm" -> text,
              "off" -> text,
              "edat" -> optional(jodaDate),
              "rl" -> text
          ){(fn,ln,em,pt,mgrid,g,ms,dpm,off,edat,rl)=>Profile(fn,ln,em,pt,mgrid,g,ms,dpm,off,edat,rl.split(",").toList)}
          {profile:Profile => Some(profile.fn,profile.ln,profile.em,profile.pt,profile.mgrid,profile.g,profile.ms,profile.dpm,profile.off,profile.edat,profile.rl.mkString(","))},
          "wd" -> mapping(
              "wd1" -> boolean,
              "wd2" -> boolean,
              "wd3" -> boolean,
              "wd4" -> boolean,
              "wd5" -> boolean,
              "wd6" -> boolean,
              "wd7" -> boolean
          )(Workday.apply)(Workday.unapply),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))
      ){(_id,p,wd,sys)=>
        Person(
            _id,
            p,
            wd,
            sys
        )
      }{person:Person=>
        Some(
            person._id,
            person.p,
            person.wd,
            person.sys
        )
      }
  )
  
  def index = withAuth { username => implicit request => {
    val f_docs = PersonModel.find(BSONDocument(),request)
    f_docs.map(docs => Ok(views.html.person.index(docs)).withSession(
        (request.session - "path") + ("path"->((routes.PersonController.index).toString))
    ))
  }}
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        persons <- PersonModel.find(BSONDocument(), request)
        maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val department = maybe_departments.getOrElse(KeywordModel.doc)
        Ok(views.html.person.form(personform.fill(PersonModel.doc), persons, department.v.get, offices))
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      personform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              val department = maybe_departments.getOrElse(KeywordModel.doc)
              Ok(views.html.person.form(formWithError, persons, department.v.get, offices))
            }
          },
          formWithData => {
            val rd = Random.alphanumeric.take(8).mkString
            val authentication_doc = Authentication(
                _id = BSONObjectID.generate,
                em = formWithData.p.em,
                p = rd,
                r = "",
                sys = None
            )
            PersonModel.insert(formWithData.copy(_id=BSONObjectID.generate), p_request=request)
            AuthenticationModel.insert(authentication_doc, p_request=request)
            
            // Send email
            val replaceMap = Map(
                "FULLNAME" -> {formWithData.p.fn + " " + formWithData.p.ln},
                "ADMIN" -> request.session.get("name").get,
                "COMPANY" -> request.session.get("company").get,
                "EMAIL" -> {formWithData.p.em},
                "PASSWORD" -> rd,
                "URL" -> Tools.hostname
            )
            MailUtility.sendEmailConfig(List(authentication_doc.em), 7, replaceMap)
            
            Future.successful(Redirect(routes.PersonController.index))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        persons <- PersonModel.find(BSONDocument(), request)
        maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
        offices <- OfficeModel.getAllOfficeName(request)
        isLastAdmin <- PersonModel.isLastAdmin(p_id, request)
      } yield {
        maybeperson.map( person => {
          val department = maybe_departments.getOrElse(KeywordModel.doc)
          Ok(views.html.person.form(personform.fill(person), persons, department.v.get, offices, isLastAdmin, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      personform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              offices <- OfficeModel.getAllOfficeName(request)
              isLastAdmin <- PersonModel.isLastAdmin(p_id, request)
            } yield {
              val department = maybe_departments.getOrElse(KeywordModel.doc)
              Ok(views.html.person.form(formWithError, persons, department.v.get, offices, isLastAdmin, p_id))
            }
          },
          formWithData => {
            Await.result(PersonModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request), Tools.db_timeout)
            if (request.session.get("id").get==p_id) {
              // Update session when update own record.
              val maybe_IsManager = Await.result(PersonModel.findOne(BSONDocument("p.mgrid" -> request.session.get("id").get), request), Tools.db_timeout)
              val isManager = if(maybe_IsManager.isEmpty) "false" else "true"
              Future.successful(Redirect(routes.PersonController.index).withSession(
                request.session + 
                ("name" -> (formWithData.p.fn + " " + formWithData.p.ln)) + 
                ("department" -> formWithData.p.dpm) + 
                ("roles"->formWithData.p.rl.mkString(",")) + 
                ("ismanager"->isManager)
              ))
            } else {
              Future.successful(Redirect(routes.PersonController.index))
            } 
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def delete(p_id:String, p_email:String) = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(PersonModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      AuthenticationModel.remove(BSONDocument("em" -> p_email), request)
      LeaveProfileModel.remove(BSONDocument("pid" -> p_id), request)
      LeaveModel.setLockDown(BSONDocument("pid" -> p_id), request)
      Future.successful(Redirect(routes.PersonController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def view(p_id:String) = withAuth { username => implicit request => {   
    for { 
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)  
      maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeperson.get.p.mgrid)), request)
      leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid" -> p_id), request)
    } yield {
      maybeperson.map( person => {
        Ok(views.html.person.view(person, maybemanager.get, leaveprofiles))
      }).getOrElse(NotFound)
    }
  }}
  
  def myprofileview = withAuth { username => implicit request => {   
    for { 
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)  
      maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeperson.get.p.mgrid)), request)
      leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid" -> request.session.get("id").get), request)
    } yield {
      maybeperson.map( person => {
        Ok(views.html.person.myprofileview(person, maybemanager.get, leaveprofiles)).withSession(
            (request.session - "path") + ("path"->((routes.PersonController.myprofileview).toString))
        )
      }).getOrElse(NotFound)
    }
  }}
  
  def myprofileedit = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
        persons <- PersonModel.find(BSONDocument(), request)
        maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
        offices <- OfficeModel.getAllOfficeName(request)
        isLastAdmin <- PersonModel.isLastAdmin(request.session.get("id").get, request)
      } yield {
        maybeperson.map( person => {
          val department = maybe_departments.getOrElse(KeywordModel.doc)
          Ok(views.html.person.myprofileform(personform.fill(person), persons, department.v.get, offices, isLastAdmin))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}

  def myprofileupdate = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      personform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              offices <- OfficeModel.getAllOfficeName(request)
              isLastAdmin <- PersonModel.isLastAdmin(request.session.get("id").get, request)
            } yield {
              val department = maybe_departments.getOrElse(KeywordModel.doc)
              Ok(views.html.person.myprofileform(formWithError, persons, department.v.get, offices, isLastAdmin))
            }
          },
          formWithData => {
            Await.result(PersonModel.update(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), formWithData.copy(_id=BSONObjectID(request.session.get("id").get)), request), Tools.db_timeout)
            val maybe_IsManager = Await.result(PersonModel.findOne(BSONDocument("p.mgrid" -> request.session.get("id").get), request), Tools.db_timeout)
            val isManager = if(maybe_IsManager.isEmpty) "false" else "true"
            Future.successful(Redirect(routes.PersonController.myprofileview).withSession(
                request.session + 
                ("name" -> (formWithData.p.fn + " " + formWithData.p.ln)) + 
                ("department" -> formWithData.p.dpm) + 
                ("roles"->formWithData.p.rl.mkString(",")) + 
                ("ismanager"->isManager)
            ))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def getEmploymentTypeJSON(p_id:String) = withAuth { username => implicit request => {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybe_staff <- PersonModel.findOne(BSONDocument("p.mgrid" -> p_id), request)
    } yield {
      if (maybe_person.get.p.rl.contains("Admin")) {
        Ok(Json.parse("""{"type":"admin"}""")).as("application/json")
      } else {  
        maybe_staff.isDefined match {
          case true => Ok(Json.parse("""{"type":"manager"}""")).as("application/json")
          case _ => Ok(Json.parse("""{"type":"staff"}""")).as("application/json")
        }
      }
    }
  }}
  
}