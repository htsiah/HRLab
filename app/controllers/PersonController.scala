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
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._

import models.{PersonModel, AuthenticationModel, KeywordModel, OfficeModel, Authentication, Person, Profile, Workday, LeaveProfileModel, LeaveModel, LeavePolicyModel, AuditLogModel}
import utilities.{System, MailUtility, Tools, AlertUtility}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

import javax.inject.Inject

class PersonController @Inject() (mailerClient: MailerClient) extends Controller with Secured{
  
  val personform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "p" -> mapping(
              "fn" -> text,
              "ln" -> text,
              "em" -> text,
              "nem" -> boolean,
              "pt" -> text,
              "mgrid" -> text,
              "smgrid" -> text,
              "g" -> text,
              "ms" -> text,
              "dpm" -> text,
              "off" -> text,
              "edat" -> optional(jodaDate("d-MMM-yyyy")),
              "rl" -> text
          ){(fn,ln,em,nem,pt,mgrid,smgrid,g,ms,dpm,off,edat,rl)=>Profile(fn,ln,em.toLowerCase().trim(),nem,pt,mgrid,smgrid,g,ms,dpm,off,edat,rl.split(",").toList)}
          {profile:Profile => Some(profile.fn,profile.ln,profile.em,profile.nem,profile.pt,profile.mgrid,profile.smgrid,profile.g,profile.ms,profile.dpm,profile.off,profile.edat,profile.rl.mkString(","))},
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
        persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
        maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val department = maybe_departments.getOrElse(KeywordModel.doc)
        val position = maybe_positions.getOrElse(KeywordModel.doc)
        Ok(views.html.person.form(personform.fill(PersonModel.doc), persons, department.v.get, offices, position.v.get))
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
              persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              val department = maybe_departments.getOrElse(KeywordModel.doc)
              val position = maybe_positions.getOrElse(KeywordModel.doc)
              Ok(views.html.person.form(formWithError, persons, department.v.get, offices, position.v.get))
            }
          },
          formWithData => {
            
            if (formWithData.p.nem==false) {
              
              val authentication_doc = Authentication(
                  _id = BSONObjectID.generate,
                  em = formWithData.p.em,
                  p = Random.alphanumeric.take(8).mkString,
                  r = Random.alphanumeric.take(8).mkString,
                  sys = None
              )
              AuthenticationModel.insert(authentication_doc, p_request=request)
              
              // Send email
              val replaceMap = Map(
                  "FULLNAME" -> {formWithData.p.fn + " " + formWithData.p.ln},
                  "ADMIN" -> request.session.get("name").get,
                  "COMPANY" -> request.session.get("company").get,
                  "URL" -> {Tools.hostname + "/set/" + authentication_doc.em  + "/" + authentication_doc.r}
              )
              MailUtility.getEmailConfig(List(authentication_doc.em), 7, replaceMap).map { email => mailerClient.send(email) }
            
            }
            val person_objectID = BSONObjectID.generate
            PersonModel.insert(formWithData.copy(_id=person_objectID), p_request=request)
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=person_objectID.stringify, c="Create document."), p_request=request)
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
        persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
        maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
        offices <- OfficeModel.getAllOfficeName(request)
        isLastAdmin <- PersonModel.isLastAdmin(p_id, request)
      } yield {
        maybeperson.map( person => {
          val department = maybe_departments.getOrElse(KeywordModel.doc)
          val position = maybe_positions.getOrElse(KeywordModel.doc)
          Ok(views.html.person.form(personform.fill(person), persons, department.v.get, offices, position.v.get, isLastAdmin, p_id))
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
              persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
              offices <- OfficeModel.getAllOfficeName(request)
              isLastAdmin <- PersonModel.isLastAdmin(p_id, request)
            } yield {
              val department = maybe_departments.getOrElse(KeywordModel.doc)
              val position = maybe_positions.getOrElse(KeywordModel.doc)
              Ok(views.html.person.form(formWithError, persons, department.v.get, offices, position.v.get, isLastAdmin, p_id))
            }
          },
          formWithData => {
            for {
              persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
              offices <- OfficeModel.getAllOfficeName(request)
              isLastAdmin <- PersonModel.isLastAdmin(p_id, request)
              leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid" -> p_id), request)
              maybealert_missingleavepolicy <- AlertUtility.findOne(BSONDocument("k"->1015))
            } yield {
              
              // Make sure after person update, all leave profile able link to leave policy.
              var LeaveTypesList = List[String]()
              leaveprofiles.map { leaveprofile => {
                val isAvailable = Await.result(LeavePolicyModel.isAvailable(leaveprofile.lt, formWithData.p.g + " only", formWithData.p.ms + " only", request), Tools.db_timeout)
                if ( isAvailable == false) {
                  LeaveTypesList = LeaveTypesList :+ leaveprofile.lt
                }
              } }
              
              if (LeaveTypesList.isEmpty) {
                Await.result(PersonModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request), Tools.db_timeout)
                AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)
                
                if (request.session.get("id").get==p_id) {
                  // Update session when update own record.
                  val maybe_IsManager = Await.result(PersonModel.findOne(BSONDocument("p.mgrid" -> request.session.get("id").get), request), Tools.db_timeout)
                  
                  val isManager = if(maybe_IsManager.isEmpty) "false" else "true"
                  Redirect(routes.PersonController.index).withSession(
                    request.session + 
                    ("name" -> (formWithData.p.fn + " " + formWithData.p.ln)) + 
                    ("department" -> formWithData.p.dpm) + 
                    ("position" -> formWithData.p.pt) + 
                    ("roles"->formWithData.p.rl.mkString(",")) + 
                    ("ismanager"->isManager)
                  )
                } else {
                  Redirect(routes.PersonController.index)
                } 
                
              } else {
                val replaceMap = Map(
                  "NAME"-> (formWithData.p.fn + " " + formWithData.p.ln),
                  "LEAVEPROFILE" -> LeaveTypesList.mkString(", ")
                )
                val alert = if ((maybealert_missingleavepolicy.getOrElse(null))!=null) { maybealert_missingleavepolicy.get.copy(m=Tools.replaceSubString(maybealert_missingleavepolicy.get.m, replaceMap.toList)) } else { null }
                val department = maybe_departments.getOrElse(KeywordModel.doc)
                val position = maybe_positions.getOrElse(KeywordModel.doc)
                Ok(views.html.person.form(personform.fill(formWithData), persons, department.v.get, offices, position.v.get, isLastAdmin, p_id, alert))
              }
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
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      LeaveProfileModel.removeWithAuditLog(BSONDocument("pid" -> p_id), request)
      LeaveModel.setLockDown(BSONDocument("pid" -> p_id), request)
      
      if (p_email!="") {
        AuthenticationModel.remove(BSONDocument("em" -> p_email), request)
        
        // Remove substitute manager
        PersonModel.updateUsingBSON(
            BSONDocument("p.smgrid" -> p_id, "sys.eid" -> request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)),
            BSONDocument("$set"->BSONDocument("p.smgrid"->""))
        )
      }
      
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
        val managername = {
          val manager = maybemanager.getOrElse(PersonModel.doc)
          manager.p.fn + " " + manager.p.ln
        }
        val smanagername = if (person.p.smgrid=="") { "" } else {
          val smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(person.p.smgrid)), request), Tools.db_timeout)
          smanager.get.p.fn + " " + smanager.get.p.ln
        }
        Ok(views.html.person.view(person, managername, smanagername, leaveprofiles.sortBy(_.lt)))
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
        val managername = {
          val manager = maybemanager.getOrElse(PersonModel.doc)
          manager.p.fn + " " + manager.p.ln
        }
        val smanagername = if (person.p.smgrid=="") { "" } else {
          val smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(person.p.smgrid)), request), Tools.db_timeout)
          smanager.get.p.fn + " " + smanager.get.p.ln
        }
        Ok(views.html.person.myprofileview(person, managername, smanagername, leaveprofiles.sortBy(_.lt))).withSession(
            (request.session - "path") + ("path"->((routes.PersonController.myprofileview).toString))
        )
      }).getOrElse(NotFound)
    }
  }}
  
  def myprofileedit = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
        persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
        maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
        offices <- OfficeModel.getAllOfficeName(request)
        isLastAdmin <- PersonModel.isLastAdmin(request.session.get("id").get, request)
      } yield {
        maybeperson.map( person => {
          val department = maybe_departments.getOrElse(KeywordModel.doc)
          val position = maybe_positions.getOrElse(KeywordModel.doc)
          Ok(views.html.person.myprofileform(personform.fill(person), persons, department.v.get, offices, position.v.get, isLastAdmin))
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
              persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
              offices <- OfficeModel.getAllOfficeName(request)
              isLastAdmin <- PersonModel.isLastAdmin(request.session.get("id").get, request)
            } yield {
              val department = maybe_departments.getOrElse(KeywordModel.doc)
              val position = maybe_positions.getOrElse(KeywordModel.doc)
              Ok(views.html.person.myprofileform(formWithError, persons, department.v.get, offices, position.v.get, isLastAdmin))
            }
          },
          formWithData => {
            for {
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
              persons <- PersonModel.find(BSONDocument("p.nem" -> false), request)
              maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
              maybe_positions <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
              offices <- OfficeModel.getAllOfficeName(request)
              isLastAdmin <- PersonModel.isLastAdmin(request.session.get("id").get, request)
              leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid" -> request.session.get("id").get), request)
              maybealert_missingleavepolicy <- AlertUtility.findOne(BSONDocument("k"->1016))
            } yield {
              
              // Make sure after person update, leave profiles able link to leave policy.
              var LeaveTypesList = List[String]()
              leaveprofiles.map { leaveprofile => {
                val isAvailable = Await.result(LeavePolicyModel.isAvailable(leaveprofile.lt, formWithData.p.g + " only", formWithData.p.ms + " only", request), Tools.db_timeout)
                if ( isAvailable == false) {
                  LeaveTypesList = LeaveTypesList :+ leaveprofile.lt
                }
              } }
              
              
              if (LeaveTypesList.isEmpty) {
                Await.result(PersonModel.update(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), formWithData.copy(_id=BSONObjectID(request.session.get("id").get)), request), Tools.db_timeout)
                AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=request.session.get("id").get, c="Modify document."), p_request=request)
                
                val maybe_IsManager = Await.result(PersonModel.findOne(BSONDocument("p.mgrid" -> request.session.get("id").get), request), Tools.db_timeout)
                val isManager = if(maybe_IsManager.isEmpty) "false" else "true"
                Redirect(routes.PersonController.myprofileview).withSession(
                    request.session + 
                    ("name" -> (formWithData.p.fn + " " + formWithData.p.ln)) + 
                    ("department" -> formWithData.p.dpm) + 
                    ("roles"->formWithData.p.rl.mkString(",")) + 
                    ("ismanager"->isManager)
                )
              } else {
                val replaceMap = Map(
                  "LEAVEPROFILE" -> LeaveTypesList.mkString(", ")
                )
                val alert = if ((maybealert_missingleavepolicy.getOrElse(null))!=null) { maybealert_missingleavepolicy.get.copy(m=Tools.replaceSubString(maybealert_missingleavepolicy.get.m, replaceMap.toList)) } else { null }
                val department = maybe_departments.getOrElse(KeywordModel.doc)
                val position = maybe_positions.getOrElse(KeywordModel.doc)
                Ok(views.html.person.myprofileform(personform.fill(formWithData), persons, department.v.get, offices, position.v.get, isLastAdmin, alert))
              }
            }
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