package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeaveModel, Leave, Workflow, LeaveProfileModel, PersonModel, CompanyHolidayModel, LeavePolicyModel, OfficeModel, TaskModel, LeaveFileModel, LeaveSettingModel, AuditLogModel, EventModel}
import utilities.{System, AlertUtility, Tools, DocNumUtility, MailUtility}

import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONDateTime}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat

import javax.inject.Inject

class LeaveController @Inject() (mailerClient: MailerClient) extends Controller with Secured {
          
  val leaveform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "docnum" -> number,
          "pid" -> text,
          "pn" -> text,
          "lt" -> text,
          "dt" -> text,
          "fdat" -> optional(jodaDate("d-MMM-yyyy")),
          "tdat" -> optional(jodaDate("d-MMM-yyyy")),
          "r" -> text,
          "uti" -> of[Double],
          "cfuti" -> of[Double],
          "ld" -> boolean,
          "wf" -> mapping(
              "s" -> text,
              "aprid" -> list(text),
              "aprn" -> list(text),
              "aprbyid" -> optional(list(text)),
              "aprbyn" -> optional(list(text)),
              "rjtbyid" -> optional(text),
              "rjtbyn" -> optional(text),
              "cclbyid" -> optional(text),
              "cclbyn" -> optional(text),
              "aprmthd" -> text
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
      leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
      maybeleavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
	  } yield{
	    val docnum = DocNumUtility.getNumberText("leave", request.session.get("entity").get)
	    maybeperson.map( person => {
        
        val approvalmethod = maybeleavesetting.get.aprmthd
        val manager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(person.p.mgrid))), Tools.db_timeout)
        
