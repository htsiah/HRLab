package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import models.{LeaveModel}
import scala.concurrent.Future
import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}
import models.TaskModel

object DashboardController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => {
    for {
      leaves <- LeaveModel.find(BSONDocument("pid"->getPersonProfile(request).get._id.stringify, "w_s"->"Pending Approval", "ld" -> false), BSONDocument("docnum" -> -1), request)
      tasks <- TaskModel.find(BSONDocument("pid"->getPersonProfile(request).get._id.stringify, "cf"->false), BSONDocument("sys.cdat" -> -1), request)
    } yield {
      Ok(views.html.dashboard.index(tasks, leaves)).withSession(
          (request.session - "path") + ("path"->((routes.DashboardController.index).toString))
      )
    }
  }}
  
}