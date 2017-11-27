package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import models.{LeaveModel,LeaveProfileModel,ClaimModel,ClaimSettingModel}
import scala.concurrent.Future
import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}
import models.TaskModel
import utilities._

class DashboardController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => {
    for {
      leaves <- LeaveModel.find(BSONDocument("pid"->request.session.get("id").get, "wf.s"->"Pending Approval", "ld" -> false), BSONDocument("docnum" -> -1), request)
      claims <- ClaimModel.find(BSONDocument("p.id"->request.session.get("id").get, "wf.wfs"->BSONDocument("$ne"->"End") ), BSONDocument("docnum" -> -1), request)
      tasks <- TaskModel.find(BSONDocument("pid"->request.session.get("id").get, "cf"->false), BSONDocument("sys.cdat" -> -1), request)
      leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid"->request.session.get("id").get), BSONDocument("lt" -> 1), request)
      calmsetting <- ClaimSettingModel.findOne(BSONDocument(), request)
    } yield {
      Ok(views.html.dashboard.index(tasks, claims, leaves, leaveprofiles, calmsetting.get.dis)).withSession(
          (request.session - "path") + ("path"->((routes.DashboardController.index).toString))
      )
    }
  }}
  
}