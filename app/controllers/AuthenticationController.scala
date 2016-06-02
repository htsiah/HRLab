package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.mailer._

import models.{AuthenticationModel, Authentication, PersonModel, CompanyModel, Person}
import utilities.{System, AlertUtility, MailUtility, Tools, DbLoggerUtility}

import reactivemongo.bson.{BSONObjectID,BSONDocument}

import scala.concurrent.{Future,Await}
import scala.util.Random

import javax.inject.Inject

case class Login (email:String,password:String,redirect:String)
case class Reset (email:String)
case class Set (email:String,resetkey:String,npassword:String,cpassword:String)
case class Change (password:String,npassword:String,cpassword:String)

class AuthenticationController @Inject() (mailerClient: MailerClient) extends Controller with Secured {
    
  val authenticationform = Form(
      mapping(
          "email" -> nonEmptyText,
          "password" -> nonEmptyText,
          "redirect" -> text
      )(Login.apply)(Login.unapply)
  )
  
  val resetform = Form(
      mapping(
          "email" -> nonEmptyText
      )(Reset.apply)(Reset.unapply)
  )
  
  val setform = Form(
      mapping(
          "email"-> nonEmptyText,
          "resetkey" -> nonEmptyText,
          "npassword" -> nonEmptyText,
          "cpassword" -> nonEmptyText
      )(Set.apply)(Set.unapply)
  )
  
  val changeform = Form(
      mapping(
          "password" -> nonEmptyText,
          "npassword" -> nonEmptyText,
          "cpassword" -> nonEmptyText
      )(Change.apply)(Change.unapply)
  )
  
  def login(p_email:String, p_redirect:String) = Action { implicit request =>
    val login_doc = Login(email=p_email,password="",redirect=p_redirect)
    Ok(views.html.authentication.login(authenticationform.fill(login_doc)))
  }
  
  def logout = Action { implicit request =>
    Cache.remove("PersonProfile." + request.session.get("username").get)
    Redirect(routes.AuthenticationController.login()).withNewSession
  }
  
