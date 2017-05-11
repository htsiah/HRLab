package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{PersonModel, Person, Profile, OrgChartSettingModel}
import utilities.{Tools}

import reactivemongo.api._
import reactivemongo.bson._

class OrgChartController extends Controller with Secured {
     
  def index =withAuth { username => implicit request => { 
    Future.successful(Ok(views.html.orgchart.index()).withSession(
        (request.session - "path") + ("path"->((routes.OrgChartController.index).toString))
    ))
  }}
  
  // Get Top Level Manager
  def getChart = withAuth { username => implicit request => { 
    for {
      maybe_OrgChartSetting <- OrgChartSettingModel.findOne(BSONDocument(), request)
    } yield {
      render {
        case Accepts.Html() => Ok(views.html.error.unauthorized())
        case Accepts.Json() => {
          val chartId = if ( maybe_OrgChartSetting.get.tl == "Automatic - employee who report to himself will be at the top-level" ) {
            val persons = Await.result(PersonModel.find(BSONDocument(), BSONDocument("_id" -> 1, "p.mgrid" -> 1), request), Tools.db_timeout)
            persons.filter { 
              person => person.getAs[BSONObjectID]("_id").get.stringify == person.getAs[BSONDocument]("p").get.getAs[String]("mgrid").get
            }.map { person => person.getAs[BSONObjectID]("_id").get.stringify }
          } else {
            maybe_OrgChartSetting.get.tlm
          }
          val json = Json.obj("ids" -> Json.toJson(chartId), "verticalDepth" -> maybe_OrgChartSetting.get.vdepth)
          Ok(json).as("application/json")
        }
      }
    }
  }}
  
  // Build Chart using Top Level Manageer
  def getchartstructure(p_id:String) = withAuth { username => implicit request => { 
    for {
      maybe_persons <- PersonModel.find(BSONDocument("_id" -> BSONObjectID(p_id)), BSONDocument("_id" -> 1, "p.mgrid" -> 1), request)
    } yield {
      render {
        case Accepts.Html() => Ok(views.html.error.unauthorized())
        case Accepts.Json() => Ok(PersonModel.buildReporting(p_id, request)).as("application/json")      
      }
    }
  }}
  
}