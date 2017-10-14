package controllers

import scala.concurrent.{Future, Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.concurrent.Execution.Implicits._

import models.{ExchangeRateModel,ExchangeRate,AuditLogModel,KeywordModel}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

class ExchangeRateController extends Controller with Secured {
  
  val exchangerateform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "fct" -> text,
          "fccy" -> text,
          "tct" -> text,
          "tccy" -> text,
          "er" -> of[Double],
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id,fct,fccy,tct,tccy,er,sys)=>ExchangeRate(_id,fct,fccy,tct,tccy,er,sys)}
      {exchangerate:ExchangeRate=>Some(exchangerate._id,exchangerate.fct,exchangerate.fccy,exchangerate.tct,exchangerate.tccy,exchangerate.er, exchangerate.sys)}
  ) 
  
  def index = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      val f_docs = ExchangeRateModel.find(BSONDocument(), request)
      f_docs.map(docs => Ok(views.html.exchangerate.index(docs)).withSession(
          (request.session - "path") + ("path"->((routes.ExchangeRateController.index).toString))
      ))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
  def view(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_exchangerate <- ExchangeRateModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      } yield {
        maybe_exchangerate.map( exchangerate => {
          Ok(views.html.exchangerate.view(exchangerate))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      val doc = ExchangeRateModel.doc
      for { 
        maybe_country<- KeywordModel.findOne(BSONDocument("n" -> "Country"), request)
        maybe_currency<- KeywordModel.findOne(BSONDocument("n" -> "Currency"), request)
      } yield {
        val country = maybe_country.getOrElse(KeywordModel.doc)
        val currency = maybe_currency.getOrElse(KeywordModel.doc)
        Ok(views.html.exchangerate.form(exchangerateform.fill(doc), country.v, currency.v))
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      exchangerateform.bindFromRequest.fold(
          formWithError => {
            for {
              maybe_country<- KeywordModel.findOne(BSONDocument("n" -> "Country"), request)
              maybe_currency<- KeywordModel.findOne(BSONDocument("n" -> "Currency"), request)
            } yield {
              val country = maybe_country.getOrElse(KeywordModel.doc)
              val currency = maybe_currency.getOrElse(KeywordModel.doc)
              Ok(views.html.exchangerate.form(formWithError, country.v, currency.v))
            }
          },
          formWithData => {
            val doc_objectID = BSONObjectID.generate
            ExchangeRateModel.insert(formWithData.copy(_id=doc_objectID), p_request=request)
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."), p_request=request)
            Future.successful(Redirect(routes.ExchangeRateController.index))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_exchangerate <- ExchangeRateModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybe_country<- KeywordModel.findOne(BSONDocument("n" -> "Country"), request)
        maybe_currency<- KeywordModel.findOne(BSONDocument("n" -> "Currency"), request)
      } yield {
        maybe_exchangerate.map( exchangerate  => {
          val country = maybe_country.getOrElse(KeywordModel.doc)
          val currency = maybe_currency.getOrElse(KeywordModel.doc)
          Ok(views.html.exchangerate.form(exchangerateform.fill(exchangerate), country.v, currency.v, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}

  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      exchangerateform.bindFromRequest.fold(
          formWithError => {
            for { 
              maybe_country<- KeywordModel.findOne(BSONDocument("n" -> "Country"), request)
              maybe_currency<- KeywordModel.findOne(BSONDocument("n" -> "Currency"), request)
            } yield {
              val country = maybe_country.getOrElse(KeywordModel.doc)
              val currency = maybe_currency.getOrElse(KeywordModel.doc)
              Ok(views.html.exchangerate.form(formWithError, country.v, currency.v))
            }
          },
          formWithData => {
            ExchangeRateModel.update(
                BSONDocument("_id" -> BSONObjectID(p_id)), 
                formWithData.copy(_id=BSONObjectID(p_id)), 
                request
            )
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)
            Future.successful(Redirect(routes.ExchangeRateController.index))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
   
  def delete(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(ExchangeRateModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(routes.ExchangeRateController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}