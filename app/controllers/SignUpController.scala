package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._
import utilities.{SystemDataStore, System, DocNumUtility, MailUtility, Tools, KeywordUtility}

import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONArray}

import scala.util.Random
import scala.concurrent.{Future,Await}

import org.joda.time.DateTime

import javax.inject.Inject

case class Signup (
    fname: String,
    lname: String,
    email: String,
    gender: String,
    marital: String,
    company: String,
    country: String
)

class SignUpController @Inject() (mailerClient: MailerClient) extends Controller {
  
  val signupform = Form(
      mapping(
          "fname" -> nonEmptyText,
          "lname" -> nonEmptyText,
          "email" -> nonEmptyText,
          "gender" -> nonEmptyText,
          "marital" -> nonEmptyText,
          "company" -> nonEmptyText,
          "country" -> nonEmptyText
      )((fname,lname,email,gender,marital,company,country) => Signup(fname,lname,email.toLowerCase().trim(),gender,marital,company,country))
      (Signup.unapply)
  )
  
  def create = Action.async { request =>
    for {
      maybe_kw_countries <- KeywordUtility.findOne(BSONDocument("n" -> "Countries"))
    } yield {
      val countries = maybe_kw_countries.get.v.get.sorted
      Ok(views.html.signup.create(signupform, countries))
    }
  }
  
