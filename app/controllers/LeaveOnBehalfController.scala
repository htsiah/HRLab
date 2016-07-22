package controllers

import scala.concurrent.{Future, Await}

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.modules.reactivemongo.json._

import models.{LeaveModel, Leave, Workflow, LeaveProfileModel, PersonModel, LeavePolicyModel, OfficeModel, LeaveFileModel, AuditLogModel}
import utilities.{AlertUtility, Tools, DocNumUtility, MailUtility}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import javax.inject.Inject

case class LeaveOnBehalf (   
    docnum: Int,
    pid: String,
    lt: String,
    dt: String,
    fdat: Option[DateTime],
    tdat: Option[DateTime],
    r: String
)

class LeaveOnBehalfController @Inject() (mailerClient: MailerClient) extends Controller with Secured {
     
  val leaveonbehalfform = Form(
      mapping(
          "docnum" -> number,
          "pid" -> text,
          "lt" -> text,
          "dt" -> text,
          "fdat" -> optional(jodaDate("d-MMM-yyyy")),
          "tdat" -> optional(jodaDate("d-MMM-yyyy")),
          "r" -> text
      )(LeaveOnBehalf.apply)(LeaveOnBehalf.unapply)
  )
    
  def create = withAuth { username => implicit request => {
    
    if(request.session.get("roles").get.contains("Admin")){
      for {
        persons <- PersonModel.find(BSONDocument(), request) 
      } yield {
        val docnum = DocNumUtility.getNumberText("leave", request.session.get("entity").get)
        val filledForm = leaveonbehalfform.fill(LeaveOnBehalf(
          docnum.toInt,
          "",
          "",
          "",
          Some(new DateTime()),
          Some(new DateTime()),
          ""
        ))
        Ok(views.html.leaveonbehalf.form(filledForm, persons, Map()))
      }  
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }

  } }
  
