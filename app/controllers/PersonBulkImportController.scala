package controllers

import play.api.mvc._
import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}
import play.api.libs.json.{ Json }
import play.modules.reactivemongo.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.libs.mailer._
import com.github.tototoshi.csv._

import scala.concurrent.{Future, Await}
import scala.util.{Random}

import javax.inject.Inject
import java.io.{FileInputStream}

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.api.gridfs.{ ReadFile, DefaultFileToSave }
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import models.{PersonBulkImportModel, PersonModel, AuthenticationModel, Authentication, AuditLogModel, KeywordModel}
import utilities.{DataValidationUtility, Tools, MailUtility}

class PersonBulkImportController @Inject() (mailerClient: MailerClient, val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with ReactiveMongoComponents with Secured {

  // type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Future.successful(Ok(views.html.personbulkimport.form()))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))    
    }
  }}
  
  def insert = withAuth (parse.maxLength(1 * 1014 * 1024, parse.multipartFormData)) { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      
      request.body match {
        case Left(MaxSizeExceeded(length)) => {     
          // Return error
          Future(Ok(Json.obj("status" -> "exceed file size limit")).as("application/json"))
        }
        case Right(multipartForm) => {
          val csv = multipartForm.file("csv").get       
          val metadata = BSONDocument(
              "eid" -> request.session.get("entity"),
              "filename" -> csv.filename, 
              "lk" -> "", 
              "f" -> "personbulkimport", 
              "cby" -> request.session.get("username")
          )

          // Upload with metadata
          val futureFile: Future[ReadFile[BSONSerializationPack.type, BSONValue]] = PersonBulkImportModel.gridFSBSON.writeFromInputStream(DefaultFileToSave(csv.filename, csv.contentType, metadata = metadata), new FileInputStream(csv.ref.file))
            
          // Return error on file upload
          futureFile.onFailure {
            case err => {
              err.printStackTrace();
              Ok(Json.obj("status" -> "error import employee")).as("application/json")
            }            
          }
                    
          // Initial import csv to CSVReader
          val reader = CSVReader.open(csv.ref.file)            
          val importrawdata = reader.allWithHeaders()
          
          // Validation - Only 50 Per Import.
          if (importrawdata.length > 50) {
            Future.successful(Ok(Json.obj("status" -> "exceed 50 employee")).as("application/json"))
          } else {
            
            // Generate import employee id list
            val empidlist = importrawdata.map{ importrawrow => if( importrawrow.contains("Employee ID")) { importrawrow("Employee ID").trim } }.filter( value => value != "" )
            val empemaillist = importrawdata.map{ importrawrow => if( importrawrow.contains("Email")) { importrawrow("Email").trim } }.filter( value => value != "" )
                        
            // Import data validation
            val importdata = importrawdata.map{ importrawrow => {
              println(importrawrow)
              
              // Import values
              val EmployeeID = if( importrawrow.contains("Employee ID")) { importrawrow("Employee ID").trim } else { "" }
              val FirstName = if( importrawrow.contains("First Name (M)")) { importrawrow("First Name (M)").trim } else { "" }
              val LastName = if( importrawrow.contains("Last Name (M)")) { importrawrow("Last Name (M)").trim } else { "" }
              val Email = if( importrawrow.contains("Email")) { importrawrow("Email").trim } else { "" }
              val Position = if( importrawrow.contains("Position")) { importrawrow("Position").trim } else { "" }
              val Manager = if( importrawrow.contains("Manager")) { importrawrow("Manager").trim } else { "" }
              val SubstituteManager = if( importrawrow.contains("Substitute Manager")) { importrawrow("Substitute Manager").trim } else { "" }
              val Gender = if( importrawrow.contains("Gender (M)")) { importrawrow("Gender (M)").trim } else { "" }
              val MaritalStatus = if( importrawrow.contains("Marital Status (M)")) { importrawrow("Marital Status (M)").trim } else { "" }
              val Department = if( importrawrow.contains("Department")) { importrawrow("Department").trim } else { "" }
              val Office = if( importrawrow.contains("Office (M)")) { importrawrow("Office (M)").trim } else { "" }
              val JoinDate = if( importrawrow.contains("Join Date")) { importrawrow("Join Date").trim } else { "" }
              val WorkDayMonday = if( importrawrow.contains("Work Day - Monday")) { importrawrow("Work Day - Monday").toLowerCase().trim } else { "" }
              val WorkDayTuesday = if( importrawrow.contains("Work Day - Tuesday")) { importrawrow("Work Day - Tuesday").toLowerCase().trim } else { "" }
              val WorkDayWebnesday = if( importrawrow.contains("Work Day - Webnesday")) { importrawrow("Work Day - Webnesday").toLowerCase().trim } else { "" }
              val WorkDayThursday = if( importrawrow.contains("Work Day - Thursday")) { importrawrow("Work Day - Thursday").toLowerCase().trim } else { "" }
              val WorkDayFriday = if( importrawrow.contains("Work Day - Friday")) { importrawrow("Work Day - Friday").toLowerCase().trim } else { "" }
              val WorkDaySaturday = if( importrawrow.contains("Work Day - Saturday")) { importrawrow("Work Day - Saturday").toLowerCase().trim } else { "" }
              val WorkDaySunday = if( importrawrow.contains("Work Day - Sunday")) { importrawrow("Work Day - Sunday").toLowerCase().trim } else { "" }
              val Admin = if( importrawrow.contains("Admin")) { importrawrow("Admin").toLowerCase().trim } else { "" }
              val SendWelcomeEmail = if( importrawrow.contains("Send Welcome Email")) { importrawrow("Send Welcome Email").toLowerCase().trim } else { "" }

              val validation = valdata(
                  EmployeeID, 
                  FirstName, 
                  LastName, 
                  Email, 
                  Position, 
                  Manager, 
                  SubstituteManager, 
                  Gender, 
                  MaritalStatus, 
                  Department, 
                  Office, 
                  JoinDate, 
                  WorkDayMonday, 
                  WorkDayTuesday, 
                  WorkDayWebnesday, 
                  WorkDayThursday, 
                  WorkDayFriday,
                  WorkDaySaturday,
                  WorkDaySunday,
                  Admin,
                  SendWelcomeEmail,
                  empidlist,
                  empemaillist,
                  request
              )
              
              // Output result
              List(
                  EmployeeID, 
                  FirstName, 
                  LastName, 
                  Email, 
                  Position, 
                  Manager, 
                  SubstituteManager, 
                  Gender, 
                  MaritalStatus, 
                  Department, 
                  Office, 
                  JoinDate, 
                  WorkDayMonday, 
                  WorkDayTuesday, 
                  WorkDayWebnesday, 
                  WorkDayThursday, 
                  WorkDayFriday,
                  WorkDaySaturday,
                  WorkDaySunday,
                  Admin,
                  SendWelcomeEmail,
                  validation("status"),
                  validation("msg")
              )

            } }
            
            // Create document
            val createdresult = importdata.filter( value => value(21) != "fail" ).map { newemployee => {
              println (newemployee(1))
              
              val dtf = DateTimeFormat.forPattern("d-MMM-yyyy")              
              
              if (newemployee(3) != "") {
                
                // Create Authentication document
                val authentication_doc = Authentication(
                    _id = BSONObjectID.generate,
                    em = newemployee(3),
                    p = Random.alphanumeric.take(8).mkString,
                    r = Random.alphanumeric.take(8).mkString,
                    sys = None
                )
                AuthenticationModel.insert(authentication_doc, p_request=request)
                
                if (newemployee(20) == "yes") {
                  
                  // Send email
                  val replaceMap = Map(
                      "FULLNAME" -> {newemployee(1) + " " + newemployee(2)},
                      "ADMIN" -> request.session.get("name").get,
                      "COMPANY" -> request.session.get("company").get,
                      "URL" -> {Tools.hostname + "/set/" + authentication_doc.em  + "/" + authentication_doc.r}
                  )
                  MailUtility.getEmailConfig(List(authentication_doc.em), 7, replaceMap).map { email => mailerClient.send(email) }
                
                }
                
              }
                
              // Create Person Document
              val person_doc = PersonModel.doc.copy(
                  _id=BSONObjectID(newemployee(22)),
                  p = PersonModel.doc.p.copy(
                      empid = newemployee(0),
                      fn = newemployee(1),
                      ln = newemployee(2),
                      em = newemployee(3),
                      nem = if ( newemployee(3) == "" ) { true } else { false },
                      pt = newemployee(4),
                      mgrid = getManagerID(newemployee(5), importdata, request),
                      smgrid = getManagerID(newemployee(6), importdata, request),
                      g = newemployee(7),
                      ms = newemployee(8),
                      dpm = newemployee(9),
                      off = newemployee(10),
                      edat = if (newemployee(11) == "") { Some(new DateTime(DateTime.now().getYear, 1, 1, 0, 0, 0, 0)) } else { Some(new DateTime(dtf.parseLocalDate(newemployee(11)).toDateTimeAtStartOfDay())) },
                      rl = if (newemployee(19) == "yes") { List("Admin") } else { List("") }
                  ),
                  wd = PersonModel.doc.wd.copy(
                      wd1 = if (newemployee(12) == "yes") { true } else { false },
                      wd2 = if (newemployee(13) == "yes") { true } else { false },
                      wd3 = if (newemployee(14) == "yes") { true } else { false },
                      wd4 = if (newemployee(15) == "yes") { true } else { false },
                      wd5 = if (newemployee(16) == "yes") { true } else { false },
                      wd6 = if (newemployee(17) == "yes") { true } else { false },
                      wd7 = if (newemployee(18) == "yes") { true } else { false }
                  )
              )
              PersonModel.insert(person_doc, p_request=request)
              
              // Create Audit Log
              AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=newemployee(22), c="Create by using CSV import."), p_request=request)
              
              // Create department keyword if import department no exist
              KeywordModel.addKeywordValue("Department", newemployee(9), request)
              
              // Create position keyword if import position no exist
              KeywordModel.addKeywordValue("Position Type", newemployee(4), request)
              
              // Output success result
              
            } }
            
            // Generate error information
           
                        
            
            println("")
            println(importdata)
            
            // Send Bulk Employee Status Email
  
            Future.successful(Ok(Json.obj("status"->"success")).as("application/json"))
          }
          
          
          
        } 
      }
      
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  private def addKeyword(p_keyword:String, p_value:String, p_request:RequestHeader) = {
    
    val department = KeywordModel.findOne(BSONDocument("n" -> p_keyword), p_request)
    department.map { doc => { 
      if (!doc.get.v.contains(p_value)) {
        KeywordModel.update(BSONDocument("_id" -> doc.get._id), doc.get.copy(v=doc.get.v.::(p_value)), p_request)
      }
    } }
    
  }
  
  private def getManagerID(p_doc:String, p_importdata:List[List[String]], p_request:RequestHeader) = {
    
    if (p_doc == "") { 
      ""
    } else {
      val smanager = Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_doc), p_request), Tools.db_timeout)
      if (smanager.isDefined) {
        smanager.get._id.stringify
      } else {
        val smgrdata = p_importdata.filter( value => value(3) == p_doc )
        smgrdata(0)(22)
      }
    }
              
  }
  
  private def valdata(
      p_EmployeeID:String, 
      p_FirstName:String, 
      p_LastName:String, 
      p_Email:String, 
      p_Position:String, 
      p_Manager:String, 
      p_SubstituteManager:String,
      p_Gender:String,
      p_MaritalStatus:String,
      p_Department:String,
      p_Office:String,
      p_JoinDate:String,
      p_WorkDayMonday:String,
      p_WorkDayTuesday:String,
      p_WorkDayWebnesday:String,
      p_WorkDayThursday:String,
      p_WorkDayFriday:String,
      p_WorkDaySaturday:String,
      p_WorkDaySunday:String,
      p_Admin:String,
      p_SendWelcomeEmail:String,
      p_empidlist:List[Any],
      p_empemaillist:List[Any],
      p_request:RequestHeader)  = {
    
    val isempidexist = if (p_EmployeeID!="") { 
      if (Await.result(PersonModel.findOne(BSONDocument("p.empid" -> p_EmployeeID), p_request), Tools.db_timeout).isDefined) {
        true
      } else if (p_empidlist.count { _ == p_EmployeeID } > 1) {
        true
      } else { 
        false
      }
    }
    
    val isempemailexist = if (p_Email!="") {
      if (Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_Email), p_request), Tools.db_timeout).isDefined) {
        true
      } else if (p_empemaillist.count { _ == p_Email } > 1) {
        true
      } else { 
        false
      }
    }
    
    val isSameManager = if (p_Manager != "" && p_SubstituteManager != "") {
      if (p_Manager == p_SubstituteManager) { true } else { false }
    }
    
    val ismanagerexist = if (p_Manager!="") {
      if (Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_Manager), p_request), Tools.db_timeout).isDefined) {
        true
      } else if (p_empemaillist.contains(p_Manager)) {
        true
      } else { 
        false
      }
    }
    
    val issmanagerexist = if (p_SubstituteManager!="") {
      if (Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_SubstituteManager), p_request), Tools.db_timeout).isDefined) {
        true
      } else if (p_empemaillist.contains(p_SubstituteManager)) {
        true
      } else { 
        false
      }
    }
      
    // Validate - mandatory fields
    if (p_FirstName=="" || p_LastName=="" || p_Gender=="" || p_MaritalStatus=="" || p_Office=="" ) {
      Map("status" -> "fail", "msg" -> "First Name (M), Last Name (M), Gender (M), Marital Status (M) and Office (M) is mandatory.")

      // Validation - Employee ID – if not empty, then must be unique.
    } else if (isempidexist == true) {
      Map("status" -> "fail", "msg" -> "Someone already used or duplicate employee id in import csv file.")
      
      // Validation - Email format
    } else if (p_Email!="" && !DataValidationUtility.isValidEmail(p_Email)) {
      Map("status" -> "fail", "msg" -> "Invalid email address format.")  
        
      // Validation - Email must be unique
    } else if (isempemailexist == true) {
      Map("status" -> "fail", "msg" -> "Someone already used or duplicate email address in import csv file.")  
      
      // Validation - Office is exist
    } else if (!DataValidationUtility.isOfficeExist(p_Office, p_request)) {
      Map("status" -> "fail", "msg" -> "Invalid office.")  
      
      // Validation - Gender
    } else if (!DataValidationUtility.isValidGender(p_Gender)) {
      Map("status" -> "fail", "msg" -> "Invalid gender.")  
      
      // Validation - Manager 
    } else if (ismanagerexist == false) {
      Map("status" -> "fail", "msg" -> "Invalid manager.")  
      
      // Validation - Substitute Manager 
    } else if (issmanagerexist == false) {
      Map("status" -> "fail", "msg" -> "Invalid substitute manager.")  
      
      // Validation - Manager and Substitute Manager is same
    } else if (isSameManager == true) {
      Map("status" -> "fail", "msg" -> "Substitute Manager can not same with Manager.")  
      
      // Validation - Marital Status
    } else if (!DataValidationUtility.isValidMaritalStatus(p_MaritalStatus)) {
      Map("status" -> "fail", "msg" -> "Invalid marital status.")  
      
      // Validation - Join Date Format
    } else if (p_JoinDate!="" && !DataValidationUtility.isValidDate(p_JoinDate)) {
      Map("status" -> "fail", "msg" -> "Join date format not recognised. Date format must dd-mmm-yyyy. ie 31-Jan-2017")
      
      // Validation - Boolean
    } else if (p_WorkDayMonday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayMonday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Monday. Please fill with Yes/No.")
      
    } else if (p_WorkDayTuesday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayTuesday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Tuesday. Please fill with Yes/No.")
      
    } else if (p_WorkDayWebnesday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayWebnesday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Webnesday. Please fill with Yes/No.")
      
    } else if (p_WorkDayThursday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayThursday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Thursday. Please fill with Yes/No.")
      
    } else if (p_WorkDayFriday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayFriday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Friday. Please fill with Yes/No.")
      
    } else if (p_WorkDaySaturday!="" && !DataValidationUtility.isValidYesNo(p_WorkDaySaturday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Saturday. Please fill with Yes/No.")
      
    } else if (p_WorkDaySunday!="" && !DataValidationUtility.isValidYesNo(p_WorkDaySunday)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Work Day - Sunday. Please fill with Yes/No.")
      
    } else if (p_Admin!="" && !DataValidationUtility.isValidYesNo(p_Admin)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Admin. Please fill with Yes/No.")
      
    } else if (p_SendWelcomeEmail!="" && !DataValidationUtility.isValidYesNo(p_SendWelcomeEmail)) {
      Map("status" -> "fail", "msg" -> "Invalid value for Send Welcome Email. Please fill with Yes/No.")
      
      // Validation Pass
    } else {
      Map("status" -> "pass", "msg" -> BSONObjectID.generate.stringify)
    }
    
  }
  
}