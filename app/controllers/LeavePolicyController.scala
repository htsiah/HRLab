package controllers

import scala.concurrent.{Future, Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeavePolicyModel, KeywordModel, Keyword, LeavePolicy, LeavePolicySetting, Entitlement, LeaveProfileModel, LeaveModel, PersonModel}
import utilities.{System, AlertUtility, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument}

class LeavePolicyController extends Controller with Secured {
  
  val leavepolicyform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "lt" -> text,
          "set" -> mapping(
              "g" -> text,
              "acc" -> text,
              "ms" -> text,
              "dt" -> text,
              "nwd" -> boolean,
              "cexp" -> number,
              "scal" -> boolean,
              "msd" -> boolean
          )(LeavePolicySetting.apply)(LeavePolicySetting.unapply), 
          "ent" -> mapping(
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
      ){(_id,lt,set,ent,sys)=>LeavePolicy(_id,lt,set,ent,sys)}
      {leavepolicy:LeavePolicy=>Some(leavepolicy._id, leavepolicy.lt, leavepolicy.set, leavepolicy.ent, leavepolicy.sys)}
  ) 
  
  def view(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request) 
      } yield {
        maybeleavepolicy.map( leavepolicy => {
          Ok(views.html.leavepolicy.view(leavepolicy))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      val doc = LeavePolicyModel.doc
      for { 
        maybe_leavetypes <- KeywordModel.findOne(BSONDocument("n" -> "Leave Type"), request)
        maybe_positiontypes <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
      } yield {
        val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
        val positiontypes = maybe_positiontypes.getOrElse(KeywordModel.doc)
        Ok(views.html.leavepolicy.form(leavepolicyform.fill(doc), leavetypes.v.get, positiontypes.v.get))
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leavepolicyform.bindFromRequest.fold(
          formWithError => {
            for { 
              maybe_leavetypes <- KeywordModel.findOne(BSONDocument("n" -> "Leave Type"), request)
              maybe_positiontypes <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
            } yield {
              val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
              val positiontypes = maybe_positiontypes.getOrElse(KeywordModel.doc)
              Ok(views.html.leavepolicy.form(formWithError, leavetypes.v.get, positiontypes.v.get))
            }
          },
          formWithData => {
            for { 
              maybe_unique <- LeavePolicyModel.isUnique(formWithData, request)
              maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt"->formWithData.lt), request)
              maybe_leavetypes <- KeywordModel.findOne(BSONDocument("n" -> "Leave Type"), request)
              maybe_positiontypes <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
              maybe_alert <- AlertUtility.findOne(BSONDocument("k"->1004))
            } yield {
              if (maybe_unique) {
                val eligbleleaveentitlement = LeavePolicyModel.sortEligbleLeaveEntitlement(formWithData, request)
                LeavePolicyModel.insert(
                    formWithData.copy(
                        _id=BSONObjectID.generate, 
                        ent=Entitlement(
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
                    p_request=request
                )
                Redirect(routes.LeaveSettingController.index)
              } else {
                val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
                val positiontypes = maybe_positiontypes.getOrElse(KeywordModel.doc)
                val replaceMap = Map("URL"->(Tools.hostname+"/leavepolicy/view?p_id=" + maybeleavepolicy.get._id.stringify))
                val alert = maybe_alert.getOrElse(null)
                val mod_alert = if(alert!=null){alert.copy(m=Tools.replaceSubString(alert.m, replaceMap.toList))}else{alert}
                Ok(views.html.leavepolicy.form(leavepolicyform.fill(formWithData), leavetypes.v.get, positiontypes.v.get,alert=mod_alert))
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
        maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybe_leavetypes <- KeywordModel.findOne(BSONDocument("n" -> "Leave Type"), request)
        maybe_positiontypes <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
      } yield {
        maybe_leavepolicy.map( leavepolicy  => {
          val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
          val positiontypes = maybe_positiontypes.getOrElse(KeywordModel.doc)
          Ok(views.html.leavepolicy.form(leavepolicyform.fill(leavepolicy), leavetypes.v.get, positiontypes.v.get, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leavepolicyform.bindFromRequest.fold(
          formWithError => {
            for { 
              maybe_leavetypes <- KeywordModel.findOne(BSONDocument("n" -> "Leave Type"), request)
              maybe_positiontypes <- KeywordModel.findOne(BSONDocument("n" -> "Position Type"), request)
            } yield {
              val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
              val positiontypes = maybe_positiontypes.getOrElse(KeywordModel.doc)
              Ok(views.html.leavepolicy.form(formWithError, leavetypes.v.get, positiontypes.v.get))
            }
          },
          formWithData => {
            val eligbleleaveentitlement = LeavePolicyModel.sortEligbleLeaveEntitlement(formWithData, request)
            LeavePolicyModel.update(
                BSONDocument("_id" -> BSONObjectID(p_id)), 
                formWithData.copy(
                    _id=BSONObjectID(p_id),
                    ent=Entitlement(
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
                request
            )
            Future.successful(Redirect(routes.LeaveSettingController.index))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def delete(p_id:String, p_lt:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      // Delete leave policy
      Await.result(LeavePolicyModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      LeaveProfileModel.find(BSONDocument("lt" -> p_lt), request).map { leaveprofiles => 
        leaveprofiles.map { leaveprofile => {
          PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(leaveprofile.pid)), request).map { person =>
            // Delete leave profile
            LeaveProfileModel.remove(BSONDocument("_id" -> leaveprofile._id), request)
            // Lockdown leave
            LeaveModel.setLockDown(BSONDocument("pid" -> leaveprofile.pid, "lt" -> leaveprofile.lt), request)
          }
        } }
      }
      Future.successful(Redirect(routes.LeaveSettingController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  /* deprecated on v1.1
  def getLeaveEntitlement(p_lt:String, p_pt:String) = withAuth { username => implicit request => {
    for {
      maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> p_lt, "pt" -> p_pt), request)
    } yield {
      maybeleavepolicy.map( leavepolicy => {
        val json = Json.parse("{\"e1\":" + leavepolicy.ent.e1 + ", \"e1_s\":" + leavepolicy.ent.e1_s + ", \"e1_cf\":" + leavepolicy.ent.e1_cf +
        	",\"e2\":" + leavepolicy.ent.e2 + ", \"e2_s\":" + leavepolicy.ent.e2_s + ", \"e2_cf\":" + leavepolicy.ent.e2_cf +
        	",\"e3\":" + leavepolicy.ent.e3 + ", \"e3_s\":" + leavepolicy.ent.e3_s + ", \"e3_cf\":" + leavepolicy.ent.e3_cf +
        	",\"e4\":" + leavepolicy.ent.e4 + ", \"e4_s\":" + leavepolicy.ent.e4_s + ", \"e4_cf\":" + leavepolicy.ent.e4_cf +
        	",\"e5\":" + leavepolicy.ent.e5 + ", \"e5_s\":" + leavepolicy.ent.e5_s + ", \"e5_cf\":" + leavepolicy.ent.e5_cf +
        	"}");
        Ok(json).as("application/json")
      }).getOrElse({        
          val json = Json.parse("{\"e1\":0,\"e1_s\":0,\"e1_cf\":0,\"e2\":0,\"e2_s\":0,\"e2_cf\":0,\"e3\":0,\"e3_s\":0,\"e3_cf\":0,\"e4\":0,\"e4_s\":0,\"e4_cf\":0,\"e5\":0,\"e5_s\":0,\"e5_cf\":0}");
          Ok(json).as("application/json")
      })
    }
  }}
  */
  
  def getDayTypeJSON(p_lt:String) = withAuth { username => implicit request => {
    for {
      maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> p_lt), request)
    } yield {
      maybeleavepolicy.map( leavepolicy => {
        val json = Json.parse("{\"daytype\":\"" + leavepolicy.set.dt + "\"}");
        Ok(json).as("application/json")
      }).getOrElse({        
          val json = Json.parse("{\"daytype\":\"error\"}");
          Ok(json).as("application/json")
      })
    }
  } }
  
  def getDayType(p_lt:String) = withAuth { username => implicit request => {
    for {
      maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> p_lt), request)
    } yield {
      render {
         case Accepts.Html() => Ok(views.html.error.unauthorized())
         case Accepts.Json() => {
           maybeleavepolicy.map( leavepolicy => {
             val json = Json.obj("daytype" -> leavepolicy.set.dt)
             Ok(json).as("application/json")
           }).getOrElse({        
             val json = Json.obj("daytype" -> "error")
             Ok(json).as("application/json")
           })
         }
      }
    }
  } }
  
}