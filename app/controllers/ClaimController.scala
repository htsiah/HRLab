package controllers

import scala.concurrent.{Future,Await}

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._

import models.{ClaimModel, Claim, ExpenseDetail, TaxDetail, ClaimFormWorkflow, ClaimFormWorkflowStatus, ClaimFormWorkflowAssignTo, ClaimFormWorkflowAction, ClaimFormWorkflowActionDate, PersonDetail, CurrencyAmount, PersonModel, OfficeModel, TaskModel, AuditLogModel}
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
          "ed" -> mapping(
              "rdat" -> optional(jodaDate("d-MMM-yyyy")),
              "cat" -> text,
              "glc" -> text,
              "amt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply),
              "er" -> of[Double],
              "aamt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply),
              "gstamt" -> mapping("cn" -> text, "crnum" -> text, "tnum" -> text, "tamt" -> of[Double])(TaxDetail.apply)(TaxDetail.unapply),
              "iamt" -> mapping("ccy" -> text, "amt" -> of[Double])(CurrencyAmount.apply)(CurrencyAmount.unapply),
              "d" -> text
          )(ExpenseDetail.apply)(ExpenseDetail.unapply),
          "wf" -> mapping(
              "paprn" -> mapping("n" -> text, "id" -> text)(PersonDetail.apply)(PersonDetail.unapply),
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
      ){(_id,docnum,ed,wf,wfs,wfat,wfa,wdadat,sys)=>Claim(_id,docnum,ed,wf,wfs,wfat,wfa,wdadat,sys)}
      {claim:Claim=>Some(claim._id, claim.docnum, claim.ed, claim.wf, claim.wfs, claim.wfat, claim.wfa, claim.wdadat, claim.sys)}
  )
  
  def create = TODO
  
  def insert = TODO
  
  def view(p_id:String) = TODO
  
  def approve(p_id:String, p_path:String, p_msg:String) = TODO
  
  def reject(p_id:String, p_path:String, p_msg:String) = TODO
  
  def cancel(p_id:String) = TODO

}