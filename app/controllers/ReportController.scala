package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID, BSONDocument}

import models.{LeaveModel, PersonModel, LeaveProfileModel}
import utilities.{Tools}

object ReportController extends Controller with Secured {
  
  def myleaverequest = withAuth { username => implicit request => { 
    Future.successful(Ok(views.html.report.myleaverequest()).withSession(
        (request.session - "path") + ("path"->((routes.ReportController.myleaverequest).toString))
    ))
  }}
  
  def myleaverequestJSON = withAuth { username => implicit request => { 
    for {
      leaves <- LeaveModel.find(BSONDocument("pid"->request.session.get("id").get), BSONDocument("docnum" -> -1), request)
    } yield {
      val leavesMap = leaves.map { leave => Map(
          "lock" -> Json.toJson(if(leave.ld){"(<i class='ace-icon fa fa-lock'></i>)"} else {""}),
          "docnum" -> Json.toJson(leave.docnum),
          "lt" -> Json.toJson(leave.lt),
          "dt" -> Json.toJson(leave.dt),
          "fdat" -> Json.toJson(leave.fdat.get.toLocalDate()),
          "tdat" -> Json.toJson(leave.tdat.get.toLocalDate()),
          "uti" -> Json.toJson(leave.uti + leave.cfuti),
          "wf_s" -> Json.toJson(leave.wf.s),
          "wf_aprn" -> Json.toJson(leave.wf.aprn),
          "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leave/report/view/" + leave._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
      )}
      Ok(Json.toJson(leavesMap)).as("application/json")
    }
  }}
  
