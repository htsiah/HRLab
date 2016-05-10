package controllers

import scala.concurrent.{Future,Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ Json, JsObject, JsString }

import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}
import play.modules.reactivemongo.json._

import models.{LeaveModel, Leave, Workflow, LeaveProfileModel, PersonModel, CompanyHolidayModel, LeavePolicyModel, OfficeModel, TaskModel, LeaveFileModel}
import utilities.{System, AlertUtility, Tools, DocNumUtility, MailUtility}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}
import reactivemongo.api.gridfs._
import reactivemongo.api.gridfs.Implicits._

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat

import javax.inject.Inject

class LeaveController @Inject() (val reactiveMongoApi: ReactiveMongoApi, mailerClient: MailerClient) extends Controller with MongoController with ReactiveMongoComponents with Secured {
      
  import MongoController.readFileReads
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
    
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
      leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
	    maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("managerid").get)), request)
	  } yield{
	    val docnum = DocNumUtility.getNumberText("leave", request.session.get("entity").get)
	    maybemanager.map( manager => {
	      Ok(views.html.leave.form(
	        leaveform.fill(LeaveModel.doc.copy(
	            docnum = docnum.toInt,
	            pid = request.session.get("id").get,
	            pn = request.session.get("name").get,
              fdat = Some(new DateTime()),
              tdat = Some(new DateTime()),
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
	          leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
	        } yield{
	          Ok(views.html.leave.form(formWithError,leavetypes))
	        }
	      },
	      formWithData => {
	        for {
	          leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
	          maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->formWithData.pid , "lt"->formWithData.lt), request)
	          maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> formWithData.lt), request)
	          maybeoffice <- OfficeModel.findOne(BSONDocument("n" -> request.session.get("office").get))
	          maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
            maybemanager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(formWithData.wf.aprid)), request)
            maybealert_restrictebeforejoindate <- AlertUtility.findOne(BSONDocument("k"->1013))
            maybefiles <- LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> formWithData.docnum.toString(), "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]()
	        } yield {
            val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }
            if (maybeperson.get.p.edat.get.isAfter(formWithData.fdat.get.plusDays(1))) {
              // restricted apply leave before employment start date.
              val fmt = ISODateTimeFormat.date()
              val replaceMap = Map(
                  "DATE"-> (fmt.print(maybeperson.get.p.edat.get))
              )
              val alert = if ((maybealert_restrictebeforejoindate.getOrElse(null))!=null) { maybealert_restrictebeforejoindate.get.copy(m=Tools.replaceSubString(maybealert_restrictebeforejoindate.get.m, replaceMap.toList)) } else { null }
              Ok(views.html.leave.form(leaveform.fill(formWithData), leavetypes, filename.toString().replaceAll("\"", ""), alert=alert))
            } else {
	            val appliedduration = LeaveModel.getAppliedDuration(formWithData, maybeleavepolicy.get, maybeperson.get, maybeoffice.get, request)
              val carryforward_bal = maybeleaveprofile.get.cal.cf - maybeleaveprofile.get.cal.cfuti - maybeleaveprofile.get.cal.cfexp
	                           
              // Add Leave
              val leave_update = if (carryforward_bal <= 0) 
                formWithData.copy(_id = BSONObjectID.generate, wf = formWithData.wf.copy(s = "Pending Approval"), uti = appliedduration, cfuti = 0)
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
                TaskModel.insert(7, leave_update.wf.aprid, leave_update._id.stringify, contentMap, buttonMap, "", request)
              } else {
                TaskModel.insert(1, leave_update.wf.aprid, leave_update._id.stringify, contentMap, buttonMap, "", request)
              }
 
              // Send email
              val reason = if (leave_update.r == "") {"."} else { " with reason '" + leave_update.r + "'."}
              val replaceMap = Map(
                  "MANAGER"->leave_update.wf.aprn, 
                  "APPLICANT"->leave_update.pn, 
                  "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
                  "LEAVETYPE"->leave_update.lt, 
                  "DOCNUM"->leave_update.docnum.toString(), 
                  "APPROVEURL"->(Tools.hostname+"/leave/approve/"+leave_update._id.stringify+"?p_msg="+leave_update.pn+"%27s leave request (%23"+leave_update.docnum.toString()+") approved."),
                  "REJECTURL"->(Tools.hostname+"/leave/reject?p_id="+leave_update._id.stringify+"&p_msg="+leave_update.pn+"%27s leave request (%23"+leave_update.docnum.toString()+") rejected."), 
                  "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify), 
                  "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                  "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                  "REASON"-> reason,
                  "UTILIZED" -> (leave_update.cfuti + leave_update.uti).toString(),
                  "BALANCE" -> (leaveprofile_update.cal.cbal - (leave_update.cfuti + leave_update.uti)).toString()
              )
              if (leave_update.fdat.get == leave_update.tdat.get) {
                MailUtility.getEmailConfig(List(maybemanager.get.p.em), 10, replaceMap).map { email => mailerClient.send(email) }
              } else {
                MailUtility.getEmailConfig(List(maybemanager.get.p.em), 3, replaceMap).map { email => mailerClient.send(email) }
              }
              Redirect(routes.DashboardController.index)
            }
          }
        }
	  )
	}}
	
	def view(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybefiles <- LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> maybeleave.get.docnum.toString(), "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]()
	  } yield {
	    maybeleave.map( leave => {
        val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }        
        Ok(views.html.leave.view(leave, filename.toString().replaceAll("\"", "")))
      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
	}}
	
	def approve(p_id:String, p_msg:String) = withAuth { username => implicit request => {
	  for {
	    maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
	    maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->maybeleave.get.pid , "lt"->maybeleave.get.lt), request)
	    maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> maybeleave.get.lt), request)
	    maybeoffice <- OfficeModel.findOne(BSONDocument("n" -> maybeperson.get.p.off))
      maybefiles <- LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> maybeleave.get.docnum.toString(), "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]()
	  } yield {
	    // Check authorized
	    if (maybeleave.get.wf.s=="Pending Approval" && maybeleave.get.wf.aprid==request.session.get("id").get && !maybeleave.get.ld) {
	      
        val appliedduration = LeaveModel.getAppliedDuration(maybeleave.get, maybeleavepolicy.get, maybeperson.get, maybeoffice.get, request)
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
              cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti + appliedduration, papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti)
          )
          else if (carryforward_bal >= appliedduration)
            maybeleaveprofile.get.copy(
                cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + appliedduration, papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti)
            )
            else
              maybeleaveprofile.get.copy(
                  cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + carryforward_bal, uti = maybeleaveprofile.get.cal.uti + (appliedduration - carryforward_bal), papr = maybeleaveprofile.get.cal.papr - maybeleave.get.uti - maybeleave.get.cfuti)
              )           
        LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
         
        // Update Todo
        Await.result(TaskModel.setCompleted(leave_update._id.stringify, request), Tools.db_timeout)
            
        // Send Email
        val replaceMap = Map(
            "MANAGER"->leave_update.wf.aprn, 
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
          MailUtility.getEmailConfig(List(maybeperson.get.p.em), 11, replaceMap).map { email => mailerClient.send(email) }
        } else {
          MailUtility.getEmailConfig(List(maybeperson.get.p.em), 4, replaceMap).map { email => mailerClient.send(email) }
        }
        Redirect(request.session.get("path").get).flashing("success" -> p_msg)
	      
	    } else {
	      Ok(views.html.error.unauthorized())
	    }
	  }
	}}
  
  def companyview(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybefiles <- LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> maybeleave.get.docnum.toString(), "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]()
    } yield {
      maybeleave.map( leave => {
        val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }   
        Ok(views.html.leave.companyview(leave, filename.toString().replaceAll("\"", "")))
      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  }}
  	
	def reject(p_id:String, p_msg:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->maybeleave.get.pid , "lt"->maybeleave.get.lt), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleave.get.pid)), request)
    } yield {
      // Check authorized
      if (maybeleave.get.wf.s=="Pending Approval" && maybeleave.get.wf.aprid==request.session.get("id").get && !maybeleave.get.ld) {
                
        // Update Leave
        val leave_update = maybeleave.get.copy(wf = maybeleave.get.wf.copy( s = "Rejected"))
        LeaveModel.update(BSONDocument("_id" -> maybeleave.get._id), leave_update, request)
        
        // Update leave profile
        val leaveprofile_update = maybeleaveprofile.get.copy(
            cal = maybeleaveprofile.get.cal.copy(papr = maybeleaveprofile.get.cal.papr - leave_update.uti - leave_update.cfuti)
        )
        LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
        
        // Update Todo
        Await.result(TaskModel.setCompleted(leave_update._id.stringify, request), Tools.db_timeout)
        
        // Send Email
        val replaceMap = Map(
              "MANAGER"->leave_update.wf.aprn, 
              "APPLICANT"->leave_update.pn, 
              "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
              "LEAVETYPE"->leave_update.lt, 
              "DOCNUM"->leave_update.docnum.toString(), 
              "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
              "BALANCE" -> (leaveprofile_update.cal.cbal + leave_update.cfuti + leave_update.uti).toString(),
              "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify)
        )
        if (leave_update.fdat.get == leave_update.tdat.get) {
          MailUtility.getEmailConfig(List(maybeperson.get.p.em), 12, replaceMap).map { email => mailerClient.send(email) }
        } else {
          MailUtility.getEmailConfig(List(maybeperson.get.p.em), 5, replaceMap).map { email => mailerClient.send(email) }
        }
            
        Redirect(request.session.get("path").get).flashing("success" -> p_msg)
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
      if ((maybeleave.get.wf.s=="Pending Approval" || maybeleave.get.wf.s=="Approved") && (maybeleave.get.pid==request.session.get("id").get || hasRoles(List("Admin"), request)) && !maybeleave.get.ld) {
        
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
          // Update leave profile
          val leaveprofile_update = maybeleaveprofile.get.copy(
              cal = maybeleaveprofile.get.cal.copy(papr = maybeleaveprofile.get.cal.papr - leave_update.uti - leave_update.cfuti)
          )
          LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
        
          // Update Todo
          Await.result(TaskModel.setCompleted(leave_update._id.stringify, request), Tools.db_timeout)
        }
                
        // No email if applicant does not email
        if (!maybeapplicant.get.p.nem){
          // Send Email
          val recipients = List(maybeapplicant.get.p.em, maybemanager.get.p.em, request.session.get("username").get)
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
          if (leave_update.fdat.get == leave_update.tdat.get) {
            MailUtility.getEmailConfig(recipients.distinct, 13, replaceMap).map { email => mailerClient.send(email) }
          } else {
            MailUtility.getEmailConfig(recipients.distinct, 6, replaceMap).map { email => mailerClient.send(email) }
          }
        }
                
        Redirect(request.session.get("path").get)
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}
  
  // Parameter:
  // p_type: my / [department name]
  def getApprovedLeaveJSON(p_type:String, p_withLink:String) = withAuth { username => implicit request => {
    var leavejsonstr = ""
    var count = 0
    val fmt = ISODateTimeFormat.date()
        
    if (p_type=="my") {
      for {
        leaves <- LeaveModel.find(BSONDocument("pid"->request.session.get("id").get, "wf.s"->"Approved"), request)
      } yield {
        leaves.map ( leave => {
          // val reason = if (leave.r != "") { " - " + leave.r } else { "" }
          val reason = ""
          val title = leave.pn + " (" + leave.lt + ") - " + leave.dt + reason
          val url = if (p_withLink=="y") "/leave/view/" + leave._id.stringify else ""
          val start = fmt.print(leave.fdat.get)
          val end = fmt.print(leave.tdat.get.plusDays(1))
          if (count > 0) leavejsonstr = leavejsonstr + ","
          leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"tip\":\"" + title + "\"}"
          count = count + 1   
        })
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    } else if (p_type=="allexceptmy") {
      for {
        leaves <- LeaveModel.find(BSONDocument("pid"->BSONDocument("$ne" -> request.session.get("id").get), "wf.s"->"Approved"), request)
      } yield {
        leaves.map ( leave => {
          val maybe_leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt"->leave.lt), request), Tools.db_timeout)
          val leavepolicy = maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
          if (leavepolicy.set.scal) {            
            // val reason = if (leave.r != "") { " - " + leave.r } else { "" }
            val reason = "" 
            val title = leave.pn + " (" + leave.lt + ") - " + leave.dt + reason
            val url = if (( PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) && p_withLink=="y") "/leave/view/" + leave._id.stringify else ""
              val start = fmt.print(leave.fdat.get)
              val end = fmt.print(leave.tdat.get.plusDays(1))
              if (count > 0) leavejsonstr = leavejsonstr + ","
              leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"tip\":\"" + title + "\"}"
              count = count + 1
          }
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
            val maybe_leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt"->leave.lt), request), Tools.db_timeout)
            val leavepolicy = maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
            if (leavepolicy.set.scal) {
              // val reason = if (leave.r != "") { " - " + leave.r } else { "" }
              val reason = ""
              val title = leave.pn + " (" + leave.lt + ") - " + leave.dt + reason
              val url = if ((leave.pid==request.session.get("id").get || PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) && p_withLink=="y") "/leave/view/" + leave._id.stringify else ""
              val start = fmt.print(leave.fdat.get)
              val end = fmt.print(leave.tdat.get.plusDays(1))
              if (count > 0) leavejsonstr = leavejsonstr + ","
              leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"tip\":\"" + title + "\"}"
              count = count + 1
            }
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
    val fmt = ISODateTimeFormat.date()
        
    if (p_type=="my") {
      for {
        leaves <- LeaveModel.find(BSONDocument("pid"->request.session.get("id").get, "wf.s"->"Approved"), request)
      } yield {
        leaves.map ( leave => {
          // val reason = if (leave.r != "") { " - " + leave.r } else { "" }
          val reason = ""
          val title = leave.pn + " (" + leave.lt + ") - " + leave.dt + reason
          val url = "/leave/company/view/" + leave._id.stringify
          val start = fmt.print(leave.fdat.get)
          val end = fmt.print(leave.tdat.get.plusDays(1))
          if (count > 0) leavejsonstr = leavejsonstr + ","
          leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"tip\":\"" + title + "\"}"
          count = count + 1   
        })
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    } else if (p_type=="allexceptmy") {
      for {
        leaves <- LeaveModel.find(BSONDocument("pid"->BSONDocument("$ne" -> request.session.get("id").get), "wf.s"->"Approved"), request)
      } yield {
        leaves.map ( leave => {
          val maybe_leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt"->leave.lt), request), Tools.db_timeout)
          val leavepolicy = maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
          if (leavepolicy.set.scal) {
            // val reason = if (leave.r != "") { " - " + leave.r } else { "" }
            val reason = ""
            val title = leave.pn + " (" + leave.lt + ") - " + leave.dt + reason
            val url = if (PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) "/leave/company/view/" + leave._id.stringify else ""
            val start = fmt.print(leave.fdat.get)
            val end = fmt.print(leave.tdat.get.plusDays(1))
            if (count > 0) leavejsonstr = leavejsonstr + ","
            leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"tip\":\"" + title + "\"}"
            count = count + 1
          }
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
            val maybe_leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt"->leave.lt), request), Tools.db_timeout)
            val leavepolicy = maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
            if (leavepolicy.set.scal) {
              // val reason = if (leave.r != "") { " - " + leave.r } else { "" }
              val reason = ""
              val title = leave.pn + " (" + leave.lt + ") - " + leave.dt + reason
              val url = if (leave.pid==request.session.get("id").get || PersonModel.isManagerFor(leave.pid, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) "/leave/company/view/" + leave._id.stringify else ""
              val start = fmt.print(leave.fdat.get)
              val end = fmt.print(leave.tdat.get.plusDays(1))
              if (count > 0) leavejsonstr = leavejsonstr + ","
              leavejsonstr = leavejsonstr + "{\"id\":"+ count + ",\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\",\"tip\":\"" + title + "\"}"
              count = count + 1
            }
          } }
        } }
        Ok(Json.parse("[" + leavejsonstr + "]")).as("application/json")
      }
    }
  }}
  
  // Return: {"a":0,"b":0}
  def getApplyDayJSON(p_pid:String, p_lt:String, p_dt:String, p_fdat:String, p_tdat:String) = withAuth { username => implicit request => {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request)
      maybe_office <- OfficeModel.findOne(BSONDocument("n" -> maybe_person.get.p.off))
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
           } else {
             maybe_leavepolicy.map( leavepolicy => {
               val appliedduration = LeaveModel.getAppliedDuration(leave_doc, maybe_leavepolicy.get, maybe_person.get, maybe_office.get, request)
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