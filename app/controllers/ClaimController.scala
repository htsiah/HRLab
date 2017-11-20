package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimWorkflowModel, ConfigCurrencyCodeModel, ClaimModel, Claim, ExpenseDetail, TaxDetail, ClaimFormWorkflow, ClaimFormWorkflowStatus, ClaimFormWorkflowAssignTo, ClaimFormWorkflowAction, ClaimFormWorkflowActionDate, PersonDetail, CurrencyAmount, ClaimCategoryModel, PersonModel, OfficeModel, TaskModel, AuditLogModel}
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
              "s" -> text
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
    } yield {
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
	          val claim_update = formWithData.copy(
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
	                  at1 = wf.at.at1 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at2 = wf.at.at2 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at3 = wf.at.at3 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at4 = wf.at.at4 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at5 = wf.at.at5 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at6 = wf.at.at6 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at7 = wf.at.at7 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at8 = wf.at.at8 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at9 = wf.at.at9 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  },
	                  at10 = wf.at.at10 match {
	                    case "[Employee’s Manager]" => PersonDetail( n=manager.p.fn + " " + manager.p.ln, id=manager._id.stringify)
	                    case "[Employee’s Substitute Manager]" => if(smanager == null) { PersonDetail(n="Not Assigned", id="") } else { PersonDetail(n=smanager.p.fn + " " + smanager.p.ln, id=smanager._id.stringify) }
	                    case _ => PersonDetail( n=wf.at.at1.split("@|@").head, id=wf.at.at1.split("@|@").last)
	                  }
	              )
	          )
	          ClaimModel.insert(claim_update, p_request=request)
	          
	        
	          // Add ToDo
	        
	          // Send email

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
        
        // Viewable by admin, manager, substitute manager and applicant
        if (claim.p.id == request.session.get("id").get || PersonModel.isManagerFor(claim.p.id, request.session.get("id").get, request) || PersonModel.isSubstituteManagerFor(claim.p.id, request.session.get("id").get, request) || hasRoles(List("Admin"), request)) {     
          Ok(views.html.claim.view(claim))
        } else {
          Ok(views.html.error.unauthorized())
        }

      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
	  }
  } }
  
  def approve(p_id:String, p_path:String, p_msg:String) = TODO
  
  def reject(p_id:String, p_path:String, p_msg:String) = TODO
  
  def cancel(p_id:String) = TODO

}