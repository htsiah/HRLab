package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID, BSONDocument}

import models.{ClaimModel, LeaveModel, PersonModel, LeaveProfileModel}
import utilities.{Tools}

import org.joda.time.DateTime

class ReportController extends Controller with Secured {
  
  def myleaverequest = withAuth { username => implicit request => { 
    for {
      leaves <- LeaveModel.find(BSONDocument("pid"->request.session.get("id").get), BSONDocument("docnum" -> -1), request)
    } yield {
      render {
        case Accepts.Html() => {
           Ok(views.html.report.myleaverequest()).withSession(
               (request.session - "path") + ("path"->((routes.ReportController.myleaverequest).toString))
           )
         }
         case Accepts.Json() => {
           val leavesMap = leaves.map { leave => Map(
               "lock" -> Json.toJson(if(leave.ld){"(<i class='ace-icon fa fa-lock'></i>)"} else {""}),
               "docnum" -> Json.toJson(leave.docnum),
               "lt" -> Json.toJson(leave.lt),
               "dt" -> Json.toJson(leave.dt),
               "fdat" -> Json.toJson(leave.fdat.get.dayOfMonth().getAsText + "-" + leave.fdat.get.monthOfYear().getAsShortText + "-" + leave.fdat.get.getYear.toString()),
               "tdat" -> Json.toJson(leave.tdat.get.dayOfMonth().getAsText + "-" + leave.tdat.get.monthOfYear().getAsShortText + "-" + leave.tdat.get.getYear.toString()),
               "uti" -> Json.toJson(leave.uti + leave.cfuti),
               "wf_s" -> Json.toJson(leave.wf.s),
               "wf_aprn" -> Json.toJson(leave.wf.aprn.mkString(", ")),
               "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leavereport/view?p_id=" + leave._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
           )}
           Ok(Json.toJson(leavesMap)).as("application/json")  
         }
      }
     }
  }}
    
  def myleaverequestcsv = withAuth { username => implicit request => { 
    for {
      leaves <- LeaveModel.find(BSONDocument("pid"->request.session.get("id").get), BSONDocument("docnum" -> -1), request)
    } yield {
      val filename = "attachment; filename=" + request.session.get("name").get.toString().replaceAll(" ", "") + "-LeaveRequest-" + DateTime.now().dayOfMonth().getAsShortText + DateTime.now().monthOfYear().getAsShortText + DateTime.now().year().getAsShortText + ".csv"
      val header = "Doc Num,Leave Type,Day Type,Submit On,Date From,Date To,Utilized,Carry Forward Utilized,Status,Approval Method,Approver(s),Approved By,Rejected By,Cancelled By,Reason,Lock\n"
      val data = leaves.map { leave => {
        val aprby = if(leave.wf.aprbyn.getOrElse(List())!=List()){ leave.wf.aprbyn.get.mkString("; ") } else { "" }
        leave.docnum + "," + 
        leave.lt + "," + 
        leave.dt + "," + 
        leave.sys.get.cdat.get.dayOfMonth().getAsText + "-" + leave.sys.get.cdat.get.monthOfYear().getAsShortText + "-" + leave.sys.get.cdat.get.getYear.toString() + "," +
        leave.fdat.get.dayOfMonth().getAsText + "-" + leave.fdat.get.monthOfYear().getAsShortText + "-" + leave.fdat.get.getYear.toString() + "," + 
        leave.tdat.get.dayOfMonth().getAsText + "-" + leave.tdat.get.monthOfYear().getAsShortText + "-" + leave.tdat.get.getYear.toString() + "," + 
        leave.uti + "," + 
        leave.cfuti + "," +
        leave.wf.s + "," +
        leave.wf.aprmthd + "," +
        leave.wf.aprn.mkString("; ") + "," +
        aprby + "," +
        leave.wf.rjtbyn.getOrElse("") + "," +
        leave.wf.cclbyn.getOrElse("") + "," +
        Tools.cleanCSV(leave.r) + "," +
        leave.ld
      }}
      
      Ok(header + data.mkString("\n")).withHeaders(
          CONTENT_TYPE -> "text/csv",
          CONTENT_DISPOSITION -> filename
      )
    }
  }}
  
