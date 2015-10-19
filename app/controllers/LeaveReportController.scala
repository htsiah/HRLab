package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeaveModel}

import reactivemongo.bson.{BSONObjectID,BSONDocument}

object LeaveReportController extends Controller with Secured {

    def view(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybeleave.map( leave => 
        Ok(views.html.leavereport.view(leave))
      ).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  }}
    
}