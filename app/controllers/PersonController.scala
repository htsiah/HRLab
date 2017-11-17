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

import models.{PersonModel, AuthenticationModel, KeywordModel, ClaimWorkflowModel, OfficeModel, Authentication, Person, Profile, Workday, LeaveProfileModel, LeaveModel, LeavePolicyModel, AuditLogModel, OrgChartSettingModel}
import utilities.{System, MailUtility, Tools, AlertUtility}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument,BSONArray}

import javax.inject.Inject

class PersonController @Inject() (mailerClient: MailerClient) extends Controller with Secured{
  
  val personform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "p" -> mapping(
              "empid" -> text,
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
          ){(empid,fn,ln,em,nem,pt,mgrid,smgrid,g,ms,dpm,off,edat,rl)=>Profile(empid,fn,ln,em.toLowerCase().trim(),nem,pt,mgrid,smgrid,g,ms,dpm,off,edat,rl.split(",").toList)}
          {profile:Profile => Some(profile.empid,profile.fn,profile.ln,profile.em,profile.nem,profile.pt,profile.mgrid,profile.smgrid,profile.g,profile.ms,profile.dpm,profile.off,profile.edat,profile.rl.mkString(","))},
          "wd" -> mapping(
              "wd1" -> text,
              "wd2" -> text,
              "wd3" -> text,
              "wd4" -> text,
              "wd5" -> text,
              "wd6" -> text,
              "wd7" -> text
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
        Ok(views.html.person.form(personform.fill(PersonModel.doc), persons, department.v, offices, position.v))
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
              Ok(views.html.person.form(formWithError, persons, department.v, offices, position.v))
            }
          },
          formWithData => {
            
            val person_objectID = BSONObjectID.generate
            PersonModel.insert(formWithData.copy(_id=person_objectID), p_request=request)
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=person_objectID.stringify, c="Create document."), p_request=request)
            
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
              Future.successful(Redirect(routes.PersonController.index).flashing(
                  "success" -> { formWithData.p.fn + " " + formWithData.p.ln + " has been created. A welcome email with logon detail has been sent to " + formWithData.p.em + "." }
              ))

            } else {
              
              Future.successful(Redirect(routes.PersonController.index).flashing(
                  "success" -> { formWithData.p.fn + " " + formWithData.p.ln + " has been created." }
              ))
              
            }

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
          Ok(views.html.person.form(personform.fill(person), persons, department.v, offices, position.v, isLastAdmin, p_id))
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
              Ok(views.html.person.form(formWithError, persons, department.v, offices, position.v, isLastAdmin, p_id))
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
              maybeauth <- AuthenticationModel.findOneByEmail(formWithData.p.em.toLowerCase())
            } yield {
              
              // Make sure after person update, all leave profile able link to leave policy.
              val LeaveTypesList = leaveprofiles.map { leaveprofile => {
                val isAvailable = Await.result(LeavePolicyModel.isAvailable(leaveprofile.lt, formWithData.p.g + " only", formWithData.p.ms + " only", request), Tools.db_timeout)
                if ( isAvailable == false) { leaveprofile.lt } else { "" }
              } }.filter( value => value != "" )
              
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
                    ("roles"->formWithData.p.rl.mkString(",")) + 
                    ("ismanager"->isManager) +
                    ("office"->formWithData.p.off) +
                    ("managerid"->formWithData.p.mgrid) +
                    ("smanagerid"->formWithData.p.smgrid)
                  ).flashing(
                      "success" -> { formWithData.p.fn + " " + formWithData.p.ln + " has been updated." }
                  )
                } else {
                                    
                  if (formWithData.p.em == ""){
                    Redirect(routes.PersonController.index).flashing(
                        "success" -> { formWithData.p.fn + " " + formWithData.p.ln + " has been updated." }
                    )
                  } else {
                    // Create authentication document if not available      
                    val maybeauth = Await.result(AuthenticationModel.findOneByEmail(formWithData.p.em.toLowerCase()), Tools.db_timeout)
                    if (maybeauth.isDefined) {
                      Redirect(routes.PersonController.index).flashing(
                          "success" -> { formWithData.p.fn + " " + formWithData.p.ln + " has been updated." }
                      )
                    } else {
                      val authentication_doc = Authentication(
                          _id = BSONObjectID.generate,
                          em = formWithData.p.em,
                          p = Random.alphanumeric.take(8).mkString,
                          r = Random.alphanumeric.take(8).mkString,
                          sys = None
                      )
                      AuthenticationModel.insert(authentication_doc, p_request=request)
                      Redirect(routes.PersonController.index).flashing(
                          "success" -> { formWithData.p.fn + " " + formWithData.p.ln + " has been updated. Remember to send welcome email with logon detail to " + formWithData.p.em + "." }
                      )
                    }
                  }
                  
                }
                
              } else {
                val replaceMap = Map(
                  "NAME"-> (formWithData.p.fn + " " + formWithData.p.ln),
                  "LEAVEPROFILE" -> LeaveTypesList.mkString(", ")
                )
                val alert = if ((maybealert_missingleavepolicy.getOrElse(null))!=null) { maybealert_missingleavepolicy.get.copy(m=Tools.replaceSubString(maybealert_missingleavepolicy.get.m, replaceMap.toList)) } else { null }
                val department = maybe_departments.getOrElse(KeywordModel.doc)
                val position = maybe_positions.getOrElse(KeywordModel.doc)
                Ok(views.html.person.form(personform.fill(formWithData), persons, department.v, offices, position.v, isLastAdmin, p_id, alert))
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
      OrgChartSettingModel.removeTLM(p_id, request)
      
      if (p_email!="") {
        AuthenticationModel.remove(BSONDocument("em" -> p_email), request)
        
        // Remove substitute manager
        PersonModel.updateUsingBSON(
            BSONDocument("p.smgrid" -> p_id, "sys.eid" -> request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)),
            BSONDocument("$set"->BSONDocument("p.smgrid"->""))
        )
      }
      
      Future.successful(Redirect(routes.PersonController.index).flashing(
          "success" -> {"Employee has been deleted." }
      ))
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
      
  def isDeleteable(p_id:String) = withAuth { username => implicit request => {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybe_staff <- PersonModel.findOne(BSONDocument("p.mgrid" -> p_id), request)
      maybe_workflow <- ClaimWorkflowModel.findOne(
          BSONDocument(
              "$or" -> BSONArray(
                  BSONDocument("s.s1"->BSONDocument("$ne"->""), "at.at1"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s2"->BSONDocument("$ne"->""), "at.at2"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s3"->BSONDocument("$ne"->""), "at.at3"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s4"->BSONDocument("$ne"->""), "at.at4"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s5"->BSONDocument("$ne"->""), "at.at5"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s6"->BSONDocument("$ne"->""), "at.at6"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s7"->BSONDocument("$ne"->""), "at.at7"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s8"->BSONDocument("$ne"->""), "at.at8"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s9"->BSONDocument("$ne"->""), "at.at9"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify)),
                  BSONDocument("s.s10"->BSONDocument("$ne"->""), "at.at10"->(maybe_person.get.p.fn + " " + maybe_person.get.p.ln + "@|@" + maybe_person.get._id.stringify))
              )
          ), 
          request)
    } yield {
      if (maybe_person.get.p.rl.contains("Admin") || maybe_staff.isDefined || maybe_workflow.isDefined) {
        Ok("false").as("text/plain")
      } else {  
        Ok("true").as("text/plain")
      }
    }
  }}
  
  def isEmpIdUnique(p_id:String, p_empid:String) = withAuth { username => implicit request => {
    if (p_id == "") {
      PersonModel.findOne(BSONDocument("p.empid" -> p_empid), request).map { person => {
        person.isDefined match {
          case true => Ok("false").as("text/plain")
          case _ =>Ok("true").as("text/plain")
        }
      } }
    } else {
      PersonModel.findOne(BSONDocument("_id"->BSONDocument("$ne"->BSONObjectID(p_id)), "p.empid" -> p_empid), request).map { person => {
        person.isDefined match {
          case true => Ok("false").as("text/plain")
          case _ =>Ok("true").as("text/plain")
        }
      } }
    }
  } }
  
  def sendWelcomeEmail(p_email:String) = withAuth { username => implicit request => {

    if(request.session.get("roles").get.contains("Admin")){
      PersonModel.findOne(BSONDocument("p.em" -> p_email), request).map { person => {
        val maybe_person = Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_email), request), Tools.db_timeout)

        // Reset authentication 
        val resetkey = Random.alphanumeric.take(8).mkString
        val modifier = BSONDocument("$set"->BSONDocument("r"->resetkey))
        AuthenticationModel.updateUsingBSON(BSONDocument("em"->p_email, "sys.ddat"->BSONDocument("$exists"->false)), modifier)
                
        // Send email
        val replaceMap = Map(
            "FULLNAME" -> {maybe_person.get.p.fn + " " + maybe_person.get.p.ln},
            "ADMIN" -> request.session.get("name").get,
            "COMPANY" -> request.session.get("company").get,
            "URL" -> {Tools.hostname + "/set/" + p_email  + "/" + resetkey}
        )
        MailUtility.getEmailConfig(List(p_email), 7, replaceMap).map { email => mailerClient.send(email) }
      
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=maybe_person.get._id.stringify, c="Send welcome email."), p_request=request)
        Ok("true").as("text/plain")
      } }  
    } else {
      Future.successful(Ok("false").as("text/plain"))
    }

  } }
  
}