  def authentication = Action.async { implicit request => {
    authenticationform.bindFromRequest.fold(
        formWithError => {
          val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1001)), Tools.db_timeout)
          Future.successful(Ok(views.html.authentication.login(formWithError,alert.getOrElse(null))))
        },
        formWithData => {
          AuthenticationModel.findOneByEmail(formWithData.email.toLowerCase()).map( auth_doc => {
            auth_doc.isDefined match {
              case true => {
                if (auth_doc.get.p == formWithData.password) {
                  
                  // Create authentication log
                  DbLoggerUtility.auth(auth_doc.get.sys.get.eid.get, auth_doc.get.em, request)
                  
                  // Updating login history records
                  CompanyModel.logon(auth_doc.get.sys.get.eid.get)
                  AuthenticationModel.logon(auth_doc.get)
                  
                  // Create Person Profile Cache
                  Cache.remove("PersonProfile." + auth_doc.get.em)
                  val person_doc = Cache.getOrElse[Option[Person]]("PersonProfile." + auth_doc.get.em) {
                    Await.result(PersonModel.findOneByEmail(auth_doc.get.em), Tools.db_timeout)
                  }                  
                  
                  val maybe_IsManager = Await.result(PersonModel.findOne(BSONDocument("p.mgrid" -> person_doc.get._id.stringify, "sys.eid" -> person_doc.get.sys.get.eid.get)), Tools.db_timeout)
                  val isManager = if(maybe_IsManager.isEmpty) "false" else "true"       
                  
                  val company_doc = Await.result(CompanyModel.findOneByEId(auth_doc.get.sys.get.eid.get), Tools.db_timeout)
                  if (formWithData.redirect!="") {
                    Redirect(formWithData.redirect).withSession(
                        "entity"->person_doc.get.sys.get.eid.get,
                        "id"->person_doc.get._id.stringify,
                        "username"->person_doc.get.p.em,
                        "name"->(person_doc.get.p.fn+" "+person_doc.get.p.ln),
                        "company"->company_doc.get.c,
                        "department"->person_doc.get.p.dpm,
                        "position"->person_doc.get.p.pt,
                        "office"->person_doc.get.p.off,
                        "roles"->person_doc.get.p.rl.mkString(","),
                        "managerid"->person_doc.get.p.mgrid,
                        "smanagerid"->person_doc.get.p.smgrid,
                        "path"->(routes.DashboardController.index).toString,
                        "ismanager"->isManager
                    ) 
                  } else {
                    Redirect(routes.DashboardController.index).withSession(
                        "entity"->person_doc.get.sys.get.eid.get,
                        "id"->person_doc.get._id.stringify,
                        "username"->person_doc.get.p.em,
                        "name"->(person_doc.get.p.fn+" "+person_doc.get.p.ln),
                        "company"->company_doc.get.c,
                        "department"->person_doc.get.p.dpm,
                        "position"->person_doc.get.p.pt,
                        "office"->person_doc.get.p.off,
                        "roles"->person_doc.get.p.rl.mkString(","),
                        "managerid"->person_doc.get.p.mgrid,
                        "smanagerid"->person_doc.get.p.smgrid,
                        "path"->(routes.DashboardController.index).toString,
                        "ismanager"->isManager
                    ) 
                  }
                } else { 
                  val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1000)), Tools.db_timeout)
                  Ok(views.html.authentication.login(authenticationform.fill(formWithData),alert.getOrElse(null)))
                }
              }
              case _ => {
                val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1000)), Tools.db_timeout)
                Ok(views.html.authentication.login(authenticationform.fill(formWithData),alert.getOrElse(null)))
              }
            }
          })
        }
    )
  }}
  
  def reset = Action { implicit request => {
    val reset_doc = Reset(email="")
    Ok(views.html.authentication.reset(resetform.fill(reset_doc)))
  }}
  
  def resetPost = Action.async { implicit request => {
    resetform.bindFromRequest.fold(
        formWithError => {
          val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1001)), Tools.db_timeout)
          Future.successful(Ok(views.html.authentication.reset(formWithError,alert.getOrElse(null))))
        },
        formWithData => {         
          val email = formWithData.email.toLowerCase()
          AuthenticationModel.findOneByEmail(email).map( user => {
            user.isDefined match {
              case true => {
                val resetkey = Random.alphanumeric.take(8).mkString
                val modifier = BSONDocument("$set"->BSONDocument("r"->resetkey))
                AuthenticationModel.updateUsingBSON(BSONDocument("em"->email, "sys.ddat"->BSONDocument("$exists"->false)), modifier)
                val replaceMap = Map("URL"->(Tools.hostname+"/set/"+email+"/"+resetkey))
                MailUtility.getEmailConfig(List(email), 2, replaceMap).map { email => mailerClient.send(email) }
                val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->2000)), Tools.db_timeout)
                Redirect(routes.AuthenticationController.login()).flashing("success" -> alert.get.m) 
              }
              case _ => {
                val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1002)), Tools.db_timeout)
                Ok(views.html.authentication.reset(resetform.fill(formWithData),alert.getOrElse(null)))
              }
            }
          })
        }
     )
  }}
    
  def change = withAuth { username => implicit request => {  
    val change_doc = Change(password="", npassword="", cpassword="")
    Future.successful(Ok(views.html.authentication.change(changeform.fill(change_doc))))
  }}
      
  def changePost = withAuth { username => implicit request => { 
    changeform.bindFromRequest.fold(
        formWithError => {
          Future.successful(Ok(views.html.authentication.change(formWithError)))
        },
        formWithData => {
          AuthenticationModel.findOneByEmail(request.session.get("username").get).map( auth_doc => {
            if (auth_doc.get.p == formWithData.password) {
              AuthenticationModel.update(BSONDocument("em" -> request.session.get("username").get), auth_doc.get.copy(p=formWithData.npassword), request)
              Redirect(routes.DashboardController.index).flashing("success" -> "Password updated")
            } else {
              val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1012)), Tools.db_timeout)
              val change_doc = Change(password="", npassword="", cpassword="")
              Ok(views.html.authentication.change(changeform.fill(change_doc), alert.getOrElse(null)))
            }
          })
        }
    )
  }}
  
  def set(p_email:String,p_resetkey:String) = Action.async { implicit request => {
    AuthenticationModel.findReset(p_email, p_resetkey).map( user => {
      user.isDefined match {
        case true => {
          val set_doc = Set(email=p_email,resetkey=p_resetkey,npassword="",cpassword="")
          Ok(views.html.authentication.set(setform.fill(set_doc)))
        }
        case _ => {
          val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1003)), Tools.db_timeout)
          Redirect(routes.AuthenticationController.reset).flashing("error" -> alert.get.m)
        }
      }
    })
  }}
  
  def setPost = Action.async { implicit request => {
    setform.bindFromRequest.fold(
        formWithError => {
          val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1001)), Tools.db_timeout)
          Future.successful(Ok(views.html.authentication.set(formWithError,alert.getOrElse(null)))) 
        },
        formWithData => {
          AuthenticationModel.findReset(formWithData.email, formWithData.resetkey).map( user => {
            user.isDefined match {
              case true => {
                AuthenticationModel.resetPassword(user.get, formWithData.npassword)
                val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->2001)), Tools.db_timeout)
                Redirect(routes.AuthenticationController.login(formWithData.email)).flashing("success" -> alert.get.m)
              }
              case _ => {
                val alert =  Await.result(AlertUtility.findOne(BSONDocument("k"->1003)), Tools.db_timeout)
                Redirect(routes.AuthenticationController.reset).flashing("error" -> alert.get.m)
              }
            }
          })
        }
    )
  }}
    
}

trait Secured {
  
  def username(request: RequestHeader) = request.session.get(Security.username)
  
  def onUnauthorized(request: RequestHeader) = {
    Results.Redirect(routes.AuthenticationController.login(p_redirect=request.uri))
  }
  
  // check user have authenticated
  def withAuth(f: => String => Request[AnyContent] => Future[Result]) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action.async(request => f(user)(request))
    }
  }
  
  def withAuth[A](bp: BodyParser[A])(f: => String => Request[A] => Future[Result]) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action.async(bp)(request => f(user)(request))
    }
  }

  // Deprecated on v1.3.1
  /* getPersonProfile(request).get
   * implicit def getPersonProfile(p_request: RequestHeader) = {
   * Cache.getOrElse[Option[Person]]("PersonProfile." + p_request.session.get("username").get) {
   * Await.result(PersonModel.findOneByEmail(p_request.session.get("username").get), Tools.db_timeout)
   *   }  
   * }
   */
  
  // Check if current login user have the role
  // hasRoles(List("Admin"), request)
  implicit def hasRoles(p_roles: List[String], p_request: RequestHeader):Boolean = {
    val personroles = p_request.session.get("roles").get
    if(p_roles.length>1){
      personroles.contains(p_roles.head) || hasRoles(p_roles.tail, p_request)
    }else{
      personroles.contains(p_roles.head)
    }
  }
  
}