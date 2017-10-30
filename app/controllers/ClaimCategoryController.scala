package controllers

import scala.concurrent.{Future, Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimCategoryModel, ClaimCategory, AuditLogModel, KeywordModel, PersonModel, OfficeModel}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

class ClaimCategoryController extends Controller with Secured {

  val claimcategoryform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "cat" -> text,
          "all" -> boolean,
          "app" -> list(text),
          "tlim" -> number,
          "hlp" -> text,
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id,cat,all,app,tlim,hlp,sys)=>ClaimCategory(_id,cat,all,app,tlim,hlp,sys)}
      {claimcategory:ClaimCategory=>Some(claimcategory._id, claimcategory.cat, claimcategory.all, claimcategory.app, claimcategory.tlim, claimcategory.hlp, claimcategory.sys)}
  ) 
      
  def view(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybeclaimcategory <- ClaimCategoryModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
      } yield {
        maybeclaimcategory.map( claimcategory => {
          Ok(views.html.claimcategory.view(claimcategory))
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
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
        Ok(views.html.claimcategory.form(claimcategoryform.fill(ClaimCategoryModel.doc), applicableSelection.sorted))
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      claimcategoryform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
              Ok(views.html.claimcategory.form(formWithError, applicableSelection.sorted))
            }
          },
          formWithData => {
            val doc_objectID = BSONObjectID.generate
            ClaimCategoryModel.insert(formWithData.copy(_id=doc_objectID), p_request=request)
            
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
        maybedoc <- ClaimCategoryModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
        persons <- PersonModel.find(BSONDocument(), request)
        offices <- OfficeModel.getAllOfficeName(request)
      } yield {
        maybedoc.map( doc  => {
          val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
          Ok(views.html.claimcategory.form(claimcategoryform.fill(doc), applicableSelection.sorted, p_id))          
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      claimcategoryform.bindFromRequest.fold(
          formWithError => {
            for {
              persons <- PersonModel.find(BSONDocument(), request)
              offices <- OfficeModel.getAllOfficeName(request)
            } yield {
              val applicableSelection = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify } ::: offices
              Ok(views.html.claimcategory.form(formWithError, applicableSelection.sorted, p_id))
            }
          },
          formWithData => {
              
            // Update claim's category if any change 


            // Update Category
            ClaimCategoryModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
              
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
      Await.result(ClaimCategoryModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(routes.ClaimSettingController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def isCatNotUnique(p_id:String, p_cat:String) = withAuth { username => implicit request => {
    val f_category = if (p_id == "") {
      ClaimCategoryModel.findOne(BSONDocument("cat" -> p_cat), request)
    } else {
      ClaimCategoryModel.findOne(BSONDocument("_id" -> BSONDocument("$ne" -> BSONObjectID(p_id)), "cat" -> p_cat), request)
    }
    f_category.map( category =>
      category.isDefined match {
        case true => Ok("false").as("text/plain")
        case _ => Ok("true").as("text/plain")
      }
    )
  }}
  
}