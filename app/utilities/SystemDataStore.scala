package utilities

import play.api.mvc._

import models.{Authentication,Person}

import org.joda.time.DateTime

case class System (
  eid: Option[String],
  cdat: Option[DateTime],
  mdat: Option[DateTime],
  mby: Option[String],
  ddat: Option[DateTime],
  dby: Option[String],
  ll: Option[DateTime]
)

object SystemDataStore {
    
  def creation(p_eid:String="", p_request:RequestHeader=null) = {
    if (p_request != null && !(p_request.session.isEmpty)) {
      Some(System(
          eid=p_request.session.get("entity"),
          cdat=Some(new DateTime()),
          mdat=Some(new DateTime()),
          mby=p_request.session.get("username"),
          ddat=None,
          dby=None,
          ll=None
      ))
    } else {
      Some(System(
          eid=Some(p_eid),
          cdat=Some(new DateTime()),
          mdat=Some(new DateTime()),
          mby=Some("anonymous"),
          ddat=None,
          dby=None,
          ll=None
      ))
    }
  }
    
  def modifyWithSystem(p_sys:System, p_request:RequestHeader=null) = {
    val sys = if (p_request != null && !(p_request.session.isEmpty)) {
      p_sys.copy(
          mdat=Some(new DateTime()),
          mby=p_request.session.get("username")
      )
    } else {
      p_sys.copy(
          mdat=Some(new DateTime()),
          mby=Some("anonymous")
      )
    }    
    Some(sys)
  }
  
  def setDeletionFlag(p_sys:System, p_request:RequestHeader=null) = {
    val sys = if (p_request != null && !(p_request.session.isEmpty)) {
      p_sys.copy(
          ddat=Some(new DateTime()),
          dby=p_request.session.get("username"),
          mdat=Some(new DateTime()),
          mby=p_request.session.get("username")
      )
    } else {
      p_sys.copy(
          ddat=Some(new DateTime()),
          dby=Some("anonymous"),
          mdat=Some(new DateTime()),
          mby=Some("anonymous")
      )
    }    
    Some(sys)
  }
  
  def logonWithSystem(p_sys:System) = {
    Some(p_sys.copy(ll=Some(new DateTime())))
  }
  
}