        val leave:Form[Leave] = if (approvalmethod == "Only manager is authorized to approve leave request") {
          leaveform.fill(LeaveModel.doc.copy(
              docnum = docnum.toInt,
              pid = request.session.get("id").get,
              pn = request.session.get("name").get,
              fdat = Some(new DateTime()),
              tdat = Some(new DateTime()),
              wf = Workflow(
                  s = "New",
                  aprid = List(manager.get._id.stringify),
                  aprn = List(manager.get.p.fn + " " + manager.get.p.ln),
                  aprbyid = None,
                  aprbyn = None,
                  rjtbyid = None,
                  rjtbyn = None,
                  cclbyid = None,
                  cclbyn = None,
                  aprmthd = "Only manager is authorized to approve leave request"
              )
          ))
        } else if (approvalmethod == "Both manager and substitute manager must approve leave request") {
          if (person.p.smgrid != "") {
            val smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(person.p.smgrid))), Tools.db_timeout)
            leaveform.fill(LeaveModel.doc.copy(
                docnum = docnum.toInt,
                pid = request.session.get("id").get,
                pn = request.session.get("name").get,
                fdat = Some(new DateTime()),
                tdat = Some(new DateTime()),
                wf = Workflow(
                    s = "New",
                    aprid = List(manager.get._id.stringify, smanager.get._id.stringify),
                    aprn = List(manager.get.p.fn + " " + manager.get.p.ln, smanager.get.p.fn + " " + smanager.get.p.ln),
                    aprbyid = None,
                    aprbyn = None,
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Both manager and substitute manager must approve leave request"
                )
            )) 
          } else {
            leaveform.fill(LeaveModel.doc.copy(
                docnum = docnum.toInt,
                pid = request.session.get("id").get,
                pn = request.session.get("name").get,
                fdat = Some(new DateTime()),
                tdat = Some(new DateTime()),
                wf = Workflow(
                    s = "New",
                    aprid = List(manager.get._id.stringify),
                    aprn = List(manager.get.p.fn + " " + manager.get.p.ln),
                    aprbyid = None,
                    aprbyn = None,
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Both manager and substitute manager must approve leave request"
                )
            ))
          }
        } else if (approvalmethod == "Either manager or substitute manager is authorized to approve leave request") {
          if (person.p.smgrid != "") {
            val smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(person.p.smgrid))), Tools.db_timeout)
            leaveform.fill(LeaveModel.doc.copy(
                docnum = docnum.toInt,
                pid = request.session.get("id").get,
                pn = request.session.get("name").get,
                fdat = Some(new DateTime()),
                tdat = Some(new DateTime()),
                wf = Workflow(
                    s = "New",
                    aprid = List(manager.get._id.stringify, smanager.get._id.stringify),
                    aprn = List(manager.get.p.fn + " " + manager.get.p.ln, smanager.get.p.fn + " " + smanager.get.p.ln),
                    aprbyid = None,
                    aprbyn = None,
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Either manager or substitute manager is authorized to approve leave request"
                )
            )) 
          } else {
            leaveform.fill(LeaveModel.doc.copy(
                docnum = docnum.toInt,
                pid = request.session.get("id").get,
                pn = request.session.get("name").get,
                fdat = Some(new DateTime()),
                tdat = Some(new DateTime()),
                wf = Workflow(
                    s = "New",
                    aprid = List(manager.get._id.stringify),
                    aprn = List(manager.get.p.fn + " " + manager.get.p.ln),
                    aprbyid = None,
                    aprbyn = None,
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Either manager or substitute manager is authorized to approve leave request"
                )
            ))
          }
        } else {
          
          val ismanageronleave = Await.result(LeaveModel.isOnleave(person.p.mgrid, new DateTime().toLocalDate().toDateTimeAtStartOfDay(), request), Tools.db_timeout)
          if (ismanageronleave && person.p.smgrid != "") {
            val smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(person.p.smgrid))), Tools.db_timeout)
            leaveform.fill(LeaveModel.doc.copy(
                docnum = docnum.toInt,
                pid = request.session.get("id").get,
                pn = request.session.get("name").get,
                fdat = Some(new DateTime()),
                tdat = Some(new DateTime()),
                wf = Workflow(
                    s = "New",
                    aprid = List(manager.get._id.stringify, smanager.get._id.stringify),
                    aprn = List(manager.get.p.fn + " " + manager.get.p.ln, smanager.get.p.fn + " " + smanager.get.p.ln),
                    aprbyid = None,
                    aprbyn = None,
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Either manager or substitute manager is authorized to approve leave request"
                )
            ))
          } else {
            leaveform.fill(LeaveModel.doc.copy(
                docnum = docnum.toInt,
                pid = request.session.get("id").get,
                pn = request.session.get("name").get,
                fdat = Some(new DateTime()),
                tdat = Some(new DateTime()),
                wf = Workflow(
                    s = "New",
                    aprid = List(manager.get._id.stringify),
                    aprn = List(manager.get.p.fn + " " + manager.get.p.ln),
                    aprbyid = None,
                    aprbyn = None,
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Only manager is authorized to approve leave request"
                )
            ))
          }
        }
	      Ok(views.html.leave.form(leave, leavetypes, maybeleavesetting.get.freq, LeaveSettingModel.getCutOffDate(maybeleavesetting.get.cfm).minusDays(1)))
      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
	}}
	
	def insert = withAuth { username => implicit request => {
	  leaveform.bindFromRequest.fold(
	      formWithError => {
	        for {
	          leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
            maybeleavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
	        } yield{
	          Ok(views.html.leave.form(formWithError,leavetypes, maybeleavesetting.get.freq, LeaveSettingModel.getCutOffDate(maybeleavesetting.get.cfm).minusDays(1)))
	        }
	      },
	      formWithData => {
	        for {
	          leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
	          maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->formWithData.pid , "lt"->formWithData.lt), request)
	          maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> formWithData.lt), request)
	          maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
            maybealert_restrictebeforejoindate <- AlertUtility.findOne(BSONDocument("k"->1013))
            maybefiles <- LeaveFileModel.findByLK(formWithData.docnum.toString(), request).collect[List]()
            maybeleavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
	        } yield {
                        
            val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }
            if (maybeperson.get.p.edat.get.isAfter(formWithData.fdat.get.plusDays(1))) {
              // restricted apply leave before employment start date.
              val fmt = ISODateTimeFormat.date()
              val replaceMap = Map(
                  "DATE"-> (fmt.print(maybeperson.get.p.edat.get))
              )
              val alert = if ((maybealert_restrictebeforejoindate.getOrElse(null))!=null) { maybealert_restrictebeforejoindate.get.copy(m=Tools.replaceSubString(maybealert_restrictebeforejoindate.get.m, replaceMap.toList)) } else { null }
              Ok(views.html.leave.form(leaveform.fill(formWithData), leavetypes, maybeleavesetting.get.freq, LeaveSettingModel.getCutOffDate(maybeleavesetting.get.cfm).minusDays(1), filename.toString().replaceAll("\"", ""), alert=alert))
            } else {
	            val appliedduration = LeaveModel.getAppliedDuration(formWithData, maybeleavepolicy.get, maybeperson.get, request)
              val carryforward_bal = maybeleaveprofile.get.cal.cf - maybeleaveprofile.get.cal.cfuti - maybeleaveprofile.get.cal.cfexp
	                           
              // Add Leave
              val leave_objectID = BSONObjectID.generate
              val leave_update = if (carryforward_bal <= 0) 
                formWithData.copy(_id = leave_objectID, wf = formWithData.wf.copy(s = "Pending Approval"), uti = appliedduration, cfuti = 0)
                else if (carryforward_bal >= appliedduration)
                  formWithData.copy(_id = BSONObjectID.generate, wf = formWithData.wf.copy(s = "Pending Approval"), uti = 0, cfuti = appliedduration)
                  else
                    formWithData.copy(_id = BSONObjectID.generate, wf = formWithData.wf.copy(s = "Pending Approval"), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
                    
              LeaveModel.insert(leave_update, p_request=request)
                 
              // Update leave profile
              val leaveprofile_update = maybeleaveprofile.get.copy(
                  cal = maybeleaveprofile.get.cal.copy(papr = maybeleaveprofile.get.cal.papr + leave_update.uti + leave_update.cfuti)
              )
              LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
                
              // Loop thru approver list to add to do list and send email
              leave_update.wf.aprid.foreach { approverid =>  {
                
                val approver = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(approverid))), Tools.db_timeout)
                
                // Add ToDo
                val contentMap = Map(
                    "DOCUNUM"->leave_update.docnum.toString(), 
                    "APPLICANT"->leave_update.pn, 
                    "NUMDAY"->(leave_update.uti + leave_update.cfuti).toString(), 
                    "LEAVETYPE"->leave_update.lt, 
                    "FDAT"->(leave_update.fdat.get.dayOfMonth().getAsText + "-" + leave_update.fdat.get.monthOfYear().getAsShortText + "-" + leave_update.fdat.get.getYear.toString()),
                    "TDAT"->(leave_update.tdat.get.dayOfMonth().getAsText + "-" + leave_update.tdat.get.monthOfYear().getAsShortText + "-" + leave_update.tdat.get.getYear.toString())
                )
                val buttonMap = Map(
                    "APPROVELINK"->(Tools.hostname + "/leave/approve/" + leave_update._id.stringify), 
                    "DOCLINK"->(Tools.hostname + "/leave/view/" + leave_update._id.stringify)    
                )
                
                if (leave_update.fdat.get == leave_update.tdat.get) {
                  TaskModel.insert(7, approver.get._id.stringify, leave_update._id.stringify, contentMap, buttonMap, "", request)
                } else {
                  TaskModel.insert(1, approver.get._id.stringify, leave_update._id.stringify, contentMap, buttonMap, "", request)
                }
                
                // Send email
                val reason = if (leave_update.r == "") {"."} else { " with reason '" + leave_update.r + "'."}
                val replaceMap = Map(
                    "MANAGER"->(approver.get.p.fn + " " + approver.get.p.ln), 
                    "APPLICANT"->leave_update.pn, 
                    "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
                    "LEAVETYPE"->leave_update.lt, 
                    "DOCNUM"->leave_update.docnum.toString(), 
                    "APPROVEURL"->(Tools.hostname+"/leave/approve/"+leave_update._id.stringify+"?p_path=/dashboard&p_msg="+leave_update.pn+"%27s leave request (%23"+leave_update.docnum.toString()+") approved."),
                    "REJECTURL"->(Tools.hostname+"/leave/reject?p_id="+leave_update._id.stringify+"&p_path=/dashboard&p_msg="+leave_update.pn+"%27s leave request (%23"+leave_update.docnum.toString()+") rejected."), 
                    "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), 
                    "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                    "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                    "REASON"-> reason,
                    "UTILIZED" -> (leave_update.cfuti + leave_update.uti).toString(),
                    "BALANCE" -> (leaveprofile_update.cal.cbal - (leave_update.cfuti + leave_update.uti)).toString()
                )
                    
                if (leave_update.fdat.get == leave_update.tdat.get) {
                  MailUtility.getEmailConfig(List(approver.get.p.em), 10, replaceMap).map { email => mailerClient.send(email) }
                } else {
                  MailUtility.getEmailConfig(List(approver.get.p.em), 3, replaceMap).map { email => mailerClient.send(email) }
                }
                
              }}
              
              // Insert Audit Log
              AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=leave_objectID.stringify, c="Apply leave request."), p_request=request)
                
              Redirect(routes.DashboardController.index)
            }
          }
        }
	  )
	}}
	
	def view(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybefiles <- LeaveFileModel.findByLK(maybeleave.get.docnum.toString(), request).collect[List]()
	  } yield {
	    maybeleave.map( leave => {
        
        // Viewable by admin, manager, substitute manager and applicant
        if (leave.pid == request.session.get("id").get || PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) {
          val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }        
          Ok(views.html.leave.view(leave, filename.toString().replaceAll("\"", "")))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
	}}
	
	def approve(p_id:String, p_path:String, p_msg:String) = withAuth { username => implicit request => {
	  for {
	    maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeapplicant <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
	    maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->maybeleave.get.pid , "lt"->maybeleave.get.lt), request)
	    maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> maybeleave.get.lt), request)
      maybefiles <- LeaveFileModel.findByLK(maybeleave.get.docnum.toString(), request).collect[List]()
	  } yield {
	    // Check authorized
	    if (maybeleave.get.wf.s=="Pending Approval" && maybeleave.get.wf.aprid.contains(request.session.get("id").get) && !maybeleave.get.wf.aprbyid.getOrElse(List()).contains(request.session.get("id").get) && !maybeleave.get.ld) {
	      
        val appliedduration = LeaveModel.getAppliedDuration(maybeleave.get, maybeleavepolicy.get, maybeapplicant.get, request)
        val carryforward_bal = maybeleaveprofile.get.cal.cf - maybeleaveprofile.get.cal.cfuti - maybeleaveprofile.get.cal.cfexp
        
        if (maybeleave.get.wf.aprmthd == "Both manager and substitute manager must approve leave request" && maybeleave.get.wf.aprid.length > 1) {
          
          if (maybeleave.get.wf.aprbyid.getOrElse(List()).length > 0) {
            // Update Leave
            val approversbyid = maybeleave.get.wf.aprbyid.getOrElse(List()) ::: List(request.session.get("id").get)
            val approversbyn = maybeleave.get.wf.aprbyn.getOrElse(List()) ::: List(request.session.get("name").get)
            val leave_update = if (carryforward_bal <= 0)
              maybeleave.get.copy(wf = maybeleave.get.wf.copy(s="Approved", aprbyid=Some(approversbyid), aprbyn=Some(approversbyn)), uti = appliedduration, cfuti = 0)
              else if (carryforward_bal >= appliedduration)
                maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved", aprbyid=Some(approversbyid), aprbyn=Some(approversbyn)), uti = 0, cfuti = appliedduration)
                else
                  maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved", aprbyid=Some(approversbyid), aprbyn=Some(approversbyn)), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
            LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
            
            // Update leave profile
            val leaveprofile_update = if (carryforward_bal <= 0) 
              maybeleaveprofile.get.copy(cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti + appliedduration, papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti))
              else if (carryforward_bal >= appliedduration)
                maybeleaveprofile.get.copy(cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + appliedduration, papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti))
                else
                  maybeleaveprofile.get.copy(cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + carryforward_bal, uti = maybeleaveprofile.get.cal.uti + (appliedduration - carryforward_bal), papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti))           
            LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)      
            
            // Update Todo
            Await.result(TaskModel.setCompleted(request.session.get("id").get, leave_update._id.stringify, request), Tools.db_timeout)
            
            // Send Email
            val replaceMap = Map(
                "APPLICANT"->leave_update.pn,
                "APPROVEDBY1"->leave_update.wf.aprbyn.get(0),
                "APPROVEDBY2"->leave_update.wf.aprbyn.get(1),
                "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
                "LEAVETYPE"->leave_update.lt, 
                "DOCNUM"->leave_update.docnum.toString(), 
                "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                "BALANCE" -> (leaveprofile_update.cal.cbal).toString(),
                "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify)
            )
            if (leave_update.fdat.get == leave_update.tdat.get) {
              MailUtility.getEmailConfig(List(maybeapplicant.get.p.em), 17, replaceMap).map { email => mailerClient.send(email) }
            } else {
              MailUtility.getEmailConfig(List(maybeapplicant.get.p.em), 18, replaceMap).map { email => mailerClient.send(email) }
            }
          } else {
            // Update Leave
            val approversbyid = List(request.session.get("id").get)
            val approversbyn = List(request.session.get("name").get)
            val leave_update = if (carryforward_bal <= 0)
              maybeleave.get.copy(wf = maybeleave.get.wf.copy(aprbyid=Some(approversbyid), aprbyn=Some(approversbyn)), uti = appliedduration, cfuti = 0)
              else if (carryforward_bal >= appliedduration)
                maybeleave.get.copy(wf = maybeleave.get.wf.copy(aprbyid=Some(approversbyid), aprbyn=Some(approversbyn)), uti = 0, cfuti = appliedduration)
                else
                  maybeleave.get.copy(wf = maybeleave.get.wf.copy(aprbyid=Some(approversbyid), aprbyn=Some(approversbyn)), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
            LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
            
            // Update Todo
            Await.result(TaskModel.setCompleted(request.session.get("id").get, leave_update._id.stringify, request), Tools.db_timeout)
            
            // Send Email            
            val replaceMap = Map(
                "APPLICANT"->leave_update.pn,
                "APPROVEDBY"->request.session.get("name").get,
                "PENDINGBY"->leave_update.wf.aprn.filter( approver => approver != request.session.get("name").get ).mkString,
                "LEAVETYPE"->leave_update.lt, 
                "DOCNUM"->leave_update.docnum.toString(), 
                "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify)
            )
            if (leave_update.fdat.get == leave_update.tdat.get) {
              MailUtility.getEmailConfig(List(maybeapplicant.get.p.em), 15, replaceMap).map { email => mailerClient.send(email) }
            } else {
              MailUtility.getEmailConfig(List(maybeapplicant.get.p.em), 16, replaceMap).map { email => mailerClient.send(email) }
            }
          }
          
        } else {
          // Update Leave
          val leave_update = if (carryforward_bal <= 0)
            maybeleave.get.copy(wf = maybeleave.get.wf.copy(s="Approved", aprbyid=Some(List(request.session.get("id").get)), aprbyn=Some(List(request.session.get("name").get))), uti = appliedduration, cfuti = 0)
            else if (carryforward_bal >= appliedduration)
              maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved", aprbyid=Some(List(request.session.get("id").get)), aprbyn=Some(List(request.session.get("name").get))), uti = 0, cfuti = appliedduration)
              else
                maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Approved", aprbyid=Some(List(request.session.get("id").get)), aprbyn=Some(List(request.session.get("name").get))), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
          LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
          
          // Update leave profile
          val leaveprofile_update = if (carryforward_bal <= 0) 
            maybeleaveprofile.get.copy(cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti + appliedduration, papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti))
            else if (carryforward_bal >= appliedduration)
              maybeleaveprofile.get.copy(cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + appliedduration, papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti))
              else
                maybeleaveprofile.get.copy(cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + carryforward_bal, uti = maybeleaveprofile.get.cal.uti + (appliedduration - carryforward_bal), papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti))           
          LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)       
                
          // Update Todo
          Await.result(TaskModel.setCompletedMulti(leave_update._id.stringify, request), Tools.db_timeout)
          
          // Send Email
          val cc = {
            if (leave_update.wf.aprid.length > 1) {
              val ccid = leave_update.wf.aprid.filter ( approver => approver != request.session.get("id").get )
              val ccperson = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(ccid(0)))), Tools.db_timeout)
              List(ccperson.get.p.em)
            } else {
              List()
            }
          }
          val replaceMap = Map(
              "APPROVER"->request.session.get("name").get, 
              "APPLICANT"->leave_update.pn, 
              "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
              "LEAVETYPE"->leave_update.lt, 
              "DOCNUM"->leave_update.docnum.toString(), 
              "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "BALANCE" -> (leaveprofile_update.cal.cbal).toString(),
              "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify)           
          )
          if (leave_update.fdat.get == leave_update.tdat.get) {
            MailUtility.getEmailConfig(List(maybeapplicant.get.p.em), cc, 11, replaceMap).map { email => mailerClient.send(email) }
          } else {
            MailUtility.getEmailConfig(List(maybeapplicant.get.p.em), cc, 4, replaceMap).map { email => mailerClient.send(email) }
          }
        }
        
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Approve leave request."), p_request=request)
        
        if (p_path!="") {
          Redirect(p_path).flashing("success" -> p_msg)
        } else {
          Redirect(request.session.get("path").get).flashing("success" -> p_msg)
        }
	      
	    } else {
	      Ok(views.html.error.unauthorized())
	    }
	  }
	}}
  
  def companyview(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybefiles <- LeaveFileModel.findByLK(maybeleave.get.docnum.toString(), request).collect[List]()
    } yield {
      maybeleave.map( leave => {

        // Viewable by admin, manager, substitute manager and applicant
        if (leave.pid == request.session.get("id").get || PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) {
          val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }   
          Ok(views.html.leave.companyview(leave, filename.toString().replaceAll("\"", "")))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  }}
  	
	def reject(p_id:String, p_path:String, p_msg:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->maybeleave.get.pid , "lt"->maybeleave.get.lt), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
    } yield {
      // Check authorized
      if (maybeleave.get.wf.s=="Pending Approval" && maybeleave.get.wf.aprid.contains(request.session.get("id").get) && !maybeleave.get.wf.aprbyid.contains(request.session.get("id").get) && !maybeleave.get.ld) {
                
        // Update Leave
        val leave_update = maybeleave.get.copy(wf = maybeleave.get.wf.copy(s="Rejected", rjtbyid=Some(request.session.get("id").get), rjtbyn=Some(request.session.get("name").get)))
        LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
        
        // Update leave profile
        val leaveprofile_update = maybeleaveprofile.get.copy(
            cal = maybeleaveprofile.get.cal.copy(papr = maybeleaveprofile.get.cal.papr - leave_update.uti - leave_update.cfuti)
        )
        LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
        
        // Update Todo
        Await.result(TaskModel.setCompletedMulti(leave_update._id.stringify, request), Tools.db_timeout)
        
        // Send Email
        val cc = {
          if (leave_update.wf.aprid.length > 1) {
            val ccid = leave_update.wf.aprid.filter ( approver => approver != request.session.get("id").get )
            val ccperson = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(ccid(0)))), Tools.db_timeout)
            List(ccperson.get.p.em)
          } else {
            List()
          }
        }
        val replaceMap = Map(
              "APPLICANT"->leave_update.pn,
              "REJECTER"->leave_update.wf.rjtbyn.get, 
              "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
              "LEAVETYPE"->leave_update.lt, 
              "DOCNUM"->leave_update.docnum.toString(), 
              "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "BALANCE" -> (leaveprofile_update.cal.cbal + leave_update.cfuti + leave_update.uti).toString(),
              "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify)
        )
        if (leave_update.fdat.get == leave_update.tdat.get) {
          MailUtility.getEmailConfig(List(maybeperson.get.p.em), cc, 12, replaceMap).map { email => mailerClient.send(email) }
        } else {
          MailUtility.getEmailConfig(List(maybeperson.get.p.em), cc, 5, replaceMap).map { email => mailerClient.send(email) }
        }
            
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Reject leave request."), p_request=request)
        
        if (p_path!="") {
          Redirect(p_path).flashing("success" -> p_msg)
        } else {
          Redirect(request.session.get("path").get).flashing("success" -> p_msg)
        }
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
    } yield {
      if ((maybeleave.get.wf.s=="Pending Approval" || maybeleave.get.wf.s=="Approved") && (maybeleave.get.pid==request.session.get("id").get || hasRoles(List("Admin"), request)) && !maybeleave.get.ld) {
        
        // Update Leave
        val leave_update = maybeleave.get.copy(wf = maybeleave.get.wf.copy(s = "Cancelled", cclbyid=Some(request.session.get("id").get), cclbyn=Some(request.session.get("name").get)))
        LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
        
        if (maybeleave.get.wf.s=="Approved") {
          // Update Leave Profile
          val leaveprofile_update = maybeleaveprofile.get.copy(
              cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti - maybeleave.get.uti, cfuti = maybeleaveprofile.get.cal.cfuti - maybeleave.get.cfuti)
          )
          LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
        }
        
        if (maybeleave.get.wf.s=="Pending Approval") {    
          // Update leave profile
          val leaveprofile_update = maybeleaveprofile.get.copy(
              cal = maybeleaveprofile.get.cal.copy(papr = maybeleaveprofile.get.cal.papr - leave_update.uti - leave_update.cfuti)
          )
          LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
        
          // Update Todo
          // Normally cancel by applicant and task was pending by another person, thus can be asynchronous.
          TaskModel.setCompletedMulti(leave_update._id.stringify, request)
        }
                
        // No email if applicant does not email
        if (!maybeapplicant.get.p.nem){
          // Send Email
          val approvers = leave_update.wf.aprid.map { approverid => {
            val approver = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(approverid))), Tools.db_timeout)
            approver.get.p.em
          } }
          
          val recipients = (approvers ::: List(maybeapplicant.get.p.em)).filter( approver => approver != request.session.get("username").get )
          val replaceMap = Map(
              "BY"->request.session.get("name").get, 
              "APPLICANT"->leave_update.pn,
              "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
              "LEAVETYPE"->leave_update.lt, 
              "DOCNUM"->leave_update.docnum.toString(), 
              "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "UTILIZED" -> (leave_update.cfuti + leave_update.uti).toString(),
              "BALANCE" -> (maybeleaveprofile.get.cal.cbal + leave_update.cfuti + leave_update.uti).toString(),
              "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify)
          )
          
          if (leave_update.pid == request.session.get("id").get) {
            if (leave_update.fdat.get == leave_update.tdat.get) {
              MailUtility.getEmailConfig(recipients.distinct, 19, replaceMap).map { email => mailerClient.send(email) }
            } else {
              MailUtility.getEmailConfig(recipients.distinct, 20, replaceMap).map { email => mailerClient.send(email) }
            }
          } else {
             if (leave_update.fdat.get == leave_update.tdat.get) {
               MailUtility.getEmailConfig(recipients.distinct, 13, replaceMap).map { email => mailerClient.send(email) }
             } else {
               MailUtility.getEmailConfig(recipients.distinct, 6, replaceMap).map { email => mailerClient.send(email) }
             }
          }
         
        }
           
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Cancel leave request."), p_request=request)
        
        Redirect(request.session.get("path").get)
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}

  // Parameter:
  // p_type: my  [department name]
  // p_withLink: y/y  [include url link]
  // p_page: [which calendar page]
  def getApprovedLeave(p_type:String, p_withLink:String, p_page:String, p_sdat:String, p_edat:String) = withAuth { username => implicit request => {
    val fmt = ISODateTimeFormat.date()
    if (p_type=="my") {
      for {
        leaves <- {
          if (p_sdat!="" || p_edat!="") {
            LeaveModel.find(BSONDocument("pid"->request.session.get("id").get, "wf.s"->"Approved", "fdat"->BSONDocument("$lte"->BSONDateTime(new DateTime(p_edat).getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(new DateTime(p_sdat).getMillis()))), request)
          } else {
            LeaveModel.find(BSONDocument("pid"->request.session.get("id").get, "wf.s"->"Approved"), request)
          }          
        }
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val leaveJSONList = leaves.zipWithIndex.map { case (leave, c) => {
              val title = leave.pn + " (" + leave.lt + ") - " + leave.dt
              val url = if (p_withLink=="y") { 
                if (p_page=="company") { "\"url\":\"/leave/company/view/" + leave._id.stringify + "\","   } else { "\"url\":\"/leave/view/" + leave._id.stringify + "\","  }
                } else { "" }
              val start = fmt.print(leave.fdat.get)
              val end = if (leave.fdat.get == leave.tdat.get) { "" } else { "\"end\":\"" + fmt.print(leave.tdat.get.plusDays(1)) + "\"," }
              "{\"id\":"+ c + ",\"title\":\"" + title + "\"," + url + "\"start\":\"" + start + "\"," + end + "\"tip\":\"" + title + "\"}"
            }}
            
            Ok(Json.parse("[" + leaveJSONList.mkString(",") + "]")).as("application/json")
          }
        }
      }
    } else if (p_type=="allexceptmy") {
      for {
        leaves <- {
          if (p_sdat!="" || p_edat!="") {
            LeaveModel.find(BSONDocument("pid"->BSONDocument("$ne" -> request.session.get("id").get), "wf.s"->"Approved", "fdat"->BSONDocument("$lte"->BSONDateTime(new DateTime(p_edat).getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(new DateTime(p_sdat).getMillis()))), request)
          } else {
            LeaveModel.find(BSONDocument("pid"->BSONDocument("$ne" -> request.session.get("id").get), "wf.s"->"Approved"), request)
          }
        }
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val leaveJSONList = leaves.zipWithIndex.map { case (leave, c) => {
              val maybe_leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt"->leave.lt), request), Tools.db_timeout)
              val leavepolicy = maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
              if (leavepolicy.set.scal) {            
                val title = leave.pn + " (" + leave.lt + ") - " + leave.dt
                val url = if (( PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) && p_withLink=="y") { 
                  if (p_page=="company") { "\"url\":\"/leave/company/view/" + leave._id.stringify + "\","   } else { "\"url\":\"/leave/view/" + leave._id.stringify + "\","  }
                } else { "" }
                val start = fmt.print(leave.fdat.get)
                val end = if (leave.fdat.get == leave.tdat.get) { "" } else { "\"end\":\"" + fmt.print(leave.tdat.get.plusDays(1)) + "\"," }
                "{\"id\":"+ c + ",\"title\":\"" + title + "\"," + url + "\"start\":\"" + start + "\"," + end + "\"tip\":\"" + title + "\"}"
              }
            }}

            Ok(Json.parse("[" + leaveJSONList.mkString(",") + "]")).as("application/json")
          }
        }
      }
    } else {
      for {
        persons <- PersonModel.find(BSONDocument("p.dpm"->p_type), request)
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val personList = persons.map ( person => person._id.stringify)
            val leaves = if (p_sdat!="" || p_edat!="") {
              Await.result(LeaveModel.find(BSONDocument("pid"->BSONDocument("$in"->personList), "wf.s"->"Approved", "fdat"->BSONDocument("$lte"->BSONDateTime(new DateTime(p_edat).getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(new DateTime(p_sdat).getMillis()))), request), Tools.db_timeout)
            } else {
              Await.result(LeaveModel.find(BSONDocument("pid"->BSONDocument("$in"->personList), "wf.s"->"Approved"), request), Tools.db_timeout)
            }
            val leaveJSONList = leaves.zipWithIndex.map { case (leave, c) => {
              val maybe_leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt"->leave.lt), request), Tools.db_timeout)
              val leavepolicy = maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
              if (leavepolicy.set.scal) {
                val title = leave.pn + " (" + leave.lt + ") - " + leave.dt
                val url = if ((leave.pid==request.session.get("id").get || PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) && p_withLink=="y") { 
                  if (p_page=="company") { "\"url\":\"/leave/company/view/" + leave._id.stringify + "\","   } else { "\"url\":\"/leave/view/" + leave._id.stringify + "\","  }
                  } else { "" }
                val start = fmt.print(leave.fdat.get)
                val end = if (leave.fdat.get == leave.tdat.get) { "" } else { "\"end\":\"" + fmt.print(leave.tdat.get.plusDays(1)) + "\"," }
                "{\"id\":"+ c + ",\"title\":\"" + title + "\"," + url + "\"start\":\"" + start + "\"," + end + "\"tip\":\"" + title + "\"}"
              }
            } }
            
            Ok(Json.parse("[" + leaveJSONList.mkString(",") + "]")).as("application/json")
          }
        }
      }
    }
  }}
    
  // Return: {"a":0,"b":0}
  def getApplyDayJSON(p_pid:String, p_lt:String, p_dt:String, p_fdat:String, p_tdat:String, p_cevent:String) = withAuth { username => implicit request => {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request)
      maybe_leaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->p_pid , "lt"->p_lt), request)
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> p_lt), request)
    } yield {
      render {
         case Accepts.Html() => Ok(views.html.error.unauthorized())
         case Accepts.Json() => {
           val dtf = DateTimeFormat.forPattern("d-MMM-yyyy");
           val leave_doc = LeaveModel.doc.copy(
               pid = p_pid,
               dt = p_dt,
               fdat = Some(new DateTime(dtf.parseLocalDate(p_fdat).toDateTimeAtStartOfDay())),
               tdat = Some(new DateTime(dtf.parseLocalDate(p_tdat).toDateTimeAtStartOfDay()))
           )
           
           if (LeaveModel.isOverlap(leave_doc, request)) {
             val json = Json.obj("a" -> "0", "b" -> "0", "msg" -> "overlap")
             Ok(json).as("application/json")
           } else if (p_cevent=="true" && !maybe_leavepolicy.get.set.nwd && EventModel.isRestriction(leave_doc.fdat.get, leave_doc.tdat.get, maybe_person.get, request)) {
             val json = Json.obj("a" -> "0", "b" -> "0", "msg" -> "restricted on event")
             Ok(json).as("application/json")
           } else {
             maybe_leavepolicy.map( leavepolicy => {
               val appliedduration = LeaveModel.getAppliedDuration(leave_doc, maybe_leavepolicy.get, maybe_person.get, request)
               val leavebalance = if (maybe_leavepolicy.get.set.acc == "Monthly - utilisation based on earned") { maybe_leaveprofile.get.cal.bal } else { maybe_leaveprofile.get.cal.cbal }             
               val json = Json.obj("a" -> appliedduration.toString(), "b" -> (leavebalance - appliedduration).toString(), "msg" -> "")
               Ok(json).as("application/json")
             }).getOrElse({        
               val json = Json.obj("daytype" -> "error")
               Ok(json).as("application/json")
             })
           }
         }
      }
    }
  }}
  
}