  def insert = Action.async { implicit request => {
    signupform.bindFromRequest.fold(
        formWithErrors => {
          for {
            maybe_countriesKeyword <- KeywordUtility.findOne(BSONDocument("n" -> "Countries"))
          } yield {
            val countries = maybe_countriesKeyword.get.v.get
            Ok(views.html.signup.create(formWithErrors, countries))
          }
        },
        formWithData => {
          
          AuthenticationModel.findOneByEmail(formWithData.email.toLowerCase()).map( email => {
            if (email.isDefined){

              // Do checking on email.
              // Why still need backend check when we have frontend jquery validation? 
              // 1. Encountered some hack using same email to signup by skip frontend check. 
              // 2. Ms-edge does not support frontend jquery validation on email.
              val msg = "Someone already used " + formWithData.email.toLowerCase() + ". Please try another email."
              Redirect(routes.AuthenticationController.login()).flashing(
                  "error" -> msg
              )
              
            } else {
              
              val eid = DocNumUtility.getNumberText("entity")
              val person_objectID = BSONObjectID.generate
              
              // Create leave policy
              ConfigLeavePolicyModel.find(BSONDocument("ct" -> formWithData.country)).map( configleavepolicies => 
                configleavepolicies.map( configleavepolicy => {
                  val leavepolicy_objectID = BSONObjectID.generate
                  LeavePolicyModel.insert(
                      LeavePolicy(
                          leavepolicy_objectID,
                          configleavepolicy.lt,
                          LeavePolicySetting(
                              configleavepolicy.set.g,
                              configleavepolicy.set.acc,
                              configleavepolicy.set.ms,
                              configleavepolicy.set.dt,
                              configleavepolicy.set.nwd,
                              configleavepolicy.set.cexp,
                              configleavepolicy.set.scal,
                              configleavepolicy.set.msd
                          ),
                          Entitlement(
                              EntitlementValue(configleavepolicy.ent.e1.e, configleavepolicy.ent.e1.s, configleavepolicy.ent.e1.cf),
                              EntitlementValue(configleavepolicy.ent.e2.e, configleavepolicy.ent.e2.s, configleavepolicy.ent.e2.cf),
                              EntitlementValue(configleavepolicy.ent.e3.e, configleavepolicy.ent.e3.s, configleavepolicy.ent.e3.cf),
                              EntitlementValue(configleavepolicy.ent.e4.e, configleavepolicy.ent.e4.s, configleavepolicy.ent.e4.cf),
                              EntitlementValue(configleavepolicy.ent.e5.e, configleavepolicy.ent.e5.s, configleavepolicy.ent.e5.cf),
                              EntitlementValue(configleavepolicy.ent.e6.e, configleavepolicy.ent.e6.s, configleavepolicy.ent.e6.cf),
                              EntitlementValue(configleavepolicy.ent.e7.e, configleavepolicy.ent.e7.s, configleavepolicy.ent.e7.cf),
                              EntitlementValue(configleavepolicy.ent.e8.e, configleavepolicy.ent.e8.s, configleavepolicy.ent.e8.cf),
                              EntitlementValue(configleavepolicy.ent.e9.e, configleavepolicy.ent.e9.s, configleavepolicy.ent.e9.cf),
                              EntitlementValue(configleavepolicy.ent.e10.e, configleavepolicy.ent.e10.s, configleavepolicy.ent.e10.cf)
                          ),
                          configleavepolicy.sys
                      ), 
                      eid
                  )
                  AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=leavepolicy_objectID.stringify, c="Create document."), p_eid=eid)
                })
              )
              
              // Create Leave Setting
              val leavesetting_doc = LeaveSettingModel.doc.copy(
                  _id = BSONObjectID.generate,
                  cflr = Some(new DateTime())
              )
              LeaveSettingModel.insert(leavesetting_doc, eid)
              
              // Create logon record
              val authentication_doc = AuthenticationModel.doc.copy(
                  _id = BSONObjectID.generate,
                  em = formWithData.email,
                  p = Random.alphanumeric.take(8).mkString,
                  r = Random.alphanumeric.take(8).mkString
              )
              AuthenticationModel.insert(authentication_doc,eid)
              
              // Create company record
              val company_doc = CompanyModel.doc.copy(
                  _id = BSONObjectID.generate,
                  c = formWithData.company,
                  ct = formWithData.country
              ) 
              CompanyModel.insert(company_doc, eid)
              
              
              // Create Claim Category
              ConfigClaimCategoryModel.find(BSONDocument()).map( configclaimcategories => 
                configclaimcategories.map( configclaimcategory => {
                  val claimcategory_objectID = BSONObjectID.generate
                  ClaimCategoryModel.insert(
                      ClaimCategory(
                          _id=claimcategory_objectID, 
                          cat=configclaimcategory.cat, 
                          all=configclaimcategory.all, 
                          app=configclaimcategory.app, 
                          tlim=configclaimcategory.tlim, 
                          hlp=configclaimcategory.hlp, 
                          sys = None
                      ),
                      eid              
                  )
                  AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=claimcategory_objectID.stringify, c="Create document."), p_eid=eid)
                })
              )
              
              // Create Claim Workflow
              ConfigClaimWorkflowModel.find(BSONDocument()).map ( configclaimworkflows =>
                configclaimworkflows.map ( configclaimworkflow => { 
                  val claimworkflow_objectID = BSONObjectID.generate
                  ClaimWorkflowModel.insert(
                      ClaimWorkflow(
                          _id = claimworkflow_objectID,
                          n = configclaimworkflow.n,
                          d = configclaimworkflow.d,
                          app = configclaimworkflow.app,
                          s = ClaimWorkflowStatus(s1=configclaimworkflow.s.s1, s2=configclaimworkflow.s.s2, s3=configclaimworkflow.s.s3, s4=configclaimworkflow.s.s4, s5=configclaimworkflow.s.s5, s6=configclaimworkflow.s.s6, s7=configclaimworkflow.s.s7, s8=configclaimworkflow.s.s8, s9=configclaimworkflow.s.s9, s10=configclaimworkflow.s.s10),
                          at = ClaimWorkflowAssigned(at1=configclaimworkflow.at.at1, at2=configclaimworkflow.at.at2, at3=configclaimworkflow.at.at3, at4=configclaimworkflow.at.at4, at5=configclaimworkflow.at.at5, at6=configclaimworkflow.at.at6, at7=configclaimworkflow.at.at7, at8=configclaimworkflow.at.at8, at9=configclaimworkflow.at.at9, at10=configclaimworkflow.at.at10),
                          caa = ClaimWorkflowApprovedAmount(caa1=configclaimworkflow.caa.caa1, caa2=configclaimworkflow.caa.caa2, caa3=configclaimworkflow.caa.caa3, caa4=configclaimworkflow.caa.caa4, caa5=configclaimworkflow.caa.caa5, caa6=configclaimworkflow.caa.caa6, caa7=configclaimworkflow.caa.caa7, caa8=configclaimworkflow.caa.caa8, caa9=configclaimworkflow.caa.caa9, caa10=configclaimworkflow.caa.caa10),
                          cg = ClaimWorkflowAmountGreater(cg1=configclaimworkflow.cg.cg1, cg2=configclaimworkflow.cg.cg2, cg3=configclaimworkflow.cg.cg3, cg4=configclaimworkflow.cg.cg4, cg5=configclaimworkflow.cg.cg5, cg6=configclaimworkflow.cg.cg6, cg7=configclaimworkflow.cg.cg7, cg8=configclaimworkflow.cg.cg8, cg9=configclaimworkflow.cg.cg9, cg10=configclaimworkflow.cg.cg10),
                          sys = None
                      ),
                      eid
                  )
                  AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=claimworkflow_objectID.stringify, c="Create document."), p_eid=eid)
                })
              )
              
              // Create Office record
              val office_objectID = BSONObjectID.generate
              val office_doc = OfficeModel.doc.copy(
                  _id = office_objectID,
                  n = "Main Office",
                  ct = formWithData.country
              )
              OfficeModel.insert(office_doc, eid)
              AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=office_objectID.stringify, c="Create document."), p_eid=eid)
              
              // Create keyword record
              ConfigKeywordModel.find(BSONDocument()).map( configkeywords => 
                configkeywords.map( configkeyword => {
                  val keyword_objectID = BSONObjectID.generate
                  KeywordModel.insert(
                      Keyword(_id = keyword_objectID, n = configkeyword.n, v = configkeyword.v, s = configkeyword.s, sys = None),
                      eid              
                  )
                  AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=keyword_objectID.stringify, c="Create document."), p_eid=eid)
                })
              )
                        
              // Create person record          
              val person_doc = PersonModel.doc.copy(
                  _id = person_objectID,
                  p = Profile(
                      empid = "",
                      fn = formWithData.fname, 
                      ln = formWithData.lname,
                      em = formWithData.email,
                      nem = false,
                      pt = "",
                      mgrid = person_objectID.stringify,
                      smgrid = "",
                      g = formWithData.gender,
                      ms = formWithData.marital,
                      dpm = "",
                      off = "Main Office",
                      edat = Some(new DateTime(DateTime.now().getYear, 1, 1, 0, 0, 0, 0)),
                      rl=List("Admin")
                  )
              )
              PersonModel.insertOnNewSignUp(person_doc, formWithData.country, eid)
              AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=person_objectID.stringify, c="Create document."), p_eid=eid)
                    
              // Create Org Chart Setting
              val orgchartsetting_objectID = BSONObjectID.generate
              val orgchartseting_doc = OrgChartSettingModel.doc.copy(
                  _id = office_objectID
              )
              OrgChartSettingModel.insert(orgchartseting_doc, eid)
              
              // Send email
              val replaceMap = Map("URL"->(Tools.hostname+"/set/"+authentication_doc.em +"/"+authentication_doc.r), "BY"->(person_doc.p.fn+" "+person_doc.p.ln))
              MailUtility.getEmailConfig(List(authentication_doc.em), 1, replaceMap).map { email => mailerClient.send(email) }
              mailerClient.send(
                  MailUtility.getEmail(List("support@hrsifu.com"), "System Notification: New Sign Up - " + formWithData.company + ".", formWithData.company + "(" + eid +  ") from " + formWithData.country + " signed up by " + formWithData.email + ".")
              )
                
              Redirect(routes.AuthenticationController.login()).flashing(
                  "success" -> "Your registration was successful. An email with your logon detail has been sent."
              )

            }
          })
          
        }     
    )    
  }}
    
  def checkemail(p_email:String) = Action.async { implicit request =>
    AuthenticationModel.findOneByEmail(p_email.toLowerCase()).map( email => {
      email.isDefined match {
        case true => Ok("false").as("text/plain")
        case _ =>Ok("true").as("text/plain")
      }
    })
  }
  
}