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
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        persons <- PersonModel.find(BSONDocument(), request)
      } yield {
        if (persons.length < 500) {
          Ok(views.html.personbulkimport.form())
        } else {
          Ok(views.html.error.unauthorized())
        }
      }
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
          val csv = multipartForm.file("file").get       
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
          
          if (importrawdata.length > 50) {                  
            Future.successful(Ok(Json.obj("status" -> "exceed 50 employee")).as("application/json"))
            
          } else {
            
            // Generate import employee id list
            val empidlist = importrawdata.map{ importrawrow => if( importrawrow.contains("Employee ID")) { importrawrow("Employee ID").trim } }.filter( value => value != "" )
            val empemaillist = importrawdata.map{ importrawrow => if( importrawrow.contains("Email")) { importrawrow("Email").trim } }.filter( value => value != "" )
                        
            // Import data validation first round
            // Validate value, manadatory, duplicate emp id, duplicate email.
            val importpersons1 = importrawdata.map{ importrawrow => {
              
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
                  validation(0),
                  validation(1)
              )

            } }
            
            // Generate new list on pass validation employee
            val importpersons1PASSONLY = importpersons1.filter( value => value(21) != "fail" )
            
            // Import data validation second round
            // Validate manager exist in system and import sheet
            val importpersons2 = importpersons1.map { person =>{
              valmanager(person(5).toString, person, importpersons1PASSONLY, "manager", request)
            } }
            
            // Import data validation second round
            // Validate substitute manager exist in system and import sheet
            val importpersons3 = importpersons2.map { person =>{
              valmanager(person(6).toString, person, importpersons1PASSONLY, "substitute manager", request)
            } }
                           
            // Create document
            val createdresult = importpersons3.filter( value => value(21) != "fail" ).map { newemployee => {
              
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
                  _id=BSONObjectID(newemployee(22).toString),
                  p = PersonModel.doc.p.copy(
                      empid = newemployee(0),
                      fn = newemployee(1),
                      ln = newemployee(2),
                      em = newemployee(3),
                      nem = if ( newemployee(3) == "" ) { true } else { false },
                      pt = newemployee(4),
                      mgrid = newemployee(5),
                      smgrid = newemployee(6),
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
              "<tr style='font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px;'>" +
              "<td class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: normal;' valign='top' align='left'>" + newemployee(0) + "</td>"+
              "<td class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: normal;' valign='top' align='left'>" + newemployee(1) + " " + newemployee(2) + "</td>" +
              "<td class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: normal;' valign='top' align='left'>" + newemployee(3) + "</td>" + 
              "<td class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: normal;' valign='top' align='center'><div style='line-height: 36px;'><a style='margin: 0px; padding: 4px 9px; border: 4px solid rgb(171, 186, 195); border-image: none; text-align: center; color: rgb(255, 255, 255); line-height: 19px; font-size: 14px; text-decoration: none; vertical-align: baseline; background-color: rgb(171, 186, 195);' href='" + Tools.hostname + "/person/view/" + newemployee(22) + "' target='_blank'> &nbsp;&nbsp; &nbsp; View &nbsp; &nbsp;&nbsp;</a></div></td>" +
              "</tr>"
              
            } }
            
            // Generate error information
           val importerror = importpersons3.zipWithIndex.map { case (newemployee, index) => {
             if (newemployee(21) == "fail") {
               "Row " + { index + 2 } + ": " + newemployee(22) +"<br>"
             } else { "" }
           } }.filter( value => value != "" )
                        
            // Send email - import completed            
            val ImportDetail = if (createdresult.length > 0) {
              "<table class='table-row' style='table-layout: auto; width: 528px; background-color: #ffffff; border: 1px solid #ddd;' bgcolor='#FFFFFF' width='528' cellspacing='0' cellpadding='0' border='0'>" + 
              "<tr style='font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px;' bgcolor='#DDDDDD'>" + 
              "<th class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: bold;' valign='top' align='left' bgcolor='#DDDDDD'>Employee ID</th>" + 
              "<th class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: bold;' valign='top' align='left' bgcolor='#DDDDDD'>Name</th>" + 
              "<th class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: bold;' valign='top' align='left' bgcolor='#DDDDDD'>Email</th>" + 
              "<th class='table-row-td' style='padding: 3px; font-family: Arial, sans-serif; line-height: 19px; color: #444444; font-size: 13px; font-weight: bold;' valign='top' align='left' bgcolor='#DDDDDD'></th>" + 
              "</tr>" + createdresult.mkString("") + "</table>"
              } else { "" }
            val ImportError = if (importerror.length > 0) { "<p>However, we encounter the errors below:</p>" + importerror.mkString } else { "" }
            val replaceMap = Map(
                "Admin" -> request.session.get("name").get,
                "TotalImport" -> createdresult.length.toString,
                "ImportDetail" -> ImportDetail,
                "ImportError" -> ImportError
            )
            MailUtility.getEmailConfig(List(request.session.get("username").get), 23, replaceMap).map { email => mailerClient.send(email) }
            
            val outputresult = if( createdresult.length > 0) { 
              "<p>A total of " + createdresult.length.toString + " employee have been successfully added.</p>" + ImportDetail + "<br>"
              } else {
                "<p>A total of " + createdresult.length.toString + " employee have been successfully added.</p>"
              }

            // Return json message
            Future.successful(Ok(Json.obj("status"->"success", "result"-> {outputresult + ImportError} )).as("application/json"))
          }
          
        } 
      }
      
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
      
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
          
    // Validate - mandatory fields
    if (p_FirstName=="" || p_LastName=="" || p_Gender=="" || p_MaritalStatus=="" || p_Office=="" ) {
      List("fail", "First Name (M), Last Name (M), Gender (M), Marital Status (M) and Office (M) is mandatory.")

      // Validation - Employee ID – if not empty, then must be unique.
    } else if (isempidexist == true) {
      List("fail", "Someone already used or duplicate employee id in import csv file.")
      
      // Validation - Email format
    } else if (p_Email!="" && !DataValidationUtility.isValidEmail(p_Email)) {
      List("fail", "Invalid email address format.")  
        
      // Validation - Email must be unique
    } else if (isempemailexist == true) {
      List("fail", "Someone already used or duplicate email address in import csv file.")  
      
      // Validation - Office is exist
    } else if (!DataValidationUtility.isOfficeExist(p_Office, p_request)) {
      List("fail", "Invalid office.")  
      
      // Validation - Gender
    } else if (!DataValidationUtility.isValidGender(p_Gender)) {
      List("fail", "Invalid gender.")  
            
      // Validation - Manager and Substitute Manager is same
    } else if (isSameManager == true) {
      List("fail", "Substitute Manager can not same with Manager.")  
      
      // Validation - Marital Status
    } else if (!DataValidationUtility.isValidMaritalStatus(p_MaritalStatus)) {
      List("fail", "Invalid marital status.")  
      
      // Validation - Join Date Format
    } else if (p_JoinDate!="" && !DataValidationUtility.isValidDate(p_JoinDate)) {
      List("fail", "Join date format not recognised. Date format must dd-mmm-yyyy. ie 31-Jan-2017")
      
      // Validation - Boolean
    } else if (p_WorkDayMonday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayMonday)) {
      List("fail", "Invalid value for Work Day - Monday. Please fill with Yes/No.")
      
    } else if (p_WorkDayTuesday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayTuesday)) {
      List("fail", "Invalid value for Work Day - Tuesday. Please fill with Yes/No.")
      
    } else if (p_WorkDayWebnesday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayWebnesday)) {
      List("fail", "Invalid value for Work Day - Webnesday. Please fill with Yes/No.")
      
    } else if (p_WorkDayThursday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayThursday)) {
      List("fail", "Invalid value for Work Day - Thursday. Please fill with Yes/No.")
      
    } else if (p_WorkDayFriday!="" && !DataValidationUtility.isValidYesNo(p_WorkDayFriday)) {
      List("fail", "Invalid value for Work Day - Friday. Please fill with Yes/No.")
      
    } else if (p_WorkDaySaturday!="" && !DataValidationUtility.isValidYesNo(p_WorkDaySaturday)) {
      List("fail", "Invalid value for Work Day - Saturday. Please fill with Yes/No.")
      
    } else if (p_WorkDaySunday!="" && !DataValidationUtility.isValidYesNo(p_WorkDaySunday)) {
      List("fail", "Invalid value for Work Day - Sunday. Please fill with Yes/No.")
      
    } else if (p_Admin!="" && !DataValidationUtility.isValidYesNo(p_Admin)) {
      List("fail", "Invalid value for Admin. Please fill with Yes/No.")
      
    } else if (p_SendWelcomeEmail!="" && !DataValidationUtility.isValidYesNo(p_SendWelcomeEmail)) {
      List("fail", "Invalid value for Send Welcome Email. Please fill with Yes/No.")
      
      // Validation Pass
    } else {
      List("pass", BSONObjectID.generate.stringify)
    }
    
  }
  
  // This function is to validate manager and substitute manager.
  private def valmanager(p_manager:String, p_person:List[Any], p_passed_person:List[List[Any]], p_option:String, p_request:RequestHeader) : List[String] = {
            
    if (p_person(21) == "fail"){
      List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString())
    } else {
      
      // validate manager
      if (p_option == "manager") {

        if (p_manager == "") {
          val manager = p_passed_person.filter( value => value(3) == p_person(3) )
          if (manager.isEmpty) {
            List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, "fail", "Invalid manager.")
          } else {
            List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, manager(0)(22).toString(), p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString)
          }
        } else {
          val manager = Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_manager), p_request), Tools.db_timeout)
          if (manager.isDefined) {
            List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, manager.get._id.stringify, p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString)
          } else {
            val manager = p_passed_person.filter( value => value(3) == p_manager )
            if (manager.isEmpty) {
              List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, "fail", "Invalid manager.")
            } else {
              List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, manager(0)(22).toString(), p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString)
            } 
          }
        }
        
      } else {
        
        // validate substitute manager
        if (p_manager == "") {
          List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, "", p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString())
        } else {
          val manager = Await.result(PersonModel.findOne(BSONDocument("p.em" -> p_manager), p_request), Tools.db_timeout)
          if (manager.isDefined) {
            List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, manager.get._id.stringify, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString)
          } else {
            val manager = p_passed_person.filter( value => value(3) == p_manager )
            if (manager.isEmpty) {
              List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, p_person(6).toString, p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, "fail", "Invalid substitute manager.")
            } else {
              List(p_person(0).toString, p_person(1).toString, p_person(2).toString, p_person(3).toString, p_person(4).toString, p_person(5).toString, manager(0)(22).toString(), p_person(7).toString, p_person(8).toString, p_person(9).toString, p_person(10).toString, p_person(11).toString, p_person(12).toString, p_person(13).toString, p_person(14).toString, p_person(15).toString, p_person(16).toString, p_person(17).toString(), p_person(18).toString, p_person(19).toString, p_person(20).toString, p_person(21).toString, p_person(22).toString)
            } 
          }
        }
        
      }

    }
    
  }
  
}