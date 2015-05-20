package controllers

import scala.util.{Success, Failure,Try,Random}
import scala.concurrent.{Future, Await}
import org.joda.time.DateTime

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.concurrent.Execution.Implicits._

import models.{LeaveProfileModel, LeaveProfile, LeaveProfileMonthEarn, Entitlement, LeavePolicyModel, PersonModel, LeaveModel}
import utilities.{System, AlertUtility, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument,BSONArray}

object LeaveProfileController extends Controller with Secured {
  
  val leaveprofileform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "pid" -> text,
          "pn" -> text,
          "lt" -> text,
          "ent" -> number,
          "ear" -> of[Double],
          "adj" -> number,
          "uti" -> of[Double],
          "cf" -> of[Double],
          "cfuti" -> of[Double],
          "cfexp" -> of[Double],
          "bal" -> of[Double],
          "me" -> mapping(
              "jan" -> of[Double],
              "feb" -> of[Double],
              "mar" -> of[Double],
              "apr" -> of[Double],
              "may" -> of[Double],
              "jun" -> of[Double],
              "jul" -> of[Double],
              "aug" -> of[Double],
              "sep" -> of[Double],
              "oct" -> of[Double],
              "nov" -> of[Double],
              "dec" -> of[Double]
          )(LeaveProfileMonthEarn.apply)(LeaveProfileMonthEarn.unapply),
          "set_ent" -> mapping(
              "e1" -> number,
              "e1_s" -> number,
              "e1_cf" -> number,
              "e2" -> number,
              "e2_s" -> number,
              "e2_cf" -> number,
              "e3" -> number,
              "e3_s" -> number,
              "e3_cf" -> number,
              "e4" -> number,
              "e4_s" -> number,
              "e4_cf" -> number,
              "e5" -> number,
              "e5_s" -> number,
              "e5_cf" -> number
          )(Entitlement.apply)(Entitlement.unapply),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))
      ){(_id,pid,pn,lt,ent,ear,adj,uti,cf,cfuti,cfexp,bal,me,set_ent,sys)=>LeaveProfile(_id,pid,pn,lt,ent,ear,adj,uti,cf,cfuti,cfexp,bal,me,set_ent,sys)}
      {leaveprofile:LeaveProfile=>
        Some(leaveprofile._id,
            leaveprofile.pid,
            leaveprofile.pn,
            leaveprofile.lt,
            leaveprofile.ent,
            leaveprofile.ear,
            leaveprofile.adj,
            leaveprofile.uti,
            leaveprofile.cf,
            leaveprofile.cfuti,
            leaveprofile.cfexp,
            leaveprofile.bal,
            leaveprofile.me,
            leaveprofile.set_ent,
            leaveprofile.sys)      
      }
  )
  
  def create(p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request) 
        leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
      } yield {
        maybeperson.map( person => {
          val leaveprofile_doc = LeaveProfile(
              _id = BSONObjectID.generate,
              pid = p_pid,
              pn = person.p.fn + " " + person.p.ln,
              lt = "",
              ent = 0,
              ear = 0.0,
              adj = 0,
              uti = 0.0,
              cf = 0.0,
              cfuti = 0.0,
              cfexp = 0.0,
              bal = 0.0,
              me = LeaveProfileMonthEarn(
                  jan = 0.0,
                  feb = 0.0,
                  mar = 0.0,
                  apr = 0.0,
                  may = 0.0,
                  jun = 0.0,
                  jul = 0.0,
                  aug = 0.0,
                  sep = 0.0,
                  oct = 0.0,
                  nov = 0.0,
                  dec = 0.0
              ),
              set_ent = Entitlement(
                  e1 = 0,
                  e1_s = 0,
                  e1_cf = 0,
                  e2 = 0,
                  e2_s = 0,
                  e2_cf = 0,
                  e3 = 0,
                  e3_s = 0,
                  e3_cf = 0,
                  e4 = 0,
                  e4_s = 0,
                  e4_cf = 0,
                  e5 = 0,
                  e5_s = 0,
                  e5_cf = 0
              ),
              sys = None
          )
          Ok(views.html.leaveprofile.form(leaveprofileform.fill(leaveprofile_doc), leavepolicies, p_pid, person.p.pt))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert(p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request) 
              leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
            } yield {
              maybeperson.map( person => {
                Ok(views.html.leaveprofile.form(formWithError, leavepolicies, p_pid, person.p.pt))
              }).getOrElse(NotFound)
            }
          },
          formWithData => {
            for {
              leaveprofileunique <- LeaveProfileModel.isUnique(formWithData, request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request)
              leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid" ->formWithData.pid, "lt"->formWithData.lt), request)
              maybe_alert <- AlertUtility.findOne(BSONDocument("k"->1005))
            } yield {
              if (leaveprofileunique) {
                val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
                Await.result(
                  LeaveProfileModel.insert(                  
                      formWithData.copy(
                          _id=BSONObjectID.generate, 
                          set_ent=Entitlement(
                              e1_s=eligbleleaveentitlement(0)(0),
                              e1=eligbleleaveentitlement(0)(1),
                              e1_cf=eligbleleaveentitlement(0)(2),
                              e2_s=eligbleleaveentitlement(1)(0),
                              e2=eligbleleaveentitlement(1)(1),
                              e2_cf=eligbleleaveentitlement(1)(2),
                              e3_s=eligbleleaveentitlement(2)(0),
                              e3=eligbleleaveentitlement(2)(1),
                              e3_cf=eligbleleaveentitlement(2)(2),
                              e4_s=eligbleleaveentitlement(3)(0),
                              e4=eligbleleaveentitlement(3)(1),
                              e4_cf=eligbleleaveentitlement(3)(2),
                              e5_s=eligbleleaveentitlement(4)(0),
                              e5=eligbleleaveentitlement(4)(1),
                              e5_cf=eligbleleaveentitlement(4)(2)
                          )
                      ),
                      p_request=request),
                  Tools.db_timeout
                )
                Redirect(routes.PersonController.view(p_pid))
              } else {
                val replaceMap = Map(
                    "NAME"-> (maybeperson.get.p.fn + " " + maybeperson.get.p.ln),
                    "LEAVEPROFILE"-> formWithData.lt,
                    "URL"->(Tools.hostname+"/leaveprofile/view?p_id=" + maybeleaveprofile.get._id.stringify)
                )
                val alert = if ((maybe_alert.getOrElse(null))!=null) { maybe_alert.get.copy(m=Tools.replaceSubString(maybe_alert.get.m, replaceMap.toList)) } else { null }
                maybeperson.map( person => {
                  Ok(views.html.leaveprofile.form(leaveprofileform.fill(formWithData), leavepolicies, p_pid, person.p.pt, "", alert))
                }).getOrElse(NotFound)
              }
            }
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request) 
        leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
      } yield {
        maybeleaveprofile.map( leaveprofile => {
            Ok(views.html.leaveprofile.form(leaveprofileform.fill(leaveprofile), leavepolicies, leaveprofile.pid, "", p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request) 
              leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
            } yield {
              Ok(views.html.leaveprofile.form(formWithError, leavepolicies, maybeleaveprofile.get.pid, "", p_id))
            }
          },
          formWithData => {
            val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
            Await.result(
              LeaveProfileModel.update(
                  BSONDocument("_id" -> BSONObjectID(p_id)), 
                  formWithData.copy(                      
                      _id=BSONObjectID(p_id), 
                      set_ent=Entitlement(
                          e1_s=eligbleleaveentitlement(0)(0),
                          e1=eligbleleaveentitlement(0)(1),
                          e1_cf=eligbleleaveentitlement(0)(2),
                          e2_s=eligbleleaveentitlement(1)(0),
                          e2=eligbleleaveentitlement(1)(1),
                          e2_cf=eligbleleaveentitlement(1)(2),
                          e3_s=eligbleleaveentitlement(2)(0),
                          e3=eligbleleaveentitlement(2)(1),
                          e3_cf=eligbleleaveentitlement(2)(2),
                          e4_s=eligbleleaveentitlement(3)(0),
                          e4=eligbleleaveentitlement(3)(1),
                          e4_cf=eligbleleaveentitlement(3)(2),
                          e5_s=eligbleleaveentitlement(4)(0),
                          e5=eligbleleaveentitlement(4)(1),
                          e5_cf=eligbleleaveentitlement(4)(2)
                      )
                  ), 
                  request),
                Tools.db_timeout
            )
            Future.successful(Redirect(routes.PersonController.view(formWithData.pid)))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def delete(p_id:String, p_lt:String, p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(LeaveProfileModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      LeaveModel.setLockDown(BSONDocument("pid" -> p_pid, "lt" -> p_lt), request)
      Future.successful(Redirect(routes.PersonController.view(p_pid)))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def view(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      } yield {
        maybeleaveprofile.map( leaveprofile => {
          Ok(views.html.leaveprofile.view(leaveprofile))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofilecreate = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
        leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
      } yield {
        maybeperson.map( person => {
          val leaveprofile_doc = LeaveProfile(
              _id = BSONObjectID.generate,
              pid = request.session.get("id").get,
              pn = person.p.fn + " " + person.p.ln,
              lt = "",
              ent = 0,
              ear = 0.0,
              adj = 0,
              uti = 0.0,
              cf = 0.0,
              cfuti = 0.0,
              cfexp = 0.0,
              bal = 0.0,
              me = LeaveProfileMonthEarn(
                  jan = 0.0,
                  feb = 0.0,
                  mar = 0.0,
                  apr = 0.0,
                  may = 0.0,
                  jun = 0.0,
                  jul = 0.0,
                  aug = 0.0,
                  sep = 0.0,
                  oct = 0.0,
                  nov = 0.0,
                  dec = 0.0
              ),
              set_ent = Entitlement(
                  e1 = 0,
                  e1_s = 0,
                  e1_cf = 0,
                  e2 = 0,
                  e2_s = 0,
                  e2_cf = 0,
                  e3 = 0,
                  e3_s = 0,
                  e3_cf = 0,
                  e4 = 0,
                  e4_s = 0,
                  e4_cf = 0,
                  e5 = 0,
                  e5_s = 0,
                  e5_cf = 0
              ),
              sys = None
          )
          Ok(views.html.leaveprofile.myprofileform(leaveprofileform.fill(leaveprofile_doc), leavepolicies, person.p.pt))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileinsert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
              leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
            } yield {
              maybeperson.map( person => {
                Ok(views.html.leaveprofile.myprofileform(formWithError, leavepolicies, person.p.pt))
              }).getOrElse(NotFound)
            }
          },
          formWithData => {
            for {
              leaveprofileunique <- LeaveProfileModel.isUnique(formWithData, request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
              leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid" ->formWithData.pid, "lt"->formWithData.lt), request)
              maybe_alert <- AlertUtility.findOne(BSONDocument("k"->1005))
            } yield {
              if (leaveprofileunique) {
                val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
                Await.result(
                  LeaveProfileModel.insert(                  
                      formWithData.copy(
                          _id=BSONObjectID.generate, 
                          set_ent=Entitlement(
                              e1_s=eligbleleaveentitlement(0)(0),
                              e1=eligbleleaveentitlement(0)(1),
                              e1_cf=eligbleleaveentitlement(0)(2),
                              e2_s=eligbleleaveentitlement(1)(0),
                              e2=eligbleleaveentitlement(1)(1),
                              e2_cf=eligbleleaveentitlement(1)(2),
                              e3_s=eligbleleaveentitlement(2)(0),
                              e3=eligbleleaveentitlement(2)(1),
                              e3_cf=eligbleleaveentitlement(2)(2),
                              e4_s=eligbleleaveentitlement(3)(0),
                              e4=eligbleleaveentitlement(3)(1),
                              e4_cf=eligbleleaveentitlement(3)(2),
                              e5_s=eligbleleaveentitlement(4)(0),
                              e5=eligbleleaveentitlement(4)(1),
                              e5_cf=eligbleleaveentitlement(4)(2)
                          )
                      ),
                      p_request=request),
                  Tools.db_timeout
                )
                Redirect(routes.PersonController.myprofileview)
              } else {
                val replaceMap = Map(
                    "NAME"-> (maybeperson.get.p.fn + " " + maybeperson.get.p.ln),
                    "LEAVEPROFILE"-> formWithData.lt,
                    "URL"->(Tools.hostname+"/leaveprofile/view?p_id=" + maybeleaveprofile.get._id.stringify)
                )
                val alert = if ((maybe_alert.getOrElse(null))!=null) { maybe_alert.get.copy(m=Tools.replaceSubString(maybe_alert.get.m, replaceMap.toList)) } else { null }
                maybeperson.map( person => {
                  Ok(views.html.leaveprofile.myprofileform(leaveprofileform.fill(formWithData), leavepolicies, person.p.pt, "", alert))
                }).getOrElse(NotFound)
              }
            }
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileedit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
        leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
      } yield {
        maybeleaveprofile.map( leaveprofile => {
          Ok(views.html.leaveprofile.myprofileform(leaveprofileform.fill(leaveprofile), leavepolicies, "", p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileupdate(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
              leavepolicies <- LeavePolicyModel.getLeavePolicies(maybeperson.get.p.pt, maybeperson.get.p.g, maybeperson.get.p.ms, request)
            } yield {
              Ok(views.html.leaveprofile.myprofileform(formWithError, leavepolicies, "", p_id))
            }
          },
          formWithData => {
            val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
            Await.result(
              LeaveProfileModel.update(
                  BSONDocument("_id" -> BSONObjectID(p_id)), 
                  formWithData.copy(                      
                      _id=BSONObjectID(p_id), 
                      set_ent=Entitlement(
                          e1_s=eligbleleaveentitlement(0)(0),
                          e1=eligbleleaveentitlement(0)(1),
                          e1_cf=eligbleleaveentitlement(0)(2),
                          e2_s=eligbleleaveentitlement(1)(0),
                          e2=eligbleleaveentitlement(1)(1),
                          e2_cf=eligbleleaveentitlement(1)(2),
                          e3_s=eligbleleaveentitlement(2)(0),
                          e3=eligbleleaveentitlement(2)(1),
                          e3_cf=eligbleleaveentitlement(2)(2),
                          e4_s=eligbleleaveentitlement(3)(0),
                          e4=eligbleleaveentitlement(3)(1),
                          e4_cf=eligbleleaveentitlement(3)(2),
                          e5_s=eligbleleaveentitlement(4)(0),
                          e5=eligbleleaveentitlement(4)(1),
                          e5_cf=eligbleleaveentitlement(4)(2)
                      )
                  ), 
                  request),
                Tools.db_timeout
            )
            Future.successful(Redirect(routes.PersonController.myprofileview))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}

  def myprofileview(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
    } yield {
      maybeleaveprofile.map( leaveprofile => {
        Ok(views.html.leaveprofile.myprofileview(leaveprofile))
    }).getOrElse(NotFound)
    }
  }}
  
  def myprofiledelete(p_id:String, p_lt:String, p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(LeaveProfileModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      LeaveModel.setLockDown(BSONDocument("pid" -> p_pid, "lt" -> p_lt), request)
      Future.successful(Redirect(routes.PersonController.myprofileview))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
}