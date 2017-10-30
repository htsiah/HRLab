package controllers

import scala.concurrent.{Future, Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimWorkflowModel, ClaimWorkflow, ClaimWorkflowStatus, ClaimWorkflowAssigned, ClaimWorkflowApprovedAmount, ClaimWorkflowAmountGreater, AuditLogModel, KeywordModel, PersonModel, OfficeModel}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

class ClaimWorkflowController extends Controller with Secured {

  val claimworkflowform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "n" -> text,
          "d" -> boolean,
          "app" -> list(text),
          "s" -> mapping(
              "s1" -> text,
              "s2" -> text,
              "s3" -> text,
              "s4" -> text,
              "s5" -> text,
              "s6" -> text,
              "s7" -> text,
              "s8" -> text,
              "s9" -> text,
              "s10" -> text
          )(ClaimWorkflowStatus.apply)(ClaimWorkflowStatus.unapply),
          "at" -> mapping(
              "at1" -> text,
              "at2" -> text,
              "at3" -> text,
              "at4" -> text,
              "at5" -> text,
              "at6" -> text,
              "at7" -> text,
              "at8" -> text,
              "at9" -> text,
              "at10" -> text
          )(ClaimWorkflowAssigned.apply)(ClaimWorkflowAssigned.unapply),
          "caa" -> mapping(
              "caa1" -> boolean,
              "caa2" -> boolean,
              "caa3" -> boolean,
              "caa4" -> boolean,
              "caa5" -> boolean,
              "caa6" -> boolean,
              "caa7" -> boolean,
              "caa8" -> boolean,
              "caa9" -> boolean,
              "caa10" -> boolean
          )(ClaimWorkflowApprovedAmount.apply)(ClaimWorkflowApprovedAmount.unapply),
          "cg" -> mapping(
              "cg1" -> number,
              "cg2" -> number,
              "cg3" -> number,
              "cg4" -> number,
              "cg5" -> number,
              "cg6" -> number,
              "cg7" -> number,
              "cg8" -> number,
              "cg9" -> number,
              "cg10" -> number
          )(ClaimWorkflowAmountGreater.apply)(ClaimWorkflowAmountGreater.unapply),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))
      ){(_id,n,d,app,s,at,caa,cg,sys)=>
        ClaimWorkflow(_id,n,d,app,s,at,caa,cg,sys)
      }{claimworkflow:ClaimWorkflow=>
        Some(claimworkflow._id,claimworkflow.n,claimworkflow.d,claimworkflow.app,claimworkflow.s,claimworkflow.at,claimworkflow.caa,claimworkflow.cg,claimworkflow.sys)
      }
  )
  
  def view(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybeclaimworkflow <- ClaimWorkflowModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
      } yield {
        maybeclaimworkflow.map( claimworkflow => {
          Ok(views.html.claimworkflow.view(claimworkflow))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        persons <- PersonModel.find(BSONDocument(), request)
        personsWithEmail <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        offices <- OfficeModel.getAllOfficeName(request)
        selectedApplicable <- ClaimWorkflowModel.getSelectedApplicable(request)
      } yield {
        val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
        val assignedToSelections = personsWithEmail.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify }
        Ok(views.html.claimworkflow.form(claimworkflowform.fill(ClaimWorkflowModel.doc), applicableSelection.filterNot(selectedApplicable.contains(_)).sorted, assignedToSelections.sorted))
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      claimworkflowform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {              
              val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
              val assignedToSelections = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify }
              Ok(views.html.claimworkflow.form(formWithError, applicableSelection.sorted, assignedToSelections.sorted))
            }
          },
          formWithData => {
            val doc_objectID = BSONObjectID.generate
            ClaimWorkflowModel.insert(formWithData.copy(_id=doc_objectID), p_request=request)
            
            // Create Audit Log 
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."),p_request=request)
              
            Future.successful(Redirect(routes.ClaimSettingController.index))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybedoc <- ClaimWorkflowModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
        persons <- PersonModel.find(BSONDocument(), request)
        personsWithEmail <- PersonModel.find(BSONDocument("p.nem" -> false), request)
        selectedApplicable <- ClaimWorkflowModel.getSelectedApplicable(request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        maybedoc.map( doc  => {
          val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
          val assignedToSelections = personsWithEmail.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify }
          Ok(views.html.claimworkflow.form(claimworkflowform.fill(doc), (applicableSelection.filterNot(selectedApplicable.contains(_)) ::: doc.app).sorted, assignedToSelections.sorted, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      claimworkflowform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {              
              val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
              val assignedToSelections = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify }
              Ok(views.html.claimworkflow.form(formWithError, applicableSelection.sorted, assignedToSelections.sorted))
            }
          },
          formWithData => {            
            // Update Claim Workflow
            ClaimWorkflowModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
              
            // Create Audit Log 
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)

            Future.successful(Redirect(routes.ClaimSettingController.index))
          } 
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
     
  def delete(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(ClaimWorkflowModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(routes.ClaimSettingController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def checkClaimWorkflowName(p_id:String, p_name:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      val f_workflow = if (p_id == "") {
        ClaimWorkflowModel.findOne(BSONDocument("n" -> p_name), request)
      } else {
        ClaimWorkflowModel.findOne(BSONDocument("_id" -> BSONDocument("$ne" -> BSONObjectID(p_id)), "n" -> p_name), request)
      }
      f_workflow.map( workflow =>
        workflow.isDefined match {
          case true => Ok("false").as("text/plain")
          case _ => Ok("true").as("text/plain")
        }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def isDefault(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){      
      for {
        claimworkflow <- ClaimWorkflowModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      } yield {
        if (claimworkflow.get.d) {
          Ok("true").as("text/plain")
        } else {
          Ok("false").as("text/plain")
        }
      }   
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
}