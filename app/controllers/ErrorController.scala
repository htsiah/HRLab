package controllers

import play.api._
import play.api.mvc._

object ErrorController extends Controller {
  
  def unsupportedbrowser = Action { implicit request => {
    Ok(views.html.error.unsupportedbrowser()) 
  }}
  
}

