package controllers

import play.api._
import play.api.mvc._

class ErrorController extends Controller {
  
  def unsupportedbrowser = Action { implicit request => {
    Ok(views.html.error.unsupportedbrowser()) 
  }}
  
}

