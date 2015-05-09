package jobs

import scala.util.{Success, Failure,Try}
import org.joda.time.DateTime
import akka.actor.ActorSystem

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import models.{CompanyModel, LeaveSettingModel, LeavePolicyModel, LeaveProfileModel, LeaveProfile, LeaveSetting, LeavePolicy, PersonModel}

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
  
  private val dbname = Play.current.configuration.getString("mongodb_job").getOrElse("job")
  private val uri = Play.current.configuration.getString("mongodb_job_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver(ActorSystem("DefaultMongoDbDriver"))
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("monthlyleaveprofileupdatelog")
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
    col.find(p_query).cursor[MonthlyLeaveProfileUpdateLog].collect[List]()
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
    
    LeavePolicyModel.find(BSONDocument("sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leavepolicies => {
      leavepolicies.map { leavepolicy => {
        
        // Only process accumulation is monthly and has carry expired
        if(leavepolicy.acc=="Monthly" || leavepolicy.cexp>0) {
          PersonModel.findOne(BSONDocument("p.pt"->leavepolicy.pt, "sys.eid"->p_eid)).map { person => {
            LeaveProfileModel.find(BSONDocument("lt"->leavepolicy.lt ,"pid"->person.get._id.stringify, "sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leaveprofiles => {
              leaveprofiles.map { leaveprofile => {
                LeaveProfileModel.update(
                    BSONDocument("_id" -> leaveprofile._id), 
                    leaveprofile.copy(
                        ear=leaveprofile.ear + LeaveProfileModel.getMonthEntitlementEarn(leaveprofile, leavepolicy, p_leavesetting, person.get, now.monthOfYear().get), 
                        cfexp=LeaveProfileModel.getEligibleCarryForwardExpired(leaveprofile, p_leavesetting, leavepolicy)
                    ), 
                    p_eid)
              } }
            }}
          }}
        }
        
      } }
    } }
  }
  
  private def yearlycut0ff(p_eid: String, p_leavesetting: LeaveSetting) = {
    val now = new DateTime
    LeavePolicyModel.find(BSONDocument("sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leavepolicies => {
      leavepolicies.map { leavepolicy => {
        PersonModel.findOne(BSONDocument("p.pt"->leavepolicy.pt, "sys.eid"->p_eid)).map { person => {
          LeaveProfileModel.findOne(BSONDocument("lt"->leavepolicy.lt ,"pid"->person.get._id.stringify, "sys.eid"->p_eid, "sys.ddat"->BSONDocument("$exists"->false))).map { leaveprofiles => {
            leaveprofiles.map { leaveprofile => {
            LeaveProfileModel.update(
                BSONDocument("_id" -> leaveprofile._id), 
                leaveprofile.copy(
                    ent=LeaveProfileModel.getEligibleEntitlement(leaveprofile, PersonModel.getServiceMonths(person.get)),
                    ear=LeaveProfileModel.getMonthEntitlementEarn(leaveprofile, leavepolicy, p_leavesetting, person.get, now.monthOfYear().get), 
                    adj=0,
                    uti=0.0,
                    cf=LeaveProfileModel.getEligibleCarryForwordEarn(leaveprofile, PersonModel.getServiceMonths(person.get)),
                    cfuti=0.0,
                    cfexp=0.0
                ), 
                p_eid)
            } }
          }}
        }}
      } }
    } }
  }
    
}