  def myteamleaverequest = withAuth { username => implicit request => { 
    if(request.session.get("ismanager").get.contains("true")){
      Future.successful(Ok(views.html.report.myteamleaverequest()).withSession(
          (request.session - "path") + ("path"->((routes.ReportController.myteamleaverequest).toString))
      ))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
  def myteamleaverequestJSON = withAuth { username => implicit request => { 
    if(request.session.get("ismanager").get.contains("true")){
      for {
        persons <- PersonModel.find(BSONDocument("p.dpm"->request.session.get("department").get), request)
        leaves <- LeaveModel.find(BSONDocument("pid"->BSONDocument("$in"->persons.map { person => person._id.stringify })), BSONDocument("pn" -> 1, "docnum" -> -1), request)
      } yield {
        val leavesMap = leaves.map { leave => Map(
            "lock" -> Json.toJson(if(leave.ld){"(<i class='ace-icon fa fa-lock'></i>)"} else {""}),
            "name" -> Json.toJson(leave.pn),
            "docnum" -> Json.toJson(leave.docnum),
            "lt" -> Json.toJson(leave.lt),
            "dt" -> Json.toJson(leave.dt),
            "fdat" -> Json.toJson(leave.fdat.get.toLocalDate()),
            "tdat" -> Json.toJson(leave.tdat.get.toLocalDate()),
            "uti" -> Json.toJson(leave.uti + leave.cfuti),
            "wf_s" -> Json.toJson(leave.wf.s),
            "wf_aprn" -> Json.toJson(leave.wf.aprn),
            "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leave/report/view/" + leave._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
        )}
        Ok(Json.toJson(leavesMap)).as("application/json")
      }
    } else {
      Future.successful(Ok(Json.parse("""{"status":false}""")).as("application/json"))
    }
  }}
  
  def myteamleaveprofile = withAuth { username => implicit request => { 
    if(request.session.get("ismanager").get.contains("true")){
      Future.successful(Ok(views.html.report.myteamleaveprofile()).withSession(
          (request.session - "path") + ("path"->((routes.ReportController.myteamleaveprofile).toString))
      ))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myteamleaveprofileJSON = withAuth { username => implicit request => { 
    if(request.session.get("ismanager").get.contains("true")){
      for {
        persons <- PersonModel.find(BSONDocument("p.dpm"->request.session.get("department").get), request)
        leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid"->BSONDocument("$in"->persons.map { person => person._id.stringify })), BSONDocument("pn" -> 1), request)
      } yield {
        val leavesMap = leaveprofiles.map { leaveprofile => {
          Map(
            "name" -> Json.toJson(leaveprofile.pn),
            "lt" -> Json.toJson(leaveprofile.lt),
            "ent" -> Json.toJson(leaveprofile.cal.ent),
            "ear" -> Json.toJson(leaveprofile.cal.ear),
            "adj" -> Json.toJson(leaveprofile.cal.adj),
            "cf" -> Json.toJson(leaveprofile.cal.cf),
            "tuti" -> Json.toJson(leaveprofile.cal.uti + leaveprofile.cal.cfuti),
            "texp" -> Json.toJson(leaveprofile.cal.cfexp),
            "papr" -> Json.toJson(leaveprofile.cal.papr),
            "bal" -> Json.toJson(leaveprofile.cal.bal),
            "cbal" -> Json.toJson(leaveprofile.cal.cbal),
            "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leaveprofilereport/view/" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>"),
            "a_link" -> Json.toJson(
                "<div class='btn-group'>" + 
                "<a class='btn btn-xs btn-success' title='View' href='/leaveprofilereport/view?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>" +
                "<a class='btn btn-xs btn-info' title='Edit' href='/leaveprofilereport/edit?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-pencil bigger-120'></i></a>" +
                "<a class='btn btn-xs btn-danger' title='Delete' href=" + '"' + "javascript:onDeleteLeaveProfile('" + leaveprofile._id.stringify + "','" + leaveprofile.lt + "','" + leaveprofile.pid + "')" + '"' + "><i class='ace-icon fa fa-trash-o bigger-120'></i></a>" +
                "</div>"
            )
            
          )}
        }
        Ok(Json.toJson(leavesMap)).as("application/json")
      }  
    } else {
      Future.successful(Ok(Json.parse("""{"status":false}""")).as("application/json"))
    }
  }}
  
  def allstaffleaverequest = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      Future.successful(Ok(views.html.report.allstaffleaverequest()).withSession(
          (request.session - "path") + ("path"->((routes.ReportController.allstaffleaverequest).toString))
      ))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def allstaffleaverequestJSON = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for {
        leaves <- LeaveModel.find(BSONDocument(), BSONDocument("pn" -> 1), request)
      } yield {
        val leavesMap = leaves.map { leave => Map(
            "lock" -> Json.toJson(if(leave.ld){"(<i class='ace-icon fa fa-lock'></i>)"} else {""}),
            "name" -> Json.toJson(leave.pn),
            "docnum" -> Json.toJson(leave.docnum),
            "lt" -> Json.toJson(leave.lt),
            "dt" -> Json.toJson(leave.dt),
            "fdat" -> Json.toJson(leave.fdat.get.toLocalDate()),
            "tdat" -> Json.toJson(leave.tdat.get.toLocalDate()),
            "uti" -> Json.toJson(leave.uti),
            "cfuti" -> Json.toJson(leave.cfuti),
            "wf_s" -> Json.toJson(leave.wf.s),
            "wf_aprn" -> Json.toJson(leave.wf.aprn),
            "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leave/report/view/" + leave._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
        )}
        Ok(Json.toJson(leavesMap)).as("application/json")
      }
    } else {
      Future.successful(Ok(Json.parse("""{"status":false}""")).as("application/json"))
    }
  }}
  
  def allstaffleaveprofile = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      Future.successful(Ok(views.html.report.allstaffleaveprofile()).withSession(
          (request.session - "path") + ("path"->((routes.ReportController.allstaffleaveprofile).toString))
      )) 
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def allstaffleaveprofileJSON = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for {
        leaveprofiles <- LeaveProfileModel.find(BSONDocument(), BSONDocument("pn" -> 1), request)
      } yield {
        val leavesMap = leaveprofiles.map { leaveprofile => Map(
            "name" -> Json.toJson(leaveprofile.pn),
            "lt" -> Json.toJson(leaveprofile.lt),
            "ent" -> Json.toJson(leaveprofile.cal.ent),
            "ear" -> Json.toJson(leaveprofile.cal.ear),
            "adj" -> Json.toJson(leaveprofile.cal.adj),
            "cf" -> Json.toJson(leaveprofile.cal.cf),
            "tuti" -> Json.toJson(leaveprofile.cal.uti + leaveprofile.cal.cfuti),
            "texp" -> Json.toJson(leaveprofile.cal.cfexp),
            "papr" -> Json.toJson(leaveprofile.cal.papr),
            "bal" -> Json.toJson(leaveprofile.cal.bal),
            "cbal" -> Json.toJson(leaveprofile.cal.cbal)
        )}
        Ok(Json.toJson(leavesMap)).as("application/json")
      }
    } else {
      Future.successful(Ok(Json.parse("""{"status":false}""")).as("application/json"))
    }
  }}

}