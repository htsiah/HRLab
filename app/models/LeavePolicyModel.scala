package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import scala.collection.mutable.ArrayBuffer
import org.joda.time.DateTime

case class LeavePolicy (
    _id: BSONObjectID,
    lt: String,
    set: LeavePolicySetting,
    ent: Entitlement,
    sys: Option[System]
)

case class LeavePolicySetting (
    g: String,
    acc: String,
    ms: String,
    dt: String,
    nwd: Boolean,
    cexp: Int,
    scal: Boolean,
    msd: Boolean
)

case class EntitlementValue (
    e: Int,
    s: Int,
    cf: Int
)

case class Entitlement (
    e1: EntitlementValue,
    e2: EntitlementValue,
    e3: EntitlementValue,
    e4: EntitlementValue,
    e5: EntitlementValue, 
    e6: EntitlementValue,
    e7: EntitlementValue,
    e8: EntitlementValue,
    e9: EntitlementValue,
    e10: EntitlementValue
)

object LeavePolicyModel {

  // Use Reader to deserialize document automatically
  implicit object SystemBSONReader extends BSONDocumentReader[System] {
    def read(p_doc: BSONDocument): System = {
      System(
          p_doc.getAs[String]("eid").map(v => v),
          p_doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("mby").map(v => v),
          p_doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("dby").map(v => v),
          p_doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object LeavePolicySettingBSONReader extends BSONDocumentReader[LeavePolicySetting] {
    def read(p_doc: BSONDocument): LeavePolicySetting = {
      LeavePolicySetting(
          p_doc.getAs[String]("g").get,
          p_doc.getAs[String]("acc").get,
          p_doc.getAs[String]("ms").get,
          p_doc.getAs[String]("dt").get,
          p_doc.getAs[Boolean]("nwd").get,
          p_doc.getAs[Int]("cexp").get,
          p_doc.getAs[Boolean]("scal").get,
          p_doc.getAs[Boolean]("msd").getOrElse(false)
      )
    }
  }
  
  implicit object EntitlementValueBSONReader extends BSONDocumentReader[EntitlementValue] {
    def read(p_doc: BSONDocument): EntitlementValue = {
      EntitlementValue(
          p_doc.getAs[Int]("e").get,
          p_doc.getAs[Int]("s").get,
          p_doc.getAs[Int]("cf").get
      )
    }
  }
    
  implicit object EntitlementBSONReader extends BSONDocumentReader[Entitlement] {
    def read(p_doc: BSONDocument): Entitlement = {
      Entitlement(
          p_doc.getAs[EntitlementValue]("e1").get,
          p_doc.getAs[EntitlementValue]("e2").get,
          p_doc.getAs[EntitlementValue]("e3").get,
          p_doc.getAs[EntitlementValue]("e4").get,
          p_doc.getAs[EntitlementValue]("e5").get,
          p_doc.getAs[EntitlementValue]("e6").get,
          p_doc.getAs[EntitlementValue]("e7").get,
          p_doc.getAs[EntitlementValue]("e8").get,
          p_doc.getAs[EntitlementValue]("e9").get,
          p_doc.getAs[EntitlementValue]("e10").get
      )
    }
  }
  
  implicit object LeavePolicyBSONReader extends BSONDocumentReader[LeavePolicy] {
    def read(p_doc: BSONDocument): LeavePolicy = {
      LeavePolicy(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("lt").get,
          p_doc.getAs[LeavePolicySetting]("set").get,
          p_doc.getAs[Entitlement]("ent").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(p_doc: System): BSONDocument = {
      BSONDocument(
          "eid" -> p_doc.eid,
          "cdat" -> p_doc.cdat.map(date => BSONDateTime(date.getMillis)),
          "mdat" -> p_doc.mdat.map(date => BSONDateTime(date.getMillis)),
          "mby" -> p_doc.mby,
          "ddat" -> p_doc.ddat.map(date => BSONDateTime(date.getMillis)),
          "dby" -> p_doc.dby,
          "ll" -> p_doc.ll.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
    
  implicit object LeavePolicySettingBSONWriter extends BSONDocumentWriter[LeavePolicySetting] {
    def write(p_doc: LeavePolicySetting): BSONDocument = {
      BSONDocument(
          "g" -> p_doc.g,
          "acc" -> p_doc.acc,
          "ms" -> p_doc.ms,
          "dt" -> p_doc.dt,
          "nwd" -> p_doc.nwd,
          "cexp" -> p_doc.cexp,
          "scal" -> p_doc.scal,
          "msd" -> p_doc.msd
      )     
    }
  }
  
  implicit object EntitlementValueBSONWriter extends BSONDocumentWriter[EntitlementValue] {
    def write(p_doc: EntitlementValue): BSONDocument = {
      BSONDocument(
          "e" -> p_doc.e,
          "s" -> p_doc.s,
          "cf" -> p_doc.cf
      )     
    }
  }
  
  implicit object EntitlementBSONWriter extends BSONDocumentWriter[Entitlement] {
    def write(p_doc: Entitlement): BSONDocument = {
      BSONDocument(
          "e1" -> p_doc.e1,
          "e2" -> p_doc.e2,
          "e3" -> p_doc.e3,
          "e4" -> p_doc.e4,
          "e5" -> p_doc.e5,
          "e6" -> p_doc.e6,
          "e7" -> p_doc.e7,
          "e8" -> p_doc.e8,
          "e9" -> p_doc.e9,
          "e10" -> p_doc.e10
      )     
    }
  }
  
  implicit object LeavePolicyBSONWriter extends BSONDocumentWriter[LeavePolicy] {
    def write(p_doc: LeavePolicy): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "lt" -> p_doc.lt,
          "set" -> p_doc.set,
          "ent" -> p_doc.ent,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.leave_db.collection("leavepolicy")
  
  val doc = LeavePolicy(
      _id = BSONObjectID.generate,
      lt = "",
      LeavePolicySetting(g = "", acc = "", ms = "", dt = "", nwd = false, cexp = 0, scal = true, msd = false),
      Entitlement(
          e1=EntitlementValue(e=0, s=0, cf=0),
          e2=EntitlementValue(e=0, s=0, cf=0),
          e3=EntitlementValue(e=0, s=0, cf=0),
          e4=EntitlementValue(e=0, s=0, cf=0),
          e5=EntitlementValue(e=0, s=0, cf=0),
          e6=EntitlementValue(e=0, s=0, cf=0),
          e7=EntitlementValue(e=0, s=0, cf=0),
          e8=EntitlementValue(e=0, s=0, cf=0),
          e9=EntitlementValue(e=0, s=0, cf=0),
          e10=EntitlementValue(e=0, s=0, cf=0)  
      ),
      sys=None
  )
  
  private def updateSystem(p_doc:LeavePolicy) = {
    val eid = p_doc.sys.get.eid.getOrElse(None)
    val cdat = p_doc.sys.get.cdat.getOrElse(None)
    val mdat = p_doc.sys.get.mdat.getOrElse(None)
    val mby = p_doc.sys.get.mby.getOrElse(None)
    val ddat = p_doc.sys.get.ddat.getOrElse(None)
    val dby = p_doc.sys.get.dby.getOrElse(None)
    val ll = p_doc.sys.get.ll.getOrElse(None)
    val sys_doc = System(
        eid = if (eid!=None) {Some(p_doc.sys.get.eid.get)} else {None},
        cdat = if (cdat!=None) {Some(p_doc.sys.get.cdat.get)} else {None},
        mdat = if (mdat!=None) {Some(p_doc.sys.get.mdat.get)} else {None},
        mby = if (mby!=None) {Some(p_doc.sys.get.mby.get)} else {None},
        ddat = if (ddat!=None) {Some(p_doc.sys.get.ddat.get)} else {None},
        dby = if (dby!=None) {Some(p_doc.sys.get.dby.get)} else {None},
        ll= if (ll!=None) {Some(p_doc.sys.get.ll.get)} else {None}
    ) 
    sys_doc
  }
      
  // Insert new document
  def insert(p_doc:LeavePolicy, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
	
  // Update document
  def update(p_query:BSONDocument,p_doc:LeavePolicy,p_request:RequestHeader) = {
    for { 
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("_id" -> p_doc._id), p_request)
    } yield {
      val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          
          // Update employee's leave profile
          if (maybe_leavepolicy.get.set.acc != p_doc.set.acc) {
            val gender = if (p_doc.set.g == "Applicable for all") {
              BSONDocument("$or" -> BSONArray(BSONDocument("p.g"->"Female"),BSONDocument("p.g"->"Male")))  
            } else if (p_doc.set.g == "Female only") {
              BSONDocument("p.g"->"Female")
            } else {
              BSONDocument("p.g"->"Male")
            }
            val marital = if (p_doc.set.ms == "Applicable for all"){
              BSONDocument("$or" -> BSONArray(BSONDocument("p.ms"->"Single"),BSONDocument("p.ms"->"Married")))
            } else if (p_doc.set.ms == "Single only") {
              BSONDocument("p.ms"->"Single")
            } else {
              BSONDocument("p.ms"->"Married")
            }
            PersonModel.find((gender)++(marital), p_request).map { persons => 
              persons.map { person => {
                LeaveProfileModel.find(BSONDocument("pid" -> person._id.stringify, "lt" -> p_doc.lt)).map { leaveprofiles =>  
                  leaveprofiles.map { leaveprofile => {
                    LeaveProfileModel.update(BSONDocument("_id" -> leaveprofile._id), leaveprofile, p_request)
                  } }
                }
              } }
            }
          }
          
        }
      } 
    }
  }    
            
  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.foreach { doc => 
        val future = col.update(BSONDocument("_id" -> doc._id, "sys.ddat"->BSONDocument("$exists"->false)), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
        }
      }
    }
  }
	
  // Delete document
  def removePermanently(p_query:BSONDocument) = {
    val future = col.remove(p_query)
  }
	
  // Find all documents
  def find(p_query:BSONDocument=BSONDocument()) = {
    col.find(p_query).cursor[LeavePolicy](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[LeavePolicy](ReadPreference.primary).collect[List]()
  }
	
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[LeavePolicy]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[LeavePolicy]
  }
  
  /** Custom Model Methods **/ 
  def isUnique(p_doc:LeavePolicy, p_request:RequestHeader) = {
    for{
      maybe_leavepolicy <- this.findOne(BSONDocument("lt"->p_doc.lt), p_request)
    } yield {
      if (maybe_leavepolicy.isEmpty) true else false
    }
  }
  
  def isAvailable(p_lt:String, p_g: String, p_ms: String, p_request:RequestHeader) = {
    for{
      maybe_leavetypes <- this.find(
        BSONDocument(
            "lt" -> p_lt,
            "$or" -> BSONArray(
                BSONDocument("set.g"->p_g),
                BSONDocument("set.g"->"Applicable for all")
            ),
            "$or" -> BSONArray(
                BSONDocument("set.ms"->p_ms),
                BSONDocument("set.ms"->"Applicable for all")
            )
        ),
        p_request
      )
    } yield {
      if (maybe_leavetypes.isEmpty) false else true
    }
  }
  
  def getLeavePolicies(p_g: String, p_ms: String, p_request:RequestHeader) = { 
    for {
      maybe_leavetypes <- this.find(
        BSONDocument(
            "$or" -> BSONArray(
                BSONDocument("set.g"->p_g),
                BSONDocument("set.g"->"Applicable for all")
            ),
            "$or" -> BSONArray(
                BSONDocument("set.ms"->p_ms),
                BSONDocument("set.ms"->"Applicable for all")
            )
        ),
        p_request
      )
    } yield {
      var typesList = List[String]()
      maybe_leavetypes.foreach(leavetype => typesList = typesList :+ leavetype.lt)
      typesList
    }
  }
  
  def sortEligbleLeaveEntitlement(p_doc:LeavePolicy, p_request:RequestHeader) ={
        
    var eligbleleaveentitlement = ArrayBuffer.fill(10,3)(0)
    
    // Replace 0 value to 1000
    if (p_doc.ent.e1.s == 0) {
      eligbleleaveentitlement(0) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(0) = ArrayBuffer(p_doc.ent.e1.s, p_doc.ent.e1.e, p_doc.ent.e1.cf)
    }

    if (p_doc.ent.e2.s == 0) {
      eligbleleaveentitlement(1) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(1) = ArrayBuffer(p_doc.ent.e2.s, p_doc.ent.e2.e, p_doc.ent.e2.cf)
    }
    
    if (p_doc.ent.e3.s == 0) {
      eligbleleaveentitlement(2) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(2) = ArrayBuffer(p_doc.ent.e3.s, p_doc.ent.e3.e, p_doc.ent.e3.cf)
    }
    
    if (p_doc.ent.e4.s == 0) {
    	eligbleleaveentitlement(3) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(3) = ArrayBuffer(p_doc.ent.e4.s, p_doc.ent.e4.e, p_doc.ent.e4.cf)
    }
    
    if (p_doc.ent.e5.s == 0) {
      eligbleleaveentitlement(4) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(4) = ArrayBuffer(p_doc.ent.e5.s, p_doc.ent.e5.e, p_doc.ent.e5.cf)
    }
    
    if (p_doc.ent.e6.s == 0) {
      eligbleleaveentitlement(5) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(5) = ArrayBuffer(p_doc.ent.e6.s, p_doc.ent.e6.e, p_doc.ent.e6.cf)
    }
    
    if (p_doc.ent.e7.s == 0) {
      eligbleleaveentitlement(6) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(6) = ArrayBuffer(p_doc.ent.e7.s, p_doc.ent.e7.e, p_doc.ent.e7.cf)
    }
    
    if (p_doc.ent.e8.s == 0) {
      eligbleleaveentitlement(7) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(7) = ArrayBuffer(p_doc.ent.e8.s, p_doc.ent.e8.e, p_doc.ent.e8.cf)
    }
    
    if (p_doc.ent.e9.s == 0) {
      eligbleleaveentitlement(8) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(8) = ArrayBuffer(p_doc.ent.e9.s, p_doc.ent.e9.e, p_doc.ent.e9.cf)
    }
    
    if (p_doc.ent.e10.s == 0) {
      eligbleleaveentitlement(9) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(9) = ArrayBuffer(p_doc.ent.e10.s, p_doc.ent.e10.e, p_doc.ent.e10.cf)
    }
    
    val eligbleleaveentitlementsorted = eligbleleaveentitlement.sortBy(_(0))
    var eligbleleaveentitlementsorted_update = ArrayBuffer.fill(10,3)(0)
    
    // Replace 1000 back to 0
    for (i <- 0 to 9) {
      if (eligbleleaveentitlementsorted(i)(0) == 1000) {
        eligbleleaveentitlementsorted_update(i) = ArrayBuffer(0, 0, 0)
      } else {
        eligbleleaveentitlementsorted_update(i) = ArrayBuffer(eligbleleaveentitlementsorted(i)(0), eligbleleaveentitlementsorted(i)(1), eligbleleaveentitlementsorted(i)(2))
      }
    }
    
    eligbleleaveentitlementsorted_update
  }
  
}