  def myteamleaverequest = withAuth { username => implicit request => { 
    if(request.session.get("ismanager").get.contains("true")){
      for {
        subordinates <- PersonModel.find(BSONDocument("p.mgrid"->request.session.get("id").get), request)
        leaves <- LeaveModel.find(BSONDocument("pid"->BSONDocument("$in"->subordinates.map { subordinate => subordinate._id.stringify })), BSONDocument("pn" -> 1, "docnum" -> -1), request)
      } yield {
        render {
          case Accepts.Html() => {
            Ok(views.html.report.myteamleaverequest()).withSession(
                (request.session - "path") + ("path"->((routes.ReportController.myteamleaverequest).toString))
            )
          }
          case Accepts.Json() => {
            val leavesMap = leaves.map { leave => Map(
                "lock" -> Json.toJson(if(leave.ld){"(<i class='ace-icon fa fa-lock'></i>)"} else {""}),
                "name" -> Json.toJson(leave.pn),
                "docnum" -> Json.toJson(leave.docnum),
                "lt" -> Json.toJson(leave.lt),
                "dt" -> Json.toJson(leave.dt),
                "fdat" -> Json.toJson(leave.fdat.get.dayOfMonth().getAsText + "-" + leave.fdat.get.monthOfYear().getAsShortText + "-" + leave.fdat.get.getYear.toString()),
                "tdat" -> Json.toJson(leave.tdat.get.dayOfMonth().getAsText + "-" + leave.tdat.get.monthOfYear().getAsShortText + "-" + leave.tdat.get.getYear.toString()),
                "uti" -> Json.toJson(leave.uti + leave.cfuti),
                "wf_s" -> Json.toJson(leave.wf.s),
                "wf_aprn" -> Json.toJson(leave.wf.aprn.mkString(", ")),
                "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leavereport/view?p_id=" + leave._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
            )}
            Ok(Json.toJson(leavesMap)).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myteamleaverequestcsv = withAuth { username => implicit request => { 
     if(request.session.get("ismanager").get.contains("true")){
       for {
         maybe_persons <- PersonModel.find(BSONDocument(), request)
         subordinates <- PersonModel.find(BSONDocument("p.mgrid"->request.session.get("id").get), request)
         leaves <- LeaveModel.find(BSONDocument("pid"->BSONDocument("$in"->subordinates.map { subordinate => subordinate._id.stringify })), BSONDocument("pn" -> 1, "docnum" -> -1), request)
       } yield {
         val filename = "attachment; filename=" + request.session.get("name").get.toString().replaceAll(" ", "") + "-TeamLeaveRequest-" + DateTime.now().dayOfMonth().getAsShortText + DateTime.now().monthOfYear().getAsShortText + DateTime.now().year().getAsShortText + ".csv"
         val header = "Employee ID,Applicant,Email,Doc Num,Leave Type,Day Type,Submit On,Date From,Date To,Utilized,Carry Forward Utilized,Status,Approval Method,Approver(s),Approved By,Rejected By,Cancelled By,Reason,Lock\n"
         val persons = maybe_persons.map { person => List(person._id.stringify, person.p.empid, person.p.em) }
         val data = leaves.map { leave => {
           
           val person = persons.filter( value => value(0) == leave.pid )
           val aprby = if(leave.wf.aprbyn.getOrElse(List())!=List()){ leave.wf.aprbyn.get.mkString("; ") } else { "" }
           val empId = if (person.isEmpty) { "" } else { person(0)(1) }
           val email = if (person.isEmpty) { "" } else { person(0)(2) }
           
           empId + "," + 
           leave.pn + "," + 
           email + "," + 
           leave.docnum + "," + 
           leave.lt + "," + 
           leave.dt + "," + 
           leave.sys.get.cdat.get.dayOfMonth().getAsText + "-" + leave.sys.get.cdat.get.monthOfYear().getAsShortText + "-" + leave.sys.get.cdat.get.getYear.toString() + "," +
           leave.fdat.get.dayOfMonth().getAsText + "-" + leave.fdat.get.monthOfYear().getAsShortText + "-" + leave.fdat.get.getYear.toString() + "," + 
           leave.tdat.get.dayOfMonth().getAsText + "-" + leave.tdat.get.monthOfYear().getAsShortText + "-" + leave.tdat.get.getYear.toString() + "," + 
           leave.uti + "," + 
           leave.cfuti + "," +
           leave.wf.s + "," +
           leave.wf.aprmthd + "," +
           leave.wf.aprn.mkString("; ") + "," +
           aprby + "," +
           leave.wf.rjtbyn.getOrElse("") + "," +
           leave.wf.cclbyn.getOrElse("") + "," +
           Tools.cleanCSV(leave.r) + "," +
           leave.ld
         }}
         
         Ok(header + data.mkString("\n")).withHeaders(
             CONTENT_TYPE -> "text/csv",
             CONTENT_DISPOSITION -> filename
         )
       }
     } else {
       Future.successful(Ok(views.html.error.unauthorized()))
     }
  }}

  def myteamleaveprofile = withAuth { username => implicit request => { 
    if(request.session.get("ismanager").get.contains("true")){
      for {
        subordinates <- PersonModel.find(BSONDocument("p.mgrid"->request.session.get("id").get), request)
        leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid"->BSONDocument("$in"->subordinates.map { subordinate => subordinate._id.stringify })), BSONDocument("pn" -> 1, "lt" -> 1), request)
      } yield {
        render {
          case Accepts.Html() => {
            Ok(views.html.report.myteamleaveprofile()).withSession(
                (request.session - "path") + ("path"->((routes.ReportController.myteamleaveprofile).toString))
            )
          }
          case Accepts.Json() => {
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
                "cbal" -> Json.toJson(leaveprofile.cal.cbal),
                "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leaveprofilereport/view?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>"),
                "a_link" -> Json.toJson(
                    "<div class='btn-group'>" + 
                    "<a class='btn btn-xs btn-success' title='View' href='/leaveprofilereport/view?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>" +
                    "<a class='btn btn-xs btn-info' title='Edit' href='/leaveprofilereport/edit?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-pencil bigger-120'></i></a>" +
                    "<a class='btn btn-xs btn-danger' title='Delete' href=" + '"' + "javascript:onDeleteLeaveProfile('" + leaveprofile._id.stringify + "','" + leaveprofile.lt + "','" + leaveprofile.pid + "')" + '"' + "><i class='ace-icon fa fa-trash-o bigger-120'></i></a>" +
                    "</div>"
                )
            )}
            Ok(Json.toJson(leavesMap)).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myteamleaveprofilecsv = withAuth { username => implicit request => {
    if(request.session.get("ismanager").get.contains("true")){
       for {
         maybe_persons <- PersonModel.find(BSONDocument(), request)
         subordinates <- PersonModel.find(BSONDocument("p.mgrid"->request.session.get("id").get), request)
         leaveprofiles <- LeaveProfileModel.find(BSONDocument("pid"->BSONDocument("$in"->subordinates.map { subordinate => subordinate._id.stringify })), BSONDocument("pn" -> 1, "it" -> 1), request)
       } yield {
         val filename = "attachment; filename=" + request.session.get("name").get.toString().replaceAll(" ", "") + "-TeamLeaveProfile-" + DateTime.now().dayOfMonth().getAsShortText + DateTime.now().monthOfYear().getAsShortText + DateTime.now().year().getAsShortText + ".csv"
         val header = "Employee ID,Applicant,Email,Leave Type,Entitlement,Earned,Adjustment,Utilized,Carry Forward,Carry Forward Utilized,Carry Forward Expired,Pending Approval,Balance,Closing Balance," + 
         "Leave Earned Jan,Leave Earned Feb,Leave Earned Mar,Leave Earned Apr,Leave Earned May,Leave Earned Jun,Leave Earned Jul,Leave Earned Aug,Leave Earned Sep,Leave Earned Oct,Leave Earned Nov,Leave Earned Dec," +
         "Eligible Leave Entitlement - Service Month 1,Eligible Leave Entitlement - Entitlement 1,Eligible Leave Entitlement - Carry Forward 1," +
         "Eligible Leave Entitlement - Service Month 2,Eligible Leave Entitlement - Entitlement 2,Eligible Leave Entitlement - Carry Forward 2," +
         "Eligible Leave Entitlement - Service Month 3,Eligible Leave Entitlement - Entitlement 3,Eligible Leave Entitlement - Carry Forward 3," +
         "Eligible Leave Entitlement - Service Month 4,Eligible Leave Entitlement - Entitlement 4,Eligible Leave Entitlement - Carry Forward 4," +
         "Eligible Leave Entitlement - Service Month 5,Eligible Leave Entitlement - Entitlement 5,Eligible Leave Entitlement - Carry Forward 5," +
         "Eligible Leave Entitlement - Service Month 6,Eligible Leave Entitlement - Entitlement 6,Eligible Leave Entitlement - Carry Forward 6," +
         "Eligible Leave Entitlement - Service Month 7,Eligible Leave Entitlement - Entitlement 7,Eligible Leave Entitlement - Carry Forward 7," +
         "Eligible Leave Entitlement - Service Month 8,Eligible Leave Entitlement - Entitlement 8,Eligible Leave Entitlement - Carry Forward 8," +
         "Eligible Leave Entitlement - Service Month 9,Eligible Leave Entitlement - Entitlement 9,Eligible Leave Entitlement - Carry Forward 9," +
         "Eligible Leave Entitlement - Service Month 10,Eligible Leave Entitlement - Entitlement 10,Eligible Leave Entitlement - Carry Forward 10\n"
         val persons = maybe_persons.map { person => List(person._id.stringify, person.p.empid, person.p.em) }
         val data = leaveprofiles.map { leaveprofile => 
           
           val person = persons.filter( value => value(0) == leaveprofile.pid )
           val empId = if (person.isEmpty) { "" } else { person(0)(1) }
           val email = if (person.isEmpty) { "" } else { person(0)(2) }
           
           empId + "," +
           leaveprofile.pn + "," + 
           email + "," +
           leaveprofile.lt + "," + 
           leaveprofile.cal.ent + "," + 
           leaveprofile.cal.ear + "," + 
           leaveprofile.cal.adj + "," + 
           leaveprofile.cal.uti + "," + 
           leaveprofile.cal.cf + "," + 
           leaveprofile.cal.cfuti + "," + 
           leaveprofile.cal.cfexp + "," + 
           leaveprofile.cal.papr + "," + 
           leaveprofile.cal.bal + "," + 
           leaveprofile.cal.cbal + "," + 
           leaveprofile.me.jan + "," + 
           leaveprofile.me.feb + "," + 
           leaveprofile.me.mar + "," + 
           leaveprofile.me.apr + "," + 
           leaveprofile.me.may + "," + 
           leaveprofile.me.jun + "," + 
           leaveprofile.me.jul + "," + 
           leaveprofile.me.aug + "," + 
           leaveprofile.me.sep + "," + 
           leaveprofile.me.oct + "," + 
           leaveprofile.me.nov + "," + 
           leaveprofile.me.dec + "," + 
           leaveprofile.set_ent.e1.s + "," + 
           leaveprofile.set_ent.e1.e + "," +
           leaveprofile.set_ent.e1.cf + "," + 
           leaveprofile.set_ent.e2.s + "," + 
           leaveprofile.set_ent.e2.e + "," +
           leaveprofile.set_ent.e2.cf + "," + 
           leaveprofile.set_ent.e3.s + "," + 
           leaveprofile.set_ent.e3.e + "," +
           leaveprofile.set_ent.e3.cf + "," + 
           leaveprofile.set_ent.e4.s + "," + 
           leaveprofile.set_ent.e4.e + "," +
           leaveprofile.set_ent.e4.cf + "," + 
           leaveprofile.set_ent.e5.s + "," + 
           leaveprofile.set_ent.e5.e + "," +
           leaveprofile.set_ent.e5.cf + "," +
           leaveprofile.set_ent.e6.s + "," + 
           leaveprofile.set_ent.e6.e + "," +
           leaveprofile.set_ent.e6.cf + "," +
           leaveprofile.set_ent.e7.s + "," + 
           leaveprofile.set_ent.e7.e + "," +
           leaveprofile.set_ent.e7.cf + "," +
           leaveprofile.set_ent.e8.s + "," + 
           leaveprofile.set_ent.e8.e + "," +
           leaveprofile.set_ent.e8.cf + "," +
           leaveprofile.set_ent.e9.s + "," + 
           leaveprofile.set_ent.e9.e + "," +
           leaveprofile.set_ent.e9.cf + "," +
           leaveprofile.set_ent.e10.s + "," + 
           leaveprofile.set_ent.e10.e + "," +
           leaveprofile.set_ent.e10.cf
         }
         
         Ok(header + data.mkString("\n")).withHeaders(
             CONTENT_TYPE -> "text/csv",
             CONTENT_DISPOSITION -> filename
         )
       }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
  def allstaffleaverequest = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for {
        leaves <- LeaveModel.find(BSONDocument(), BSONDocument("pn" -> 1), request)
      } yield {
        render {
          case Accepts.Html() => {
            Ok(views.html.report.allstaffleaverequest()).withSession(
                (request.session - "path") + ("path"->((routes.ReportController.allstaffleaverequest).toString))
            )
          }
          case Accepts.Json() => {
            val leavesMap = leaves.map { leave => Map(
                "lock" -> Json.toJson(if(leave.ld){"(<i class='ace-icon fa fa-lock'></i>)"} else {""}),
                "name" -> Json.toJson(leave.pn),
                "docnum" -> Json.toJson(leave.docnum),
                "lt" -> Json.toJson(leave.lt),
                "dt" -> Json.toJson(leave.dt),
                "fdat" -> Json.toJson(leave.fdat.get.dayOfMonth().getAsText + "-" + leave.fdat.get.monthOfYear().getAsShortText + "-" + leave.fdat.get.getYear.toString()),
                "tdat" -> Json.toJson(leave.tdat.get.dayOfMonth().getAsText + "-" + leave.tdat.get.monthOfYear().getAsShortText + "-" + leave.tdat.get.getYear.toString()),
                "uti" -> Json.toJson(leave.uti),
                "cfuti" -> Json.toJson(leave.cfuti),
                "wf_s" -> Json.toJson(leave.wf.s),
                "wf_aprn" -> Json.toJson(leave.wf.aprn.mkString(", ")),
                "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/leavereport/view?p_id=" + leave._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
            )}
            Ok(Json.toJson(leavesMap)).as("application/json")
          }
         }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
  def allstaffleaverequestcsv = withAuth { username => implicit request => { 
     if(request.session.get("roles").get.contains("Admin")){
       for {
         maybe_persons <- PersonModel.find(BSONDocument(), request)
         leaves <- LeaveModel.find(BSONDocument(), BSONDocument("pn" -> 1), request)
       } yield {
         val filename = "attachment; filename=" + "AllStaff-LeaveRequest-" + DateTime.now().dayOfMonth().getAsShortText + DateTime.now().monthOfYear().getAsShortText + DateTime.now().year().getAsShortText + ".csv"
         val header = "Employee ID,Applicant,Email,Doc Num,Leave Type,Day Type,Submit On,Date From,Date To,Utilized,Carry Forward Utilized,Status,Approval Method,Approver(s),Approved By,Rejected By,Cancelled By,Reason,Lock\n"
         val persons = maybe_persons.map { person => List(person._id.stringify, person.p.empid, person.p.em) }
         val data = leaves.map { leave => {         

           val person = persons.filter( value => value(0) == leave.pid )
           val aprby = if(leave.wf.aprbyn.getOrElse(List())!=List()){ leave.wf.aprbyn.get.mkString("; ") } else { "" }
           val empId = if (person.isEmpty) { "" } else { person(0)(1) }
           val email = if (person.isEmpty) { "" } else { person(0)(2) }
           
           empId + "," + 
           leave.pn + "," + 
           email + "," + 
           leave.docnum + "," + 
           leave.lt + "," + 
           leave.dt + "," + 
           leave.sys.get.cdat.get.dayOfMonth().getAsText + "-" + leave.sys.get.cdat.get.monthOfYear().getAsShortText + "-" + leave.sys.get.cdat.get.getYear.toString() + "," +
           leave.fdat.get.dayOfMonth().getAsText + "-" + leave.fdat.get.monthOfYear().getAsShortText + "-" + leave.fdat.get.getYear.toString() + "," + 
           leave.tdat.get.dayOfMonth().getAsText + "-" + leave.tdat.get.monthOfYear().getAsShortText + "-" + leave.tdat.get.getYear.toString() + "," + 
           leave.uti + "," + 
           leave.cfuti + "," +
           leave.wf.s + "," +
           leave.wf.aprmthd + "," +
           leave.wf.aprn.mkString("; ") + "," +
           aprby + "," +
           leave.wf.rjtbyn.getOrElse("") + "," +
           leave.wf.cclbyn.getOrElse("") + "," +
           Tools.cleanCSV(leave.r) + "," +
           leave.ld
         } }
         
         Ok(header + data.mkString("\n")).withHeaders(
             CONTENT_TYPE -> "text/csv",
             CONTENT_DISPOSITION -> filename
         )
       }
     } else {
       Future.successful(Ok(views.html.error.unauthorized()))
     }
  }}
  
  def allstaffleaveprofile = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for {
        leaveprofiles <- LeaveProfileModel.find(BSONDocument(), BSONDocument("pn" -> 1), request)
      } yield {
        render {
          case Accepts.Html() => {
            Ok(views.html.report.allstaffleaveprofile()).withSession(
                (request.session - "path") + ("path"->((routes.ReportController.allstaffleaveprofile).toString))
            )
          }
          case Accepts.Json() => {
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
                "cbal" -> Json.toJson(leaveprofile.cal.cbal),
                "a_link" -> Json.toJson(
                    "<div class='btn-group'>" + 
                    "<a class='btn btn-xs btn-success' title='View' href='/leaveprofilereport/view?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>" +
                    "<a class='btn btn-xs btn-info' title='Edit' href='/leaveprofilereport/edit?p_id=" + leaveprofile._id.stringify + "'><i class='ace-icon fa fa-pencil bigger-120'></i></a>" +
                    "<a class='btn btn-xs btn-danger' title='Delete' href=" + '"' + "javascript:onDeleteLeaveProfile('" + leaveprofile._id.stringify + "','" + leaveprofile.lt + "','" + leaveprofile.pid + "')" + '"' + "><i class='ace-icon fa fa-trash-o bigger-120'></i></a>" +
                    "</div>"
                )
            )}
            Ok(Json.toJson(leavesMap)).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def allstaffleaveprofilecsv = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybe_persons <- PersonModel.find(BSONDocument(), request)
        leaveprofiles <- LeaveProfileModel.find(BSONDocument(), BSONDocument("pn" -> 1), request)
      } yield {
        val filename = "attachment; filename=" + "AllStaffLeaveProfile-" + DateTime.now().dayOfMonth().getAsShortText + DateTime.now().monthOfYear().getAsShortText + DateTime.now().year().getAsShortText + ".csv"
        val header = "Employee ID,Applicant,Email,Leave Type,Entitlement,Earned,Adjustment,Utilized,Carry Forward,Carry Forward Utilized,Carry Forward Expired,Pending Approval,Balance,Closing Balance," + 
        "Leave Earned Jan,Leave Earned Feb,Leave Earned Mar,Leave Earned Apr,Leave Earned May,Leave Earned Jun,Leave Earned Jul,Leave Earned Aug,Leave Earned Sep,Leave Earned Oct,Leave Earned Nov,Leave Earned Dec," +
        "Eligible Leave Entitlement - Service Month 1,Eligible Leave Entitlement - Entitlement 1,Eligible Leave Entitlement - Carry Forward 1," +
        "Eligible Leave Entitlement - Service Month 2,Eligible Leave Entitlement - Entitlement 2,Eligible Leave Entitlement - Carry Forward 2," +
        "Eligible Leave Entitlement - Service Month 3,Eligible Leave Entitlement - Entitlement 3,Eligible Leave Entitlement - Carry Forward 3," +
        "Eligible Leave Entitlement - Service Month 4,Eligible Leave Entitlement - Entitlement 4,Eligible Leave Entitlement - Carry Forward 4," +
        "Eligible Leave Entitlement - Service Month 5,Eligible Leave Entitlement - Entitlement 5,Eligible Leave Entitlement - Carry Forward 5," +
        "Eligible Leave Entitlement - Service Month 6,Eligible Leave Entitlement - Entitlement 6,Eligible Leave Entitlement - Carry Forward 6," +
        "Eligible Leave Entitlement - Service Month 7,Eligible Leave Entitlement - Entitlement 7,Eligible Leave Entitlement - Carry Forward 7," +
        "Eligible Leave Entitlement - Service Month 8,Eligible Leave Entitlement - Entitlement 8,Eligible Leave Entitlement - Carry Forward 8," +
        "Eligible Leave Entitlement - Service Month 9,Eligible Leave Entitlement - Entitlement 9,Eligible Leave Entitlement - Carry Forward 9," +
        "Eligible Leave Entitlement - Service Month 10,Eligible Leave Entitlement - Entitlement 10,Eligible Leave Entitlement - Carry Forward 10\n"
        val persons = maybe_persons.map { person => List(person._id.stringify, person.p.empid, person.p.em) }
        val data = leaveprofiles.map { leaveprofile => 
          
          val person = persons.filter( value => value(0) == leaveprofile.pid )
          val empId = if (person.isEmpty) { "" } else { person(0)(1) }
          val email = if (person.isEmpty) { "" } else { person(0)(2) }
           
          empId + "," +
          leaveprofile.pn + "," + 
          email + "," +
          leaveprofile.lt + "," + 
          leaveprofile.cal.ent + "," + 
          leaveprofile.cal.ear + "," + 
          leaveprofile.cal.adj + "," + 
          leaveprofile.cal.uti + "," + 
          leaveprofile.cal.cf + "," + 
          leaveprofile.cal.cfuti + "," + 
          leaveprofile.cal.cfexp + "," + 
          leaveprofile.cal.papr + "," + 
          leaveprofile.cal.bal + "," + 
          leaveprofile.cal.cbal + "," + 
          leaveprofile.me.jan + "," + 
          leaveprofile.me.feb + "," + 
          leaveprofile.me.mar + "," + 
          leaveprofile.me.apr + "," + 
          leaveprofile.me.may + "," + 
          leaveprofile.me.jun + "," + 
          leaveprofile.me.jul + "," + 
          leaveprofile.me.aug + "," + 
          leaveprofile.me.sep + "," + 
          leaveprofile.me.oct + "," + 
          leaveprofile.me.nov + "," + 
          leaveprofile.me.dec + "," + 
          leaveprofile.set_ent.e1.s + "," + 
          leaveprofile.set_ent.e1.e + "," +
          leaveprofile.set_ent.e1.cf + "," + 
          leaveprofile.set_ent.e2.s + "," + 
          leaveprofile.set_ent.e2.e + "," +
          leaveprofile.set_ent.e2.cf + "," + 
          leaveprofile.set_ent.e3.s + "," + 
          leaveprofile.set_ent.e3.e + "," +
          leaveprofile.set_ent.e3.cf + "," + 
          leaveprofile.set_ent.e4.s + "," + 
          leaveprofile.set_ent.e4.e + "," +
          leaveprofile.set_ent.e4.cf + "," + 
          leaveprofile.set_ent.e5.s + "," + 
          leaveprofile.set_ent.e5.e + "," +
          leaveprofile.set_ent.e5.cf + "," +
          leaveprofile.set_ent.e6.s + "," + 
          leaveprofile.set_ent.e6.e + "," +
          leaveprofile.set_ent.e6.cf + "," +
          leaveprofile.set_ent.e7.s + "," + 
          leaveprofile.set_ent.e7.e + "," +
          leaveprofile.set_ent.e7.cf + "," +
          leaveprofile.set_ent.e8.s + "," + 
          leaveprofile.set_ent.e8.e + "," +
          leaveprofile.set_ent.e8.cf + "," +
          leaveprofile.set_ent.e9.s + "," + 
          leaveprofile.set_ent.e9.e + "," +
          leaveprofile.set_ent.e9.cf + "," +
          leaveprofile.set_ent.e10.s + "," + 
          leaveprofile.set_ent.e10.e + "," +
          leaveprofile.set_ent.e10.cf
        }
        Ok(header + data.mkString("\n")).withHeaders(
            CONTENT_TYPE -> "text/csv",
            CONTENT_DISPOSITION -> filename
        )
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}

  def myclaimrequest = withAuth { username => implicit request => { 

    for {
      claims <- ClaimModel.find(BSONDocument("p.id"->request.session.get("id").get), BSONDocument("docnum" -> -1), request)
    } yield {
      render {
        case Accepts.Html() => {
           Ok(views.html.report.myclaimrequest()).withSession(
               (request.session - "path") + ("path"->((routes.ReportController.myclaimrequest).toString))
           )
         }
         case Accepts.Json() => {
           val claimsMap = claims.map { claim => Map(
               "docnum" -> Json.toJson(claim.docnum),
               "rdat" -> Json.toJson(claim.ed.rdat.get.dayOfMonth().getAsText + "-" + claim.ed.rdat.get.monthOfYear().getAsShortText + "-" + claim.ed.rdat.get.getYear.toString()),
               "cat" -> Json.toJson(claim.ed.cat),
               "amt" -> Json.toJson(claim.ed.amt.ccy + " " + claim.ed.amt.amt),
               "er" -> Json.toJson(claim.ed.er),
               "aamt" -> Json.toJson(claim.ed.aamt.ccy + " " + claim.ed.aamt.amt),
               "tamt" -> Json.toJson(claim.ed.gstamt.tamt.ccy + " " + claim.ed.gstamt.tamt.amt),
               "s" -> Json.toJson(claim.wf.s),
               "papr" -> Json.toJson(claim.wf.papr.n),
               "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/claimreport/view?p_id=" + claim._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
           )}
           Ok(Json.toJson(claimsMap)).as("application/json")  
         }
      }
     }
  
  }}
  
  def allstaffclaimrequest = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for {
        claims <- ClaimModel.find(BSONDocument(), BSONDocument("p.n" -> -1), request)
      } yield {
        render {
          case Accepts.Html() => {
            Ok(views.html.report.allstaffclaimrequest()).withSession(
                (request.session - "path") + ("path"->((routes.ReportController.allstaffclaimrequest).toString))
            )
          }
          case Accepts.Json() => {
           val claimsMap = claims.map { claim => Map(
               "name" -> Json.toJson(claim.p.n),
               "docnum" -> Json.toJson(claim.docnum),
               "rdat" -> Json.toJson(claim.ed.rdat.get.dayOfMonth().getAsText + "-" + claim.ed.rdat.get.monthOfYear().getAsShortText + "-" + claim.ed.rdat.get.getYear.toString()),
               "cat" -> Json.toJson(claim.ed.cat),
               "amt" -> Json.toJson(claim.ed.amt.ccy + " " + claim.ed.amt.amt),
               "er" -> Json.toJson(claim.ed.er),
               "aamt" -> Json.toJson(claim.ed.aamt.ccy + " " + claim.ed.aamt.amt),
               "tamt" -> Json.toJson(claim.ed.gstamt.tamt.ccy + " " + claim.ed.gstamt.tamt.amt),
               "s" -> Json.toJson(claim.wf.s),
               "papr" -> Json.toJson(claim.wf.papr.n),
               "v_link" -> Json.toJson("<a class='btn btn-xs btn-success' title='View' href='/claimreport/view?p_id=" + claim._id.stringify + "'><i class='ace-icon fa fa-search-plus bigger-120'></i></a>")
           )}
           Ok(Json.toJson(claimsMap)).as("application/json")  
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myclaimapproval = TODO

}