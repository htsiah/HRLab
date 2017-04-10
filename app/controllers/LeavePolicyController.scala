package controllers

import scala.concurrent.{Future, Await}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeavePolicyModel, KeywordModel, Keyword, LeavePolicy, LeavePolicySetting, Entitlement, EntitlementValue, LeaveProfileModel, LeaveModel, PersonModel, AuditLogModel}
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
              "e1" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e2" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e3" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e4" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e5" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e6" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e7" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e8" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e9" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e10" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply)
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
      } yield {
        val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
        Ok(views.html.leavepolicy.form(leavepolicyform.fill(doc), leavetypes.v))
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
            } yield {
              val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
              Ok(views.html.leavepolicy.form(formWithError, leavetypes.v))
            }
          },
          formWithData => {
            for { 
              maybe_unique <- LeavePolicyModel.isUnique(formWithData, request)
              maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt"->formWithData.lt), request)
              maybe_leavetypes <- KeywordModel.findOne(BSONDocument("n" -> "Leave Type"), request)
              maybe_alert <- AlertUtility.findOne(BSONDocument("k"->1004))
            } yield {
              if (maybe_unique) {
                val doc_objectID = BSONObjectID.generate
                val eligbleleaveentitlement = LeavePolicyModel.sortEligbleLeaveEntitlement(formWithData, request)
                LeavePolicyModel.insert(
                    formWithData.copy(
                        _id=doc_objectID, 
                        ent=Entitlement(
                            EntitlementValue(s=eligbleleaveentitlement(0)(0), e=eligbleleaveentitlement(0)(1), cf=eligbleleaveentitlement(0)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(1)(0), e=eligbleleaveentitlement(1)(1), cf=eligbleleaveentitlement(1)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(2)(0), e=eligbleleaveentitlement(2)(1), cf=eligbleleaveentitlement(2)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(3)(0), e=eligbleleaveentitlement(3)(1), cf=eligbleleaveentitlement(3)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(4)(0), e=eligbleleaveentitlement(4)(1), cf=eligbleleaveentitlement(4)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(5)(0), e=eligbleleaveentitlement(5)(1), cf=eligbleleaveentitlement(5)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(6)(0), e=eligbleleaveentitlement(6)(1), cf=eligbleleaveentitlement(6)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(7)(0), e=eligbleleaveentitlement(7)(1), cf=eligbleleaveentitlement(7)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(8)(0), e=eligbleleaveentitlement(8)(1), cf=eligbleleaveentitlement(8)(2)),
                            EntitlementValue(s=eligbleleaveentitlement(9)(0), e=eligbleleaveentitlement(9)(1), cf=eligbleleaveentitlement(9)(2))
                        )
                    ),
                    p_request=request
                )
                AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."), p_request=request)
                Redirect(routes.LeaveSettingController.index)
              } else {
                val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
                val replaceMap = Map("URL"->(Tools.hostname+"/leavepolicy/view?p_id=" + maybeleavepolicy.get._id.stringify))
                val alert = maybe_alert.getOrElse(null)
                val mod_alert = if(alert!=null){alert.copy(m=Tools.replaceSubString(alert.m, replaceMap.toList))}else{alert}
                Ok(views.html.leavepolicy.form(leavepolicyform.fill(formWithData), leavetypes.v, alert=mod_alert))
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
      } yield {
        maybe_leavepolicy.map( leavepolicy  => {
          val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
          Ok(views.html.leavepolicy.form(leavepolicyform.fill(leavepolicy), leavetypes.v, p_id))
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
            } yield {
              val leavetypes = maybe_leavetypes.getOrElse(KeywordModel.doc)
              Ok(views.html.leavepolicy.form(formWithError, leavetypes.v))
            }
          },
          formWithData => {
            val eligbleleaveentitlement = LeavePolicyModel.sortEligbleLeaveEntitlement(formWithData, request)
            LeavePolicyModel.update(
                BSONDocument("_id" -> BSONObjectID(p_id)), 
                formWithData.copy(
                    _id=BSONObjectID(p_id),
                    ent=Entitlement(
                        EntitlementValue(s=eligbleleaveentitlement(0)(0), e=eligbleleaveentitlement(0)(1), cf=eligbleleaveentitlement(0)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(1)(0), e=eligbleleaveentitlement(1)(1), cf=eligbleleaveentitlement(1)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(2)(0), e=eligbleleaveentitlement(2)(1), cf=eligbleleaveentitlement(2)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(3)(0), e=eligbleleaveentitlement(3)(1), cf=eligbleleaveentitlement(3)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(4)(0), e=eligbleleaveentitlement(4)(1), cf=eligbleleaveentitlement(4)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(5)(0), e=eligbleleaveentitlement(5)(1), cf=eligbleleaveentitlement(5)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(6)(0), e=eligbleleaveentitlement(6)(1), cf=eligbleleaveentitlement(6)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(7)(0), e=eligbleleaveentitlement(7)(1), cf=eligbleleaveentitlement(7)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(8)(0), e=eligbleleaveentitlement(8)(1), cf=eligbleleaveentitlement(8)(2)),
                        EntitlementValue(s=eligbleleaveentitlement(9)(0), e=eligbleleaveentitlement(9)(1), cf=eligbleleaveentitlement(9)(2))
                    )
                ), 
                request
            )
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)
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
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      LeaveProfileModel.find(BSONDocument("lt" -> p_lt), request).map { leaveprofiles => 
        leaveprofiles.map { leaveprofile => {
          // Delete leave profile          
          LeaveProfileModel.remove(BSONDocument("_id" -> leaveprofile._id), request)
          AuditLogModel.remove(BSONDocument("lk"->leaveprofile._id), request)
          // Lockdown leave
          LeaveModel.setLockDown(BSONDocument("pid" -> leaveprofile.pid, "lt" -> leaveprofile.lt), request)
        } }
      }
      Future.successful(Redirect(routes.LeaveSettingController.index))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
    
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
  
  def getSupportingDocument(p_lt:String) = withAuth { username => implicit request => {
    for {
      maybeleavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> p_lt), request)
    } yield {
      render {
         case Accepts.Html() => Ok(views.html.error.unauthorized())
         case Accepts.Json() => {
           maybeleavepolicy.map( leavepolicy => {
             val json = Json.obj("supportingdocument" -> leavepolicy.set.msd)
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