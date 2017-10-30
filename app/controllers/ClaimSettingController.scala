package controllers

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimCategoryModel, ClaimWorkflowModel}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

import play.api.mvc._

class ClaimSettingController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        claimcategories <- ClaimCategoryModel.find(BSONDocument(), request)
        claimworkflows <- ClaimWorkflowModel.find(BSONDocument(), request)
      } yield {
        Ok(views.html.claimsetting.index(claimcategories, claimworkflows)).withSession(
            (request.session - "path") + ("path"->((routes.ClaimSettingController.index).toString))
        )
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}