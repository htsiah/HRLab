package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

import models.TaskModel

class TaskController extends Controller with Secured {
  
  def dismiss(p_lk: String) = withAuth { username => implicit request => {
    for {
      isTaskCompleted <- TaskModel.setCompleted(p_lk, request)
    } yield {
      Ok(Json.parse("""{"status":true}""")).as("application/json")
    }
  }}

}