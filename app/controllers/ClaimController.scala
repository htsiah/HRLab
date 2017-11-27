package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimSettingModel, ClaimWorkflowModel, ConfigCurrencyCodeModel, ClaimModel, Claim, ExpenseDetail, TaxDetail, ClaimFormWorkflow, ClaimFormWorkflowStatus, ClaimFormWorkflowAssignTo, ClaimFormWorkflowAction, ClaimFormWorkflowActionDate, PersonDetail, CurrencyAmount, ClaimCategoryModel, PersonModel, OfficeModel, TaskModel, AuditLogModel}
import utilities.{System, AlertUtility, Tools, DocNumUtility, MailUtility}

import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONDateTime}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat

import javax.inject.Inject

class ClaimController @Inject() (mailerClient: MailerClient) extends Controller with Secured {

  val claimform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "docnum" -> number,
          "p" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
          "ed" -> mapping(
              "rdat" -> optional(jodaDate("d-MMM-yyyy")),
              "cat" -> text,
              "glc" -> text,
              "amt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply),
              "er" -> of[Double],
              "aamt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply),
              "gstamt" -> mapping("cn" -> text, "crnum" -> text, "tnum" -> text, "tamt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply))(TaxDetail.apply)(TaxDetail.unapply),
              "iamt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply),
              "d" -> text
          )(ExpenseDetail.apply)(ExpenseDetail.unapply),
          "wf" -> mapping(
              "papr" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "s" -> text,
              "wfs" -> text,
              "aprid" ->  list(text)
          )(ClaimFormWorkflow.apply)(ClaimFormWorkflow.unapply),
          "wfs" -> mapping(
              "s1" -> text,
              "s2" -> text,
              "s3" -> text,
              "s4" -> text,
              "s5" -> text,
              "s6" -> text,
              "s7" -> text,
              "s8" -> text,
              "s9" -> text,
              "s10" -> text
          )(ClaimFormWorkflowStatus.apply)(ClaimFormWorkflowStatus.unapply),
          "wfat" -> mapping(
              "at1" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at2" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at3" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at4" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at5" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at6" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at7" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at8" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at9" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
              "at10" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply)
          )(ClaimFormWorkflowAssignTo.apply)(ClaimFormWorkflowAssignTo.unapply),
          "wfa" -> mapping(
              "a1" -> text,
              "a2" -> text,
              "a3" -> text,
              "a4" -> text,
              "a5" -> text,
              "a6" -> text,
              "a7" -> text,
              "a8" -> text,
              "a9" -> text,
              "a10" -> text
          )(ClaimFormWorkflowAction.apply)(ClaimFormWorkflowAction.unapply),
          "wdadat" ->  mapping(
              "ad1" -> optional(jodaDate("d-MMM-yyyy")),
              "ad2" -> optional(jodaDate("d-MMM-yyyy")),
              "ad3" -> optional(jodaDate("d-MMM-yyyy")),
              "ad4" -> optional(jodaDate("d-MMM-yyyy")),
              "ad5" -> optional(jodaDate("d-MMM-yyyy")),
              "ad6" -> optional(jodaDate("d-MMM-yyyy")),
              "ad7" -> optional(jodaDate("d-MMM-yyyy")),
              "ad8" -> optional(jodaDate("d-MMM-yyyy")),
              "ad9" -> optional(jodaDate("d-MMM-yyyy")),
              "ad10" -> optional(jodaDate("d-MMM-yyyy"))
          )(ClaimFormWorkflowActionDate.apply)(ClaimFormWorkflowActionDate.unapply),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply)) 
      ){(_id,docnum,p,ed,wf,wfs,wfat,wfa,wdadat,sys)=>Claim(_id,docnum,p,ed,wf,wfs,wfat,wfa,wdadat,sys)}
      {claim:Claim=>Some(claim._id, claim.docnum, claim.p, claim.ed, claim.wf, claim.wfs, claim.wfat, claim.wfa, claim.wdadat, claim.sys)}
  )
  
  def create = withAuth { username => implicit request => {
    for {
      maybe_wfcategories <- ClaimCategoryModel.find(BSONDocument(), request)
      maybe_currencies <- ConfigCurrencyCodeModel.find(BSONDocument())
      maybe_office <- OfficeModel.findOne(BSONDocument("n" -> request.session.get("office").get), request)
      calmsetting <- ClaimSettingModel.findOne(BSONDocument(), request)
    } yield {
      if(calmsetting.get.dis){
        Ok(views.html.error.unauthorized())
      } else {
        val docnum = DocNumUtility.getNumberText("claim", request.session.get("entity").get)
        val wfcategories = maybe_wfcategories.map(wfcategories => wfcategories.cat)
        val currencies = maybe_currencies.map(currencies => currencies.ccyc)
        val defcurrency = maybe_currencies.filter(currency => currency.ct == (maybe_office.get.ct))
        val claim:Form[Claim] = claimform.fill(ClaimModel.doc.copy(
            docnum = docnum.toInt,
            p = PersonDetail(n=request.session.get("name").get, id=request.session.get("id").get),
            ed = ExpenseDetail(
                rdat=Some(new DateTime()), 
                cat="", 
                glc="", 
                amt=CurrencyAmount(ccy=defcurrency.head.ccyc, amt=0.0), 
                er=1.0, 
                aamt=CurrencyAmount(ccy=defcurrency.head.ccyc, amt=0.0), 
                gstamt=TaxDetail(cn="", crnum="", tnum="", tamt=CurrencyAmount(ccy=defcurrency.head.ccyc, amt=0.0)), 
                iamt=CurrencyAmount(ccy=defcurrency.head.ccyc, amt=0.0), 
                d=""
            )        
        ))
        Ok(views.html.claim.form(claim, wfcategories.sorted, currencies.sorted))
      }
    }
  } }
  
  def insert = withAuth { username => implicit request => {
	  claimform.bindFromRequest.fold(
	      formWithError => {
	        for {
	          maybe_wfcategories <- ClaimCategoryModel.find(BSONDocument(), request)
	          maybe_currencies <- ConfigCurrencyCodeModel.find(BSONDocument())
	        } yield{
	          val wfcategories = maybe_wfcategories.map(wfcategories => wfcategories.cat)
	          val currencies = maybe_currencies.map(currencies => currencies.ccyc)
	          Ok(views.html.claim.form(formWithError, wfcategories.sorted, currencies.sorted))
	        }
	      },
	      formWithData => {
	        for {
	          maybe_default_wf <- ClaimWorkflowModel.findOne(BSONDocument("d" -> true), request)
	          maybe_person_wf <- ClaimWorkflowModel.findOne(BSONDocument("app" -> BSONDocument("$in"->List(request.session.get("name").get + "@|@" + request.session.get("id").get))), request)
	          maybe_office_wf <- ClaimWorkflowModel.findOne(BSONDocument("app" -> BSONDocument("$in"->List(request.session.get("office").get))), request)
	          maybe_manager <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("managerid").get)))
	        } yield {
	          val wf = if(maybe_person_wf.isDefined){ maybe_person_wf.get } else if (maybe_office_wf.isDefined) { maybe_office_wf.get } else { maybe_default_wf.get }
	          val manager = maybe_manager.get
	          val smanager = if(request.session.get("smanagerid").get=="") { null } else {
	            val maybe_smanager = Await.result(PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("smanagerid").get))), Tools.db_timeout)
	            maybe_smanager.get
	          }
	          val doc_objectID = BSONObjectID.generate
	          
	          // Add Claim
	          val claim_update1 = formWithData.copy(
	              _id = doc_objectID,
	              ed = formWithData.ed.copy( 
	                  iamt = formWithData.ed.iamt.copy(
	                      amt = formWithData.ed.aamt.amt - formWithData.ed.gstamt.tamt.amt
	                  ) 
	              ),
	              wfs = ClaimFormWorkflowStatus(
	                  s1 = wf.s.s1,
	                  s2 = wf.s.s2,
	                  s3 = wf.s.s3,
	                  s4 = wf.s.s4,
	                  s5 = wf.s.s5,
	                  s6 = wf.s.s6,
	                  s7 = wf.s.s7,
	                  s8 = wf.s.s8,
	                  s9 = wf.s.s9,
	                  s10 = wf.s.s10
	              ),
	              wfat = ClaimFormWorkflowAssignTo(
	                  at1 = if(wf.cg.cg1 == 0 || wf.cg.cg1 < formWithData.ed.aamt.amt) {
	                    wf.at.at1 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at2.split("@|@").head, id=wf.at.at2.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },
	                  at2 = if(wf.cg.cg2 == 0 || wf.cg.cg2 < formWithData.ed.aamt.amt) {
	                    wf.at.at2 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at2.split("@|@").head, id=wf.at.at2.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },
	                  at3 = if(wf.cg.cg3 == 0 || wf.cg.cg3 < formWithData.ed.aamt.amt) {
	                    wf.at.at3 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at3.split("@|@").head, id=wf.at.at3.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },
	                  at4 = if(wf.cg.cg4 == 0 || wf.cg.cg4 < formWithData.ed.aamt.amt) {
	                    wf.at.at4 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at4.split("@|@").head, id=wf.at.at4.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },
	                  at5 = if(wf.cg.cg5 == 0 || wf.cg.cg5 < formWithData.ed.aamt.amt) {
	                    wf.at.at5 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at5.split("@|@").head, id=wf.at.at5.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  }, 
	                  at6 = if(wf.cg.cg6 == 0 || wf.cg.cg6 < formWithData.ed.aamt.amt) {
	                    wf.at.at6 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at6.split("@|@").head, id=wf.at.at6.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },  
	                  at7 = if(wf.cg.cg7 == 0 || wf.cg.cg7 < formWithData.ed.aamt.amt) {
	                    wf.at.at7 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at7.split("@|@").head, id=wf.at.at7.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },  
	                  at8 = if(wf.cg.cg8 == 0 || wf.cg.cg8 < formWithData.ed.aamt.amt) {
	                    wf.at.at8 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at8.split("@|@").head, id=wf.at.at8.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },  
	                  at9 = if(wf.cg.cg9 == 0 || wf.cg.cg9 < formWithData.ed.aamt.amt) {
	                    wf.at.at9 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at9.split("@|@").head, id=wf.at.at9.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  },
	                  at10 = if(wf.cg.cg10 == 0 || wf.cg.cg10 < formWithData.ed.aamt.amt) {
	                    wf.at.at10 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at10.split("@|@").head, id=wf.at.at10.split("@|@").last)
	                    }
	                  } else {
	                    PersonDetail(n="Not Applicable", id="")
	                  }
	              )
	          )
	          
	          val claim_update2 = if(claim_update1.wfat.at1.n=="Not Assigned" || claim_update1.wfat.at1.n=="Not Applicable"){
	            ClaimModel.approve(	     
	                claim_update1.copy(  
	                    wf = claim_update1.wf.copy(   
	                        papr=PersonDetail(n="", id=""),   
	                        s = "Submitted",   
	                        wfs="0"
	                    )
	                ), request  
	            )
	          } else {
	            claim_update1.copy( 
	                wf = claim_update1.wf.copy(  
	                    papr=PersonDetail(n=claim_update1.wfat.at1.n, id=claim_update1.wfat.at1.id), 
	                    s = "Submitted",
	                    wfs="0",
	                    aprid = List(claim_update1.wfat.at1.id)
	                )
	            )
	          }
	         
	          ClaimModel.insert(claim_update2, p_request=request)
	          
	          // Add ToDo
	          if(claim_update2.wf.papr.id!=""){
	            val contentMap = Map( 
	                "DOCUNUM"->claim_update2.docnum.toString(), 
	                "APPLICANT"->claim_update2.p.n, 
	                "AMT"->(claim_update2.ed.aamt.ccy + " " + claim_update2.ed.aamt.amt), 
	                "CAT"->claim_update2.ed.cat, 
	                "DAT"->(claim_update2.ed.rdat.get.dayOfMonth().getAsText + "-" + claim_update2.ed.rdat.get.monthOfYear().getAsShortText + "-" + claim_update2.ed.rdat.get.getYear.toString())
	            )
	            val buttonMap = Map(
	                "APPROVELINK"->(Tools.hostname + "/claim/approve/" + claim_update2._id.stringify), 
	                "DOCLINK"->(Tools.hostname + "/claim/view/" + claim_update2._id.stringify)    
	            )
	            TaskModel.insert(8, claim_update2.wf.papr.id, claim_update2._id.stringify, contentMap, buttonMap, "", request)
	          }
	        
	          // Send Email

	          // Insert Audit Log 
	          AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Submit claim request."),p_request=request)
	          
	          Redirect(routes.DashboardController.index)
	        }
	      }
	  )
  } }
  
  def view(p_id:String) = withAuth { username => implicit request => {
	  for {
	    maybeclaim <- ClaimModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
	  } yield {
	    maybeclaim.map( claim => {
        
        // Viewable by admin, approvers and applicant
        if (claim.p.id == request.session.get("id").get || claim.wf.aprid.contains(request.session.get("id").get) || hasRoles(List("Admin"), request)) {
          Ok(views.html.claim.view(claim))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
  } }
  
  def approve(p_id:String, p_path:String, p_msg:String) = withAuth { username => implicit request => {
    for {
      maybeclaim <- ClaimModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybeclaim.map( claim => {
        
	    // Check authorized
	    if (claim.wf.papr.id==request.session.get("id").get) {
	      
	      val claim_update = ClaimModel.approve(claim, request)
	      ClaimModel.update(BSONDocument("_id" -> claim._id), claim_update, request)
        
	      // Update ToDo
	      Await.result(TaskModel.setCompleted(request.session.get("id").get, claim_update._id.stringify, request), Tools.db_timeout)
	      
	      // Add ToDo
	      if(claim_update.wf.papr.id!=""){    
	        val contentMap = Map( 
	            "DOCUNUM"->claim_update.docnum.toString(), 
	            "APPLICANT"->claim_update.p.n, 
	            "AMT"->(claim_update.ed.aamt.ccy + " " + claim_update.ed.aamt.amt),    
	            "CAT"->claim_update.ed.cat,  
	            "DAT"->(claim_update.ed.rdat.get.dayOfMonth().getAsText + "-" + claim_update.ed.rdat.get.monthOfYear().getAsShortText + "-" + claim_update.ed.rdat.get.getYear.toString())  
	        )
	            
	        val buttonMap = Map(   
	            "APPROVELINK"->(Tools.hostname + "/claim/approve/" + claim_update._id.stringify),  
	            "DOCLINK"->(Tools.hostname + "/claim/view/" + claim_update._id.stringify)    
	        )
	        TaskModel.insert(8, claim_update.wf.papr.id, claim_update._id.stringify, contentMap, buttonMap, "", request)  
	      }
	        
	      // Send Email
	      
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Approve claim request."), p_request=request)

        if (p_path!="") {
          Redirect(p_path).flashing("success" -> p_msg)
        } else {
          Redirect(request.session.get("path").get).flashing("success" -> p_msg)
        }
	    } else {
	      Ok(views.html.error.unauthorized())
	    }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  } }
  
  def reject(p_id:String, p_path:String, p_msg:String) = withAuth { username => implicit request => {
    for {
      maybeclaim <- ClaimModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybeclaim.map( claim => {
        
	    // Check authorized
	    if (claim.wf.papr.id==request.session.get("id").get) {
	      val claim_update = ClaimModel.reject(claim, request)
	      ClaimModel.update(BSONDocument("_id" -> claim._id), claim_update, request)
        
	      // Update Todo
        Await.result(TaskModel.setCompletedMulti(claim_update._id.stringify, request), Tools.db_timeout)
        
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Reject claim request."), p_request=request)
        
        if (p_path!="") {
          Redirect(p_path).flashing("success" -> p_msg)
        } else {
          Redirect(request.session.get("path").get).flashing("success" -> p_msg)
        }
	    } else {
	      Ok(views.html.error.unauthorized())
	    }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  } }
  
  def cancel(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeclaim <- ClaimModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybeclaim.map( claim => {
        
	    // Check authorized
	    if (claim.p.id==request.session.get("id").get) {
	      val claim_update = ClaimModel.cancel(claim, request)
	      ClaimModel.update(BSONDocument("_id" -> claim._id), claim_update, request)
	      
	      // Update Todo
        Await.result(TaskModel.setCompletedMulti(claim_update._id.stringify, request), Tools.db_timeout)
        
        // Insert audit log
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Cancel claim request."), p_request=request)
        
        Redirect(request.session.get("path").get)
	    } else {
	      Ok(views.html.error.unauthorized())
	    }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  } }
  
}