  def insert = withAuth { username => implicit request => {
    
    leaveonbehalfform.bindFromRequest.fold(
        formWithError => {
          for {
            persons <- PersonModel.find(BSONDocument(), request) 
            leavetypes <- LeaveProfileModel.getLeaveTypesSelection(request.session.get("id").get, request)
          } yield{
            Ok(views.html.leaveonbehalf.form(formWithError, persons, leavetypes))
          }
        },
        formWithData => {
          for {
            persons <- PersonModel.find(BSONDocument(), request) 
            leavetypes <- LeaveProfileModel.getLeaveTypesSelection(formWithData.pid, request)
            maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(formWithData.pid)), request)
            maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid"->formWithData.pid , "lt"->formWithData.lt), request)
            maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> formWithData.lt), request)
            maybealert_restrictebeforejoindate <- AlertUtility.findOne(BSONDocument("k"->1017))
            maybefiles <- LeaveFileModel.findByLK(formWithData.docnum.toString(), request).collect[List]()
          } yield {
            val leaveWithData = LeaveModel.doc.copy(
                _id = BSONObjectID.generate,
                docnum = formWithData.docnum,
                pid = formWithData.pid,
                pn = maybeperson.get.p.fn + " " + maybeperson.get.p.ln,
                lt = formWithData.lt,
                dt = formWithData.dt,
                fdat = formWithData.fdat,
                tdat = formWithData.tdat,
                r = formWithData.r,
                wf = Workflow(
                    s = "New",
                    aprid = List(request.session.get("id").get),
                    aprn = List(request.session.get("name").get),
                    aprbyid = Some(List(request.session.get("id").get)),
                    aprbyn = Some(List(request.session.get("name").get)),
                    rjtbyid = None,
                    rjtbyn = None,
                    cclbyid = None,
                    cclbyn = None,
                    aprmthd = "Automatically approved"
                )
            )
            val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }
            if (maybeperson.get.p.edat.get.isAfter(formWithData.fdat.get.plusDays(1))) {
              // restricted apply leave before employment start date.
              val fmt = ISODateTimeFormat.date()
              val replaceMap = Map(
                  "DATE"-> (fmt.print(maybeperson.get.p.edat.get))
              )
              val alert = if ((maybealert_restrictebeforejoindate.getOrElse(null))!=null) { maybealert_restrictebeforejoindate.get.copy(m=Tools.replaceSubString(maybealert_restrictebeforejoindate.get.m, replaceMap.toList)) } else { null }
              Ok(views.html.leaveonbehalf.form(leaveonbehalfform.fill(formWithData), persons, leavetypes, filename.toString().replaceAll("\"", ""), alert=alert))
            } else {
              
              val appliedduration = LeaveModel.getAppliedDuration(leaveWithData, maybeleavepolicy.get, maybeperson.get, request)              
              val carryforward_bal = maybeleaveprofile.get.cal.cf - maybeleaveprofile.get.cal.cfuti - maybeleaveprofile.get.cal.cfexp
                
              // Add Leave
              val leave_objectID = BSONObjectID.generate
              val leave_update = if (carryforward_bal <= 0) 
                leaveWithData.copy(_id = leave_objectID, wf = leaveWithData.wf.copy(s = "Approved"), uti = appliedduration, cfuti = 0)
                else if (carryforward_bal >= appliedduration)
                  leaveWithData.copy(_id = leave_objectID, wf = leaveWithData.wf.copy(s = "Approved"), uti = 0, cfuti = appliedduration)
                  else
                    leaveWithData.copy(_id = leave_objectID, wf = leaveWithData.wf.copy(s = "Approved"), uti = appliedduration - carryforward_bal, cfuti = carryforward_bal)
                    
              LeaveModel.insert(leave_update, p_request=request)
                
              // Insert Audit Log
              AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=leave_objectID.stringify, c="Apply leave request on behalf of " + maybeperson.get.p.fn + " " + maybeperson.get.p.ln + "."), p_request=request)
              
              // Update leave profile
              val leaveprofile_update = if (carryforward_bal <= 0) {
                maybeleaveprofile.get.copy(
                    cal = maybeleaveprofile.get.cal.copy(uti = maybeleaveprofile.get.cal.uti + appliedduration, papr = maybeleaveprofile.get.cal.papr - leaveWithData.uti - leaveWithData.cfuti)
                )
              } else if (carryforward_bal >= appliedduration) {
                maybeleaveprofile.get.copy(
                    cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + appliedduration, papr = maybeleaveprofile.get.cal.papr - leaveWithData.uti - leaveWithData.cfuti)
                )
              } else {
                maybeleaveprofile.get.copy(
                    cal = maybeleaveprofile.get.cal.copy(cfuti = maybeleaveprofile.get.cal.cfuti + carryforward_bal, uti = maybeleaveprofile.get.cal.uti + (appliedduration - carryforward_bal), papr = maybeleaveprofile.get.cal.papr - leaveWithData.uti - leaveWithData.cfuti)
                )
              }
                
              LeaveProfileModel.update(BSONDocument("_id" -> maybeleaveprofile.get._id), leaveprofile_update, request)
              
              // Send Email
              val reason = if (leave_update.r == "") {"."} else { " with reason '" + leave_update.r + "'."}
              if (!maybeperson.get.p.nem) {
                val recipients = List(maybeperson.get.p.em)
                val manager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeperson.get.p.mgrid))), Tools.db_timeout)
                val cc = if ( maybeperson.get.p.smgrid != "" ) { 
                  val smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeperson.get.p.smgrid))), Tools.db_timeout) 
                  List(manager.get.p.em, smanager.get.p.em)
                } else { 
                  List(manager.get.p.em)
                }
                                
                val replaceMap = Map(
                    "BY"->request.session.get("name").get, 
                    "APPLICANT"->leave_update.pn, 
                    "NUMBER"->(leave_update.uti + leave_update.cfuti).toString(), 
                    "LEAVETYPE"->leave_update.lt, 
                    "DOCNUM"->leave_update.docnum.toString(), 
                    "DOCURL"->(Tools.hostname+"/leave/view/"+leave_update._id.stringify),
                    "FROM"->(leave_update.fdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.fdat.get.toLocalDate().toString("MMM") + "-" + leave_update.fdat.get.toLocalDate().getYear + " (" + leave_update.fdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                    "TO"->(leave_update.tdat.get.toLocalDate().getDayOfMonth + "-" + leave_update.tdat.get.toLocalDate().toString("MMM") + "-" + leave_update.tdat.get.toLocalDate().getYear + " (" + leave_update.tdat.get.toLocalDate().dayOfWeek().getAsText + ")"),
                    "REASON"-> reason,
                    "UTILIZED" -> (leave_update.cfuti + leave_update.uti).toString(),
                    "BALANCE" -> (leaveprofile_update.cal.cbal - (leave_update.cfuti + leave_update.uti)).toString()
                )
                if (leave_update.fdat.get == leave_update.tdat.get) {
                  MailUtility.getEmailConfig(recipients, cc.filter { email => email != request.session.get("username").get }, 14, replaceMap).map { email => mailerClient.send(email) }
                } else {
                  MailUtility.getEmailConfig(recipients, cc.filter { email => email != request.session.get("username").get }, 9, replaceMap).map { email => mailerClient.send(email) }
                }
              }
              
              Redirect(routes.DashboardController.index)
            }            
          }
        }
    )
    
  } }

}