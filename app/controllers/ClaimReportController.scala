package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimModel,PersonModel}

import reactivemongo.bson.{BSONObjectID,BSONDocument}

class ClaimReportController extends Controller with Secured {
  
  def view(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeclaim <- ClaimModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
	  } yield {
	    maybeclaim.map( claim => {
        
        // Viewable by admin, manager, substitute manager and applicant
        if (claim.p.id == request.session.get("id").get || PersonModel.isManagerFor(claim.p.id, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(claim.p.id, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) {
          Ok(views.html.claimreport.view(claim))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
  } }
    
}