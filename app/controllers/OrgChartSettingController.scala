package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.bson.{BSONObjectID, BSONDocument}

import models.{OrgChartSettingModel, PersonModel}

class OrgChartSettingController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        orgchartsetting <- OrgChartSettingModel.findOne(BSONDocument(), request)
        persons <- PersonModel.find(BSONDocument(), request)
      } yield {
        val personlist = persons.map { person => person.p.fn  + " " + person.p.ln + "@|@" + person._id.stringify }
        Ok(views.html.orgchartsetting.index(orgchartsetting.get, personlist.sorted)).withSession(
            (request.session - "path") + ("path"->((routes.OrgChartSettingController.index).toString))
        )
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def updateOrgChartSetting(p_toplevel:String, p_toplevelmanager:List[String]) = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_orgchartsetting <- OrgChartSettingModel.findOne(BSONDocument(), request)
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val orgchartsetting = maybe_orgchartsetting.get
            OrgChartSettingModel.update(
               BSONDocument(), 
               orgchartsetting.copy(
                   _id=orgchartsetting._id,
                   tl=p_toplevel,
                   tlm=p_toplevelmanager
               ), 
               request
            )
            Ok(Json.parse("""{"status":true}""")).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}