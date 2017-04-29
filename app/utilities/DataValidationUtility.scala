package utilities

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.{Await}

import models.OfficeModel

import reactivemongo.bson.{BSONDocument}

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object DataValidationUtility {
  
  def isValidEmail (p_value:String) : Boolean = {
    val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    
    p_value match{
      case null                                           => false
      case e if e.trim.isEmpty                            => false
      case e if emailRegex.findFirstMatchIn(e).isDefined  => true
      case _                                              => false
    }
  }
  
  def isValidGender (p_value:String) : Boolean = {
    if (p_value == "Male" || p_value =="Female" ) { true } else { false }    
  }
  
  def isValidMaritalStatus (p_value:String) : Boolean = {
    if (p_value == "Single" || p_value =="Married" ) { true } else { false } 
  }
  
  def isOfficeExist (p_value:String, p_request:RequestHeader) : Boolean = {    
    val office = Await.result(OfficeModel.findOne(BSONDocument("n" -> p_value), p_request), Tools.db_timeout)
    if ( office.isDefined ) { true } else { false }
  }
  
  // Format is d-MMM-YYYY
  // Ref: http://stackoverflow.com/questions/14194290/validating-a-date-in-java
  // Ref: http://stackoverflow.com/questions/31463243/regex-for-validating-date-in-dd-mmm-yyyy-format
  def isValidDate (p_value:String) : Boolean = {
    try {
      val dateRegex = """^[0-3][0-9]-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-\d{4}$""".r
      if (dateRegex.findFirstMatchIn(p_value).isDefined) {
        val dtf = DateTimeFormat.forPattern("d-MMM-yyyy");
        val fdat = Some(new DateTime(dtf.parseLocalDate(p_value).toDateTimeAtStartOfDay()))
        return true
      } else {
        return false
      }    
    } catch {
      case e: Exception => {
        return false
      }
    }
  }
  
  def isValidYesNo (p_value:String) : Boolean = {
    if ( p_value == "yes" || p_value =="no" ) { true } else { false }
  }
  
}