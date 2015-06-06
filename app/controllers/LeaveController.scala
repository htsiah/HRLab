package controllers

import scala.concurrent.{Future,Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeaveModel, Leave, Workflow, LeaveProfileModel, PersonModel, CompanyHolidayModel, LeavePolicyModel, OfficeModel, TaskModel}
import utilities.{System, AlertUtility, Tools, DocNumUtility, MailUtility}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat

object LeaveController extends Controller with Secured {
  
  val leaveform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "docnum" -> number,
          "pid" -> text,
          "pn" -> text,
          "lt" -> text,
          "dt" -> text,
          "fdat" -> optional(jodaDate),
          "tdat" -> optional(jodaDate),
          "r" -> text,
          "uti" -> of[Double],
          "cfuti" -> of[Double],
          "ld" -> boolean,
          "wf" -> mapping(
              "s" -> text,
              "aprid" -> text,
              "aprn" -> text
          )(Workflow.apply)(Workflow.unapply),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))  
      ){(_id,docnum,pid,pn,lt,dt,fdat,tdat,r,uti,cfuti,ld,wf,sys)=>Leave(_id,docnum,pid,pn,lt,dt,fdat,tdat,r,uti,cfuti,ld,wf,sys)}
      {leave:Leave=>Some(leave._id, leave.docnum, leave.pid, leave.pn, leave.lt, leave.dt, leave.fdat, leave.tdat, leave.r, leave.uti, leave.cfuti, leave.ld, leave.wf, leave.sys)}
  )
  
	def create = withAuth { username => implicit request => {
	  for {
	    leavetypes <- LeaveProfileModel.getLeaveTypes(request.session.get("id").get, request)
	    maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(getPersonProfile(request).get.p.mgrid)), request)
	  } yield{
	    val docnum = DocNumUtility.getNumberText("leave", request.session.get("entity").get)
	    maybemanager.map( manager => {
	      Ok(views.html.leave.form(
	        leaveform.fill(LeaveModel.doc.copy(
	            docnum = docnum.toInt,
	            pid = request.session.get("id").get,
	            pn = request.session.get("name").get,
              wf = Workflow(
                  s = "New",
                  aprid = manager._id.stringify,
                  aprn = manager.p.fn + " " + manager.p.ln     
              )
	        )),
	        leavetypes))
	    }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
	}}
	
	def insert = withAuth { username => implicit request => {
	  leaveform.bindFromRequest.fold(
	      formWithError => {
	        for {
	          leavetypes <- LeaveProfileModel.getLeaveTypes(request.session.get("id").get, request)
	        } yield{
	          Ok(views.html.leave.form(formWithError,leavetypes))
	        }
	      },
	      formWithData => {
	        for {
	          leavetypes <- LeaveProfileModel.getLeaveTypes(request.session.get("id").get, request)
	          maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->formWithData.pid , "lt"->formWithData.lt), request)
	          maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> formWithData.lt, "pt" -> getPersonProfile(request).get.p.pt), request)
	          maybeoffice <- OfficeModel.findOne(BSONDocument("n" -> getPersonProfile(request).get.p.off))
	          maybeperson <- PersonModel.findOne(BSONDocument("_id" -> getPersonProfile(request).get._id), request)
            maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(formWithData.wf.aprid)), request)
	          maybealert_missingleavepolicy <- AlertUtility.findOne(BSONDocument("k"->1006))
	          maybealert_notenoughtbalance <- AlertUtility.findOne(BSONDocument("k"->1007))
            maybealert_restrictebeforejoindate <- AlertUtility.findOne(BSONDocument("k"->1013))
	        } yield {
	          if (!maybeleavepolicy.isDefined) {
              // Missing leave policy.
              Ok(views.html.leave.form(leaveform.fill(formWithData), leavetypes, alert=maybealert_missingleavepolicy.getOrElse(null)))
            } else if (maybeperson.get.p.edat.get.isAfter(formWithData.fdat.get.plusDays(1))) {
              // restricted apply leave before employment start date.
              val dtfOut:DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
              val replaceMap = Map(
                  "DATE"-> (dtfOut.print(maybeperson.get.p.edat.get))
              )
              val alert = if ((maybealert_restrictebeforejoindate.getOrElse(null))!=null) { maybealert_restrictebeforejoindate.get.copy(m=Tools.replaceSubString(maybealert_restrictebeforejoindate.get.m, replaceMap.toList)) } else { null }
              Ok(views.html.leave.form(leaveform.fill(formWithData), leavetypes, alert=alert))
            } else {
	            val appliedduration = LeaveModel.getAppliedDuration(formWithData, maybeleavepolicy.get, maybeperson.get, maybeoffice.get, request)
	            val leavebalance = maybeleaveprofile.get.cal.bal 
	            if (leavebalance < appliedduration) {
	              // No enough leave balance
	              Ok(views.html.leave.form(leaveform.fill(formWithData), leavetypes, alert=maybealert_notenoughtbalance.getOrElse(null)))
	            } else {
                val carryforward_bal = maybeleaveprofile.get.cal.cf - maybeleaveprofile.get.cal.cfuti - maybeleaveprofile.get.cal.cfexp
	                           
                // Add Leave
                val leave_update = if (carryforward_bal <= 0) 
                  formWithData.copy(_id = BSONObjectID.generate, wf = formWithData.wf.copy(s = "Pending Approval"), uti = appliedduration, cfuti = 0)
                  else if (carryforward_bal >= appliedduration)
                    formWithData.copy(_id = BSONObjectID.generate, wf = formWithData.wf.copy(s = "Pending Approval"), uti = 0, cfuti = appliedduration)
                    else
                      formWithData.copy(_id = BSONObjectID.generate, wf = formWithData.wf.copy(s = "Pending Approval"), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
                LeaveModel.insert(leave_update, p_request=request)
                
	              // Add ToDo
	              val contentMap = Map(
                    "DOCUNUM"->leave_update.docnum.toString(), 
                    "APPLICANT"->leave_update.pn, 
                    "NUMDAY"->(leave_update.uti + leave_update.cfuti).toString(), 
                    "LEAVETYPE"->leave_update.lt.toLowerCase(), 
                    "FDAT"->leave_update.fdat.get.toLocalDate().toString(), 
                    "TDAT"->leave_update.tdat.get.toLocalDate().toString()
                )
                val buttonMap = Map(
                    "APPROVELINK"->(Tools.hostname + "/leave/approve/" + leave_update._id.stringify), 
                    "DOCLINK"->(Tools.hostname + "/leave/view/" + leave_update._id.stringify)    
                )
                TaskModel.insert(1, leave_update.wf.aprid, leave_update._id.stringify, contentMap, buttonMap, "", request)
                
                // Send email
                val replaceMap = Map("MANAGER"->leave_update.wf.aprn, "APPLICANT"->leave_update.pn, "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), "LEAVETYPE"->leave_update.lt.toLowerCase(), "DOCNUM"->leave_update.docnum.toString(), "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), "URL"->Tools.hostname)
                MailUtility.sendEmailConfig(List(maybemanager.get.p.em), 3, replaceMap)
	              
	              Redirect(routes.DashboardController.index)
	            }
	          }
	        }
        }
	  )
	}}
	
	def view(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
	  } yield {
	    maybeleave.map( leave => 
	      Ok(views.html.leave.view(leave))
	    ).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
	}}
	
	def approve(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
	    maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->maybeleave.get.pid , "lt"->maybeleave.get.lt), request)
	    maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> maybeleave.get.lt, "pt" -> maybeperson.get.p.pt), request)
	    maybeoffice <- OfficeModel.findOne(BSONDocument("n" -> maybeperson.get.p.off))
	    maybealert_missingleavepolicy <- AlertUtility.findOne(BSONDocument("k"->1006))
	    maybealert_notenoughtbalance <- AlertUtility.findOne(BSONDocument("k"->1008))
	  } yield {
	    // Check authorized
	    if (maybeleave.get.wf.s=="Pending Approval" && maybeleave.get.wf.aprid==getPersonProfile(request).get._id.stringify && !maybeleave.get.ld) {
	      
	      // Check leave policy existence
	      if (maybeleavepolicy.isDefined == false) {
	        Ok(views.html.leave.view(maybeleave.get, alert=maybealert_missingleavepolicy.getOrElse(null)))
	      } else {
	        val appliedduration = LeaveModel.getAppliedDuration(maybeleave.get, maybeleavepolicy.get, maybeperson.get, maybeoffice.get, request)
	        val leavebalance = maybeleaveprofile.get.cal.bal 

	        // Check enough leave balance
	        if (leavebalance < appliedduration) {
	          Ok(views.html.leave.view(maybeleave.get, alert=maybealert_notenoughtbalance.getOrElse(null)))
	        } else {
            val carryforward_bal = maybeleaveprofile.get.cal.cf - maybeleaveprofile.get.cal.cfuti - maybeleaveprofile.get.cal.cfexp
            
            // Update Leave
            val leave_update = if (carryforward_bal <= 0)
              maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved"), uti = appliedduration, cfuti = 0)
              else if (carryforward_bal >= appliedduration)
                maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved"), uti = 0, cfuti = appliedduration)
                else
                  maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved"), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
            LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
                
            // Update leave profile
            val leaveprofile_update = if (carryforward_bal <= 0) 
              maybeleaveprofile.get.copy(
                  cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti + appliedduration)
              )
              else if (carryforward_bal >= appliedduration)
                maybeleaveprofile.get.copy(
                    cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + appliedduration)
                )
              else
                maybeleaveprofile.get.copy(
                    cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + carryforward_bal, uti = maybeleaveprofile.get.cal.uti + (appliedduration - carryforward_bal))
                )
            LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
            
            // Update Todo
            Await.result(TaskModel.setCompleted(leave_update._id.stringify, request), Tools.db_timeout)
            
            // Send Email
            val replaceMap = Map("MANAGER"->leave_update.wf.aprn, "APPLICANT"->leave_update.pn, "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), "LEAVETYPE"->leave_update.lt.toLowerCase(), "DOCNUM"->leave_update.docnum.toString(), "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), "URL"->Tools.hostname)
            MailUtility.sendEmailConfig(List(maybeperson.get.p.em), 4, replaceMap)
                
	          Redirect(request.session.get("path").get)
	        }
	      }
	      
	    } else {
	      Ok(views.html.error.unauthorized())
	    }
	  }
	}}
  
  def companyview(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybeleave.map( leave => 
        Ok(views.html.leave.companyview(leave))
      ).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  }}
	
	def reject(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
    } yield {
      // Check authorized
      if (maybeleave.get.wf.s=="Pending Approval" && maybeleave.get.wf.aprid==getPersonProfile(request).get._id.stringify && !maybeleave.get.ld) {
        
        // Update Leave
        val leave_update = maybeleave.get.copy(wf = maybeleave.get.wf.copy( s = "Rejected"))
        LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
        
        // Update Todo
        TaskModel.setCompleted(leave_update._id.stringify, request)
        
        // Send Email
        val replaceMap = Map("MANAGER"->leave_update.wf.aprn, "APPLICANT"->leave_update.pn, "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), "LEAVETYPE"->leave_update.lt.toLowerCase(), "DOCNUM"->leave_update.docnum.toString(), "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), "URL"->Tools.hostname)
        MailUtility.sendEmailConfig(List(maybeperson.get.p.em), 5, replaceMap)
            
        Redirect(request.session.get("path").get)
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}
	
	def cancel(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->maybeleave.get.pid , "lt"->maybeleave.get.lt), request)
      maybeapplicant <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
      maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.wf.aprid)), request)
    } yield {
      if ((maybeleave.get.wf.s=="Pending Approval" || maybeleave.get.wf.s=="Approved") && (maybeleave.get.pid==getPersonProfile(request).get._id.stringify || hasRoles(List("Admin"), request)) && !maybeleave.get.ld) {
        
        // Update Leave
        val leave_update = maybeleave.get.copy(wf = maybeleave.get.wf.copy( s = "Cancelled"))
        LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
        
        if (maybeleave.get.wf.s=="Approved") {
          // Update Leave Profile
          val leaveprofile_update = maybeleaveprofile.get.copy(
              cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti - maybeleave.get.uti, cfuti = maybeleaveprofile.get.cal.cfuti - maybeleave.get.cfuti)
          )
          LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
        }
        
        if (maybeleave.get.wf.s=="Pending Approval") {          
          // Update Todo
          TaskModel.setCompleted(leave_update._id.stringify, request)
        }
                
        // Send Email
        if (request.session.get("username").get == maybeapplicant.get.p.em){
          if (request.session.get("username").get != maybemanager.get.p.em){ 
            val recipients = List(maybemanager.get.p.em)
            val replaceMap = Map("BY"->request.session.get("name").get, "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), "LEAVETYPE"->leave_update.lt.toLowerCase(), "DOCNUM"->leave_update.docnum.toString(), "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), "URL"->Tools.hostname)
            MailUtility.sendEmailConfig(recipients, 6, replaceMap)
          }
        } else {
          val recipients = List(maybeapplicant.get.p.em, maybemanager.get.p.em).filter(_ != request.session.get("username").get)
          val replaceMap = Map("BY"->request.session.get("name").get, "APPLICANT"->leave_update.pn, "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), "LEAVETYPE"->leave_update.lt.toLowerCase(), "DOCNUM"->leave_update.docnum.toString(), "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), "URL"->Tools.hostname)
          MailUtility.sendEmailConfig(recipients, 8, replaceMap)
        }
        
        Redirect(request.session.get("path").get)
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}
  
  // Parameter:
  // p_type: my / [department name]
  def getApprovedLeaveJSON(p_type:String) = withAuth { username => implicit request => {
    var leavejsonstr = ""
    var count = 0
    val fmt = ISODateTimeFormat.dateTime()
        
    if (p_type=="my") {
      for {
        leaves <- LeaveModel.find(BSONDocument("pid"->getPersonProfile(request).get._id.stringify, "wf.s"->"Approved"), request)
      } yield {
        leaves.map ( leave => {
          val title = leave.pn + " (" + leave.lt + ")"
          val url = if (leave.pid==getPersonProfile(request).get._id.stringify || leave.wf.aprid==getPersonProfile(request).get._id.stringify || hasRoles(List("Admin"), request)) "/leave/view/" + leave._id.stringify else ""
          val start = fmt.print(leave.fdat.get)
          val end = fmt.print(leave.tdat.get)
          if (count > 0) leavejsonstr = leavejsonstr + ","
          leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"
          count = count + 1   
        })
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    } else {
      for {
        persons <- PersonModel.find(BSONDocument("p.dpm"->p_type), request)
      } yield {
        persons.map { person => {
          val leaves = Await.result(LeaveModel.find(BSONDocument("pid"->person._id.stringify, "wf.s"->"Approved"), request), Tools.db_timeout)
          leaves.map { leave => {
            val title = leave.pn + " (" + leave.lt + ")"
            val url = if (leave.pid==getPersonProfile(request).get._id.stringify || leave.wf.aprid==getPersonProfile(request).get._id.stringify || hasRoles(List("Admin"), request)) "/leave/view/" + leave._id.stringify else ""
            val start = fmt.print(leave.fdat.get)
            val end = fmt.print(leave.tdat.get)
            if (count > 0) leavejsonstr = leavejsonstr + ","
            leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"
            count = count + 1   
          } }
        } }
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    }
  }}
    
  // Parameter:
  // p_type: my / [department name]
  def getApprovedLeaveForCompanyViewJSON(p_type:String) = withAuth { username => implicit request => {
    var leavejsonstr = ""
    var count = 0
    val fmt = ISODateTimeFormat.dateTime()
        
    if (p_type=="my") {
      for {
        leaves <- LeaveModel.find(BSONDocument("pid"->getPersonProfile(request).get._id.stringify, "wf.s"->"Approved"), request)
      } yield {
        leaves.map ( leave => {
          val title = leave.pn + " (" + leave.lt + ")"
          val url = if (leave.pid==getPersonProfile(request).get._id.stringify || leave.wf.aprid==getPersonProfile(request).get._id.stringify || hasRoles(List("Admin"), request)) "/leave/company/view/" + leave._id.stringify else ""
          val start = fmt.print(leave.fdat.get)
          val end = fmt.print(leave.tdat.get)
          if (count > 0) leavejsonstr = leavejsonstr + ","
          leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"
          count = count + 1   
        })
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    } else {
      for {
        persons <- PersonModel.find(BSONDocument("p.dpm"->p_type), request)
      } yield {
        persons.map { person => {
          val leaves = Await.result(LeaveModel.find(BSONDocument("pid"->person._id.stringify, "wf.s"->"Approved"), request), Tools.db_timeout)
          leaves.map { leave => {
            val title = leave.pn + " (" + leave.lt + ")"
            val url = if (leave.pid==getPersonProfile(request).get._id.stringify || leave.wf.aprid==getPersonProfile(request).get._id.stringify || hasRoles(List("Admin"), request)) "/leave/compnay/view/" + leave._id.stringify else ""
            val start = fmt.print(leave.fdat.get)
            val end = fmt.print(leave.tdat.get)
            if (count > 0) leavejsonstr = leavejsonstr + ","
            leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"
            count = count + 1   
          } }
        } }
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    }
  }}
  
}