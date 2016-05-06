package jobs

import scala.util.{Success, Failure}
import org.joda.time.DateTime

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import models.{CompanyModel, LeaveSettingModel, LeavePolicyModel, LeaveProfileModel, LeaveModel, LeaveProfile, LeaveSetting, LeavePolicy, PersonModel}
import utilities.DbConnUtility

case class MonthlyLeaveProfileUpdateLog (
    _id: BSONObjectID,
    month: String,
    year: String
)

object MonthlyLeaveProfileUpdateJob {
  
  // Use Reader to deserialize document automatically
  implicit object MonthlyLeaveProfileUpdateLogBSONReader extends BSONDocumentReader[MonthlyLeaveProfileUpdateLog] {
    def read(doc: BSONDocument): MonthlyLeaveProfileUpdateLog = {
      MonthlyLeaveProfileUpdateLog(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("month").get,
          doc.getAs[String]("year").get
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object MonthlyLeaveProfileUpdateLogBSONWriter extends BSONDocumentWriter[MonthlyLeaveProfileUpdateLog] {
    def write(monthlyleaveprofileupdatelog: MonthlyLeaveProfileUpdateLog): BSONDocument = {
      BSONDocument(
          "_id" -> monthlyleaveprofileupdatelog._id,
          "month" -> monthlyleaveprofileupdatelog.month,
          "year" -> monthlyleaveprofileupdatelog.year
      )     
    }
  }
  
  private val col = DbConnUtility.job_db.collection("monthlyleaveprofileupdatelog")
  
  val doc = MonthlyLeaveProfileUpdateLog(
      _id = BSONObjectID.generate,
      month = "",
      year = ""
  )
  
  private def insert(p_doc:MonthlyLeaveProfileUpdateLog)= {
    val future = col.insert(p_doc)
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  private def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[MonthlyLeaveProfileUpdateLog](ReadPreference.primary).collect[List]()
  }
  
  private def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[MonthlyLeaveProfileUpdateLog]
  }
  
  def run = {   
    val job_start = new DateTime
    val now = new DateTime
    val today = now.dayOfMonth()
    val thismonth = now.monthOfYear()
    val nextmonth = now.plusMonths(1).monthOfYear()
    val previousmonth = now.minusMonths(1).monthOfYear()
    val thisyear = now.year().get()
    
    this.findOne(BSONDocument("month"->thismonth.getAsShortText(), "year"-> thisyear.toString)).map { upload_log => {
      if (!(upload_log.isDefined)) {
        CompanyModel.find(BSONDocument("sys.ddat"->BSONDocument("$exists"->false))).map { companies => {
          companies.map { company => {
            LeaveSettingModel.findOne(BSONDocument("sys.eid" -> company.sys.get.eid.get)).map { leavesetting => {
              if (leavesetting.get.cfm==thismonth.get) {
                LeaveModel.setLockDown(BSONDocument("sys.eid" -> company.sys.get.eid.get))
                this.yearlycut0ff(company.sys.get.eid.get, leavesetting.get)
              } else {
                this.monthlyaccumulation(company.sys.get.eid.get, leavesetting.get)
              }
            } }
          } }
          this.insert(MonthlyLeaveProfileUpdateLog(BSONObjectID.generate, thismonth.getAsShortText(), thisyear.toString))
        } }
      }
    } }

    val job_end = new DateTime
    Job.insert(Job(BSONObjectID.generate, "MonthlyLeaveProfileUpdateJob", Some(job_start), Some(job_end)))
  }
  
  private def monthlyaccumulation(p_eid:String, p_leavesetting: LeaveSetting) = {
    val now = new DateTime
    val cutoffdate = LeaveSettingModel.getCutOffDate(p_leavesetting.cfm)
    
    LeavePolicyModel.find(BSONDocument("sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leavepolicies => {
      leavepolicies.map { leavepolicy => {
        
        // Only process accumulation is monthly and has carry expired
        if(leavepolicy.set.acc=="Monthly - utilisation based on earned" || leavepolicy.set.acc=="Monthly - utilisation based on closing balance" || leavepolicy.set.cexp>0) {
          PersonModel.find(BSONDocument("sys.eid"->p_eid)).map { persons => {
            persons.map { person => {
              LeaveProfileModel.find(BSONDocument("lt"->leavepolicy.lt ,"pid"->person._id.stringify, "sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leaveprofiles => {
                leaveprofiles.map { leaveprofile => {
                  LeaveProfileModel.update(
                      BSONDocument("_id" -> leaveprofile._id), 
                      leaveprofile.copy(
                          cal = leaveprofile.cal.copy(
                              cfexp=LeaveProfileModel.getEligibleCarryForwardExpired(leaveprofile, p_leavesetting, leavepolicy)
                          )
                      ), 
                      p_eid
                  )
                }}
              }}
            }}
          }}
        }
        
      }}
    }}
  }
  
  private def yearlycut0ff(p_eid: String, p_leavesetting: LeaveSetting) = {
    val now = new DateTime
    val cutoffdate = LeaveSettingModel.getCutOffDate(p_leavesetting.cfm)
    
    LeavePolicyModel.find(BSONDocument("sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leavepolicies => {
      leavepolicies.map { leavepolicy => {
        PersonModel.find(BSONDocument("sys.eid"->p_eid)).map { persons => {
          persons.map { person => {
            LeaveProfileModel.findOne(BSONDocument("lt"->leavepolicy.lt ,"pid"->person._id.stringify, "sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leaveprofiles => { 
              leaveprofiles.map { leaveprofile => {
                  LeaveProfileModel.update(
                      BSONDocument("_id" -> leaveprofile._id), 
                      leaveprofile.copy(
                          cal = leaveprofile.cal.copy(
                              adj=0,
                              uti=0.0,
                              cf=(leaveprofile.cal.cf - leaveprofile.cal.cfexp - leaveprofile.cal.cfuti) + LeaveProfileModel.getEligibleCarryForwordEarn(leaveprofile, PersonModel.getServiceMonths(person)),
                              cfuti=0.0,
                              cfexp=0.0,
                              papr=0.0
                          )
                      ), 
                      p_eid
                  )
              }}
            }}
          }}
        }}
      }}
    }}
  }
  
}