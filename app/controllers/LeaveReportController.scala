package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeaveModel, LeaveFileModel, PersonModel}

import reactivemongo.bson.{BSONObjectID,BSONDocument}

class LeaveReportController extends Controller with Secured {
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybefiles <- LeaveFileModel.findByLK(maybeleave.get.docnum.toString(), request).collect[List]()
    } yield {
      maybeleave.map( leave => {
        
        // Viewable by admin, manager, substitute manager and applicant
        if (leave.pid == request.session.get("id").get || PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) {
          val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }
          Ok(views.html.leavereport.view(leave, filename.toString().replaceAll("\"", "")))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  }}
    
}