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
          "app" -> text,
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
      ){(_id,cat,all,app,tlim,hlp,sys)=>ClaimCategory(_id,cat,all,app.split(",").toList,tlim,hlp,sys)}
      {claimcategory:ClaimCategory=>Some(claimcategory._id, claimcategory.cat, claimcategory.all, claimcategory.app.mkString(","), claimcategory.tlim, claimcategory.hlp, claimcategory.sys)}
  ) 
      
  def view(p_id:String) = TODO
  
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
  
  def insert = TODO
  
  def edit(p_id:String) = TODO

  def update(p_id:String) = TODO
   
  def delete(p_id:String) = TODO
  
}