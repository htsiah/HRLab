package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimFileModel, ClaimModel,PersonModel}

import reactivemongo.bson.{BSONObjectID,BSONDocument}

class ClaimReportController extends Controller with Secured {
  
  def view(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeclaim <- ClaimModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
	    maybefiles <- ClaimFileModel.findByLK(maybeclaim.get.docnum.toString(), request).collect[List]()
	  } yield {
	    val files = maybefiles.map( file => {
	      val id = file.id.toString()
	      Map(id.substring(1, id.length()-1) -> file.filename.getOrElse("No filename"))
	    })
	    
	    maybeclaim.map( claim => {
        
        // Viewable by admin, manager, substitute manager and applicant
        if (claim.p.id == request.session.get("id").get || claim.wf.aprid.contains(request.session.get("id").get) || hasRoles(List("Admin"), request)) {
          Ok(views.html.claimreport.view(claim, files))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
  } }
    
}