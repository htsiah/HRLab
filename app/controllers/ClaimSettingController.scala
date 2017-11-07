package controllers

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimCategoryModel, ClaimWorkflowModel, ClaimSettingModel}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

import play.api.mvc._
import play.api.libs.json._

class ClaimSettingController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        claimcategories <- ClaimCategoryModel.find(BSONDocument(), request)
        claimworkflows <- ClaimWorkflowModel.find(BSONDocument(), request)
        claimsetting <- ClaimSettingModel.findOne(BSONDocument(), request)
      } yield {
        Ok(views.html.claimsetting.index(claimcategories, claimworkflows, claimsetting.get)).withSession(
            (request.session - "path") + ("path"->((routes.ClaimSettingController.index).toString))
        )
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def updateDisableRequest(p_val:String) = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_claimsetting <- ClaimSettingModel.findOne(BSONDocument(), request)
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val claimsetting = maybe_claimsetting.get
            val disablerequest = if(p_val=="Yes") { true } else { false }
            ClaimSettingModel.update(
               BSONDocument(), 
               claimsetting.copy(
                   _id=claimsetting._id,
                   dis=disablerequest
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