package controllers

import scala.concurrent.Future

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.concurrent.Execution.Implicits._

import models.{KeywordModel,Keyword,AuditLogModel}
import utilities.System

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

class KeywordController extends Controller with Secured {
  
  val keywordform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "n" -> text,
          "v" -> optional(list(nonEmptyText)),
          "s" -> boolean,
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id,n,v,s,sys)=>Keyword(_id,n,v,s,sys)}
      {keyword:Keyword=>Some(keyword._id, keyword.n, keyword.v, keyword.s, keyword.sys)}
  ) 

  def index = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      val f_docs = KeywordModel.find(BSONDocument(), request)
      f_docs.map(docs => Ok(views.html.keyword.index(docs)).withSession(
          (request.session - "path") + ("path"->((routes.KeywordController.index).toString))
      ))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybedoc <- KeywordModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      } yield {
        maybedoc.map( doc  => {
          Ok(views.html.keyword.form(keywordform.fill(doc),KeywordModel.getProtectedKey(maybedoc.get, request),p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      keywordform.bindFromRequest.fold(
          formWithError => {
            for { 
              maybedoc <- KeywordModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
            } yield {
              maybedoc.map( doc  => {
                Ok(views.html.keyword.form(formWithError,KeywordModel.getProtectedKey(maybedoc.get, request),p_id))
              }).getOrElse(NotFound)
            }
          },
          formWithData => {
            KeywordModel.update(BSONDocument("_id" -> BSONObjectID(p_id)), formWithData.copy(_id=BSONObjectID(p_id)), request)
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)
            Future.successful(Redirect(routes.KeywordController.index))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}