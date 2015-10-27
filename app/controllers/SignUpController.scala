package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._
import utilities.{SystemDataStore, System, DocNumUtility, MailUtility, Tools}

import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONArray}

import scala.util.Random
import scala.concurrent.{Future,Await}
import org.joda.time.DateTime

case class Signup (
    fname: String,
    lname: String,
    email: String,
    gender: String,
    marital: String,
    company: String  
)

object SignUpController extends Controller {
  
  val signupform = Form(
      mapping(
          "fname" -> nonEmptyText,
          "lname" -> nonEmptyText,
          "email" -> nonEmptyText,
          "gender" -> nonEmptyText,
          "marital" -> nonEmptyText,
          "company" -> nonEmptyText
      )((fname,lname,email,gender,marital,company) => Signup(fname,lname,email.toLowerCase().trim(),gender,marital,company))
      (Signup.unapply)
  )
  
  def create = Action { request =>
    Ok(views.html.signup.create(signupform))
  }
  
  def insert = Action.async { implicit request => {
    signupform.bindFromRequest.fold(
        formWithErrors => Future.successful(Ok(views.html.signup.create(formWithErrors))),
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
              ConfigLeavePolicyModel.find(BSONDocument()).map( configleavepolicies => 
                configleavepolicies.map( configleavepolicy => {
                  LeavePolicyModel.insert(
                      LeavePolicy(
                          BSONObjectID.generate,
                          configleavepolicy.lt,
                          configleavepolicy.pt,
                          LeavePolicySetting(
                              configleavepolicy.set.g,
                              configleavepolicy.set.acc,
                              configleavepolicy.set.ms,
                              configleavepolicy.set.dt,
                              configleavepolicy.set.nwd,
                              configleavepolicy.set.cexp,
                              configleavepolicy.set.scal
                          ),
                          Entitlement(
                              configleavepolicy.ent.e1,
                              configleavepolicy.ent.e1_s,
                              configleavepolicy.ent.e1_cf,
                              configleavepolicy.ent.e2,
                              configleavepolicy.ent.e2_s,
                              configleavepolicy.ent.e2_cf,
                              configleavepolicy.ent.e3,
                              configleavepolicy.ent.e3_s,
                              configleavepolicy.ent.e3_cf,
                              configleavepolicy.ent.e4,
                              configleavepolicy.ent.e4_s,
                              configleavepolicy.ent.e4_cf,
                              configleavepolicy.ent.e5,
                              configleavepolicy.ent.e5_s,
                              configleavepolicy.ent.e5_cf
                          ),
                          configleavepolicy.sys 
                      ), 
                      eid
                  )
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
                  ct = "Malaysia"
              ) 
              CompanyModel.insert(company_doc, eid)
              
              // Create Office record
              val office_doc = OfficeModel.doc.copy(
                  _id = BSONObjectID.generate,
                  n = "Main Office",
                  ct = "Malaysia",
                  st = "Kuala Lumpur"
              )
              OfficeModel.insert(office_doc, eid)
              
              // Create keyword record
              ConfigKeywordModel.find(BSONDocument()).map( configkeywords => 
                configkeywords.map( configkeyword => 
                  KeywordModel.insert(
                      Keyword(_id = BSONObjectID.generate, n = configkeyword.n, v = configkeyword.v, s = configkeyword.s, sys = None),
                      eid              
                  ) 
                )
              )
                        
              // Create person record          
              val person_doc = PersonModel.doc.copy(
                  _id = person_objectID,
                  p = Profile(
                      fn = formWithData.fname, 
                      ln = formWithData.lname,
                      em = formWithData.email,
                      nem = false,
                      pt = "Manager",
                      mgrid = person_objectID.stringify,
                      g = formWithData.gender,
                      ms = formWithData.marital,
                      dpm = "Information Technology",
                      off = "Main Office",
                      edat = Some(new DateTime()),
                      rl=List("Admin")
                  )
              )
              PersonModel.insertOnNewSignUp(person_doc, eid)
     
              val contentEmptyMap = Map(""->"")
              
              // TODO Step 6: Add company staff
              val lookupkey6 = (BSONObjectID.generate).stringify
              val buttonMap6 = Map(
                  "ADDLINK"->{routes.PersonController.create.toString}, 
                  "SETTINGLINK"->{routes.PersonController.index.toString}, 
                  "DISMISSLINK"->{"javascript:dismissTask(\"" + lookupkey6 + "\")"}    
              )
              Await.result(TaskModel.insert(7, person_objectID.stringify, lookupkey6, contentEmptyMap, buttonMap6, eid), Tools.db_timeout)
              
              // TODO Step 5: Add company holiday
              val lookupkey5 = (BSONObjectID.generate).stringify
              val buttonMap5 = Map(
                  "ADDLINK"->{routes.CompanyHolidayController.create.toString}, 
                  "SETTINGLINK"->{routes.CalendarController.company.toString}, 
                  "DISMISSLINK"->{"javascript:dismissTask(\"" + lookupkey5 + "\")"}    
              )
              Await.result(TaskModel.insert(6, person_objectID.stringify, lookupkey5, contentEmptyMap, buttonMap5, eid), Tools.db_timeout)
              
              // TODO Step 4: Update your person information and leave profiles
              val lookupkey4 = (BSONObjectID.generate).stringify
              val buttonMap4 = Map(
                  "SETTINGLINK"->{routes.PersonController.view(person_objectID.stringify).toString}, 
                  "DISMISSLINK"->{"javascript:dismissTask(\"" + lookupkey4 + "\")"}    
              )
              Await.result(TaskModel.insert(5, person_objectID.stringify, lookupkey4, contentEmptyMap, buttonMap4, eid), Tools.db_timeout)
    
              // TODO Step 3: Configure leave policy
              val lookupkey3 = (BSONObjectID.generate).stringify
              val buttonMap3 = Map(
                  "SETTINGLINK"->{routes.LeaveSettingController.index.toString}, 
                  "DISMISSLINK"->{"javascript:dismissTask(\"" + lookupkey3 + "\")"}    
              )
              Await.result(TaskModel.insert(4, person_objectID.stringify, lookupkey3, contentEmptyMap, buttonMap3, eid), Tools.db_timeout)
              
              // TODO Step 2: Update keywords for department, leave types and position types
              val lookupkey2 = (BSONObjectID.generate).stringify
              val buttonMap2 = Map(
                  "SETTINGLINK"->{routes.KeywordController.index.toString}, 
                  "DISMISSLINK"->{"javascript:dismissTask(\"" + lookupkey2 + "\")"}    
              )
              Await.result(TaskModel.insert(3, person_objectID.stringify, lookupkey2, contentEmptyMap, buttonMap2, eid), Tools.db_timeout)
              
              // TODO Step 1: Update company profile
              val lookupkey1 = (BSONObjectID.generate).stringify
              val buttonMap1 = Map(
                  "SETTINGLINK"->{routes.CompanyController.index.toString}, 
                  "DISMISSLINK"->{"javascript:dismissTask(\"" + lookupkey1 + "\")"}    
              )
              Await.result(TaskModel.insert(2, person_objectID.stringify, lookupkey1, contentEmptyMap, buttonMap1, eid), Tools.db_timeout)
                    
              // Send email
              val replaceMap = Map("URL"->(Tools.hostname+"/set/"+authentication_doc.em +"/"+authentication_doc.r), "BY"->(person_doc.p.fn+" "+person_doc.p.ln))
              MailUtility.sendEmailConfig(List(authentication_doc.em), 1, replaceMap)
              MailUtility.sendEmail(List("support@hrsifu.my"), "System Notification: New Sign Up - " + formWithData.company + ".", formWithData.company + "(" + eid +  ") sign up by " + formWithData.email + ".")
                
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