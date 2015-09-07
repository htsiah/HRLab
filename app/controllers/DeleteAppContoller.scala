package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._

import scala.concurrent.{Future, Await}

import models.{PersonModel, AuthenticationModel, KeywordModel, OfficeModel, LeavePolicyModel, LeaveProfileModel, LeaveSettingModel, LeaveModel, CompanyModel, TaskModel}
import utilities.{MailUtility, Tools}
import reactivemongo.bson.BSONDocument

case class DeleteApp (company:String)

object DeleteAppController extends Controller with Secured {
  
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
            TaskModel.remove(BSONDocument("sys.eid" -> request.session.get("entity").get), request)
            
            // Send email
            MailUtility.sendEmail(List("support@hrsifu.my"), "System Notification: " + formWithData.company + " deleted.", formWithData.company + "(" + request.session.get("entity").get +  ") deleted by " + request.session.get("username").get + ".")
            
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