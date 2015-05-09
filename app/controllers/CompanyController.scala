package controllers

import scala.concurrent.Future

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{CompanyModel, OfficeModel, Office}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

import play.api.mvc._

object CompanyController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => { 
    val f_docs = OfficeModel.find(BSONDocument(), request)
    f_docs.map(docs => Ok(views.html.company.index(docs)).withSession(
        (request.session - "path") + ("path"->((routes.CompanyController.index).toString))
    ))
  }}
  
  def updateCompanyName(p_name:String) = withAuth { username => implicit request => {
    for { 
      maybe_company <- CompanyModel.findOne(BSONDocument(), request)
    } yield {
      val company = maybe_company.get
      CompanyModel.update(
          BSONDocument(), 
          company.copy(_id = company._id, c = p_name), 
          request
      )
      val json = Json.parse("{\"name\":\"" + p_name + "\"}")
      Ok(json).as("application/json").withSession(
          request.session + ("company" -> p_name)
      )
    }
  }}
    
}