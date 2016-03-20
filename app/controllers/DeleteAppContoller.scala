package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ Json, JsObject, JsString }
import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}
import play.modules.reactivemongo.json._

import scala.concurrent.{Future, Await}

import models.{PersonModel, AuthenticationModel, KeywordModel, OfficeModel, CompanyHolidayModel, LeavePolicyModel, LeaveProfileModel, LeaveSettingModel, LeaveModel, CompanyModel, TaskModel, LeaveFileModel}
import utilities.{MailUtility, Tools}

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.api.gridfs._
import reactivemongo.api.gridfs.Implicits._

import org.joda.time.DateTime

import javax.inject.Inject

case class DeleteApp (company:String)

class DeleteAppController @Inject() (val reactiveMongoApi: ReactiveMongoApi, mailerClient: MailerClient) extends Controller with MongoController with ReactiveMongoComponents with Secured {
  
  import MongoController.readFileReads
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
    
  val deleteappform = Form(
      mapping(
          "company" -> text
      )(DeleteApp.apply)(DeleteApp.unapply)
  )
    
  def delete = withAuth { username => implicit request => {
    deleteappform.bindFromRequest.fold(
        formWithError => {
            val output = """{"status":"error","url":"""" + Tools.hostname + """/deleteapp/error"}"""
            Future.successful(Ok(Json.parse(output)).as("application/json"))
        },
        formWithData => {
          if (hasRoles(List("Admin"), request) == false) {
            val output = """{"status":"error","url":"""" + Tools.hostname + """/deleteapp/error"}"""
            Future.successful(Ok(Json.parse(output)).as("application/json"))
          } else if (formWithData.company != request.session.get("company").get){
            val output = """{"status":"fail","url":"""" + Tools.hostname + """/deleteapp/fail"}"""
            Future.successful(Ok(Json.parse(output)).as("application/json"))
          } else {
            
            // Soft deletion
            Await.result(AuthenticationModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request), Tools.db_timeout)
            CompanyModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            OfficeModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            PersonModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            LeaveModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            LeaveProfileModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            LeaveSettingModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            LeavePolicyModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            KeywordModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            CompanyHolidayModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            TaskModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.eid" -> request.session.get("entity").get, "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]().map { files =>
              val filesWithId = files.map { file => {
                LeaveFileModel.gridFS.files.update(
                    Json.obj("_id" -> file.id),
                    Json.obj("$set" -> Json.obj("metadata" -> Json.obj(     
                        "eid" -> file.metadata.value.get("eid").get,
                        "filename" -> file.metadata.value.get("filename").get,
                        "lk" -> file.metadata.value.get("lk").get,
                        "f" -> file.metadata.value.get("f").get,
                        "cby" -> file.metadata.value.get("cby").get,
                        "ddat" -> BSONDateTime(new DateTime().getMillis),
                        "dby" -> request.session.get("username")
                    )))
                )
              }}
            }
            
            // Send email
            mailerClient.send(
                MailUtility.getEmail(List("support@hrsifu.my"), "System Notification: " + formWithData.company + " deleted.", formWithData.company + "(" + request.session.get("entity").get +  ") deleted by " + request.session.get("username").get + ".")
            )
            
            // return result
            val output = """{"status":"fail","url":"""" + Tools.hostname + """/deleteapp/success"}"""
            Future.successful(Ok(Json.parse(output)).as("application/json"))
          }
        }
    )
  }}
  
  def success = withAuth { username => implicit request => {
    Cache.remove("PersonProfile." + request.session.get("username").get)
    Future.successful(Redirect(routes.AuthenticationController.login()).withNewSession.flashing("success" -> (request.session.get("company").get + " deleted.")))
  }}
  
  def fail = withAuth { username => implicit request => {
    Future.successful(Redirect(routes.DashboardController.index).flashing("error" -> "Delete abort! Incorrect company name entered."))
  }}
  
  def error = withAuth { username => implicit request => {
    Future.successful(Redirect(routes.DashboardController.index).flashing("error" -> "Delete abort! Contact support@hrlab.my."))
  }}
  
}