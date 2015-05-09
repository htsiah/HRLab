package models

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore}

import scala.util.{Success, Failure,Try}
import scala.collection.mutable.ArrayBuffer
import org.joda.time.DateTime
import org.joda.time.Months
import akka.actor.ActorSystem

case class LeaveProfile (
    _id: BSONObjectID,
    pid: String,	// person object id
    pn: String,
    lt: String,
    ent: Int,
    ear: Double,
    adj: Int,
    uti: Double,
    cf: Double,
    cfuti: Double,
    cfexp: Double,
    bal: Double,
    me: LeaveProfileMonthEarn,
    set_ent: Entitlement, // Case class from leave policy model
    sys: Option[System]
)

case class LeaveProfileMonthEarn (
    jan: Double,
    feb: Double,
    mar: Double,
    apr: Double,
    may: Double,
    jun: Double,
    jul: Double,
    aug: Double,
    sep: Double,
    oct: Double,
    nov: Double,
    dec: Double
)
    
object LeaveProfileModel {

    // Use Reader to deserialize document automatically
  implicit object SystemBSONReader extends BSONDocumentReader[System] {
    def read(doc: BSONDocument): System = {
      System(
          doc.getAs[String]("eid").map(v => v),
          doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          doc.getAs[String]("mby").map(v => v),
          doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          doc.getAs[String]("dby").map(v => v),
          doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object LeaveProfileMonthEarnBSONReader extends BSONDocumentReader[LeaveProfileMonthEarn] {
    def read(doc: BSONDocument): LeaveProfileMonthEarn = {
      LeaveProfileMonthEarn(
          doc.getAs[Double]("jan").get,
          doc.getAs[Double]("feb").get,
          doc.getAs[Double]("mar").get,
          doc.getAs[Double]("apr").get,
          doc.getAs[Double]("may").get,
          doc.getAs[Double]("jun").get,
          doc.getAs[Double]("jul").get,
          doc.getAs[Double]("aug").get,
          doc.getAs[Double]("sep").get,
          doc.getAs[Double]("oct").get,
          doc.getAs[Double]("nov").get,
          doc.getAs[Double]("dec").get
      )
    }
  }
  
  implicit object EntitlementBSONReader extends BSONDocumentReader[Entitlement] {
    def read(doc: BSONDocument): Entitlement = {
      Entitlement(
          doc.getAs[Int]("e1").get,
          doc.getAs[Int]("e1_s").get,
          doc.getAs[Int]("e1_cf").get,
          doc.getAs[Int]("e2").get,
          doc.getAs[Int]("e2_s").get,
          doc.getAs[Int]("e2_cf").get,
          doc.getAs[Int]("e3").get,
          doc.getAs[Int]("e3_s").get,
          doc.getAs[Int]("e3_cf").get,
          doc.getAs[Int]("e4").get,
          doc.getAs[Int]("e4_s").get,
          doc.getAs[Int]("e4_cf").get,
          doc.getAs[Int]("e5").get,
          doc.getAs[Int]("e5_s").get,
          doc.getAs[Int]("e5_cf").get
      )
    }
  }
  
  implicit object LeaveProfileBSONReader extends BSONDocumentReader[LeaveProfile] {
    def read(doc: BSONDocument): LeaveProfile = {
      LeaveProfile(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("pid").get,
          doc.getAs[String]("pn").get,
          doc.getAs[String]("lt").get,
          doc.getAs[Int]("ent").get,
          doc.getAs[Double]("ear").get,
          doc.getAs[Int]("adj").get,
          doc.getAs[Double]("uti").get,
          doc.getAs[Double]("cf").get,
          doc.getAs[Double]("cfuti").get,
          doc.getAs[Double]("cfexp").get,
          doc.getAs[Double]("bal").get,
          doc.getAs[LeaveProfileMonthEarn]("me").get,
          doc.getAs[Entitlement]("set_ent").get,
          doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(system: System): BSONDocument = {
      BSONDocument(
          "eid" -> system.eid,
          "cdat" -> system.cdat.map(date => BSONDateTime(date.getMillis)),
          "mdat" -> system.mdat.map(date => BSONDateTime(date.getMillis)),
          "mby" -> system.mby,
          "ddat" -> system.ddat.map(date => BSONDateTime(date.getMillis)),
          "dby" -> system.dby,
          "ll" -> system.ll.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  implicit object EntitlementBSONWriter extends BSONDocumentWriter[Entitlement] {
    def write(entitlement: Entitlement): BSONDocument = {
      BSONDocument(
          "e1" -> entitlement.e1,
          "e1_s" -> entitlement.e1_s,
          "e1_cf" -> entitlement.e1_cf,
          "e2" -> entitlement.e2,
          "e2_s" -> entitlement.e2_s,
          "e2_cf" -> entitlement.e2_cf,
          "e3" -> entitlement.e3,
          "e3_s" -> entitlement.e3_s,
          "e3_cf" -> entitlement.e3_cf,
          "e4" -> entitlement.e4,
          "e4_s" -> entitlement.e4_s,
          "e4_cf" -> entitlement.e4_cf,
          "e5" -> entitlement.e5,
          "e5_s" -> entitlement.e5_s,
          "e5_cf" -> entitlement.e5_cf
      )     
    }
  }

  implicit object LeaveProfileMonthEarnBSONWriter extends BSONDocumentWriter[LeaveProfileMonthEarn] {
    def write(leaveprofilemonthearn: LeaveProfileMonthEarn): BSONDocument = {
      BSONDocument(
          "jan" -> leaveprofilemonthearn.jan,
          "feb" -> leaveprofilemonthearn.feb,
          "mar" -> leaveprofilemonthearn.mar,
          "apr" -> leaveprofilemonthearn.apr,
          "may" -> leaveprofilemonthearn.may,
          "jun" -> leaveprofilemonthearn.jun,
          "jul" -> leaveprofilemonthearn.jul,
          "aug" -> leaveprofilemonthearn.aug,
          "sep" -> leaveprofilemonthearn.sep,
          "oct" -> leaveprofilemonthearn.oct,
          "nov" -> leaveprofilemonthearn.nov,
          "dec" -> leaveprofilemonthearn.dec
      )     
    }
  }
      
  implicit object LeaveProfileBSONWriter extends BSONDocumentWriter[LeaveProfile] {
    def write(leaveprofile: LeaveProfile): BSONDocument = {
      BSONDocument(
          "_id" -> leaveprofile._id,
          "pid" -> leaveprofile.pid,
          "pn" -> leaveprofile.pn,
          "lt" -> leaveprofile.lt,
          "ent" -> leaveprofile.ent,
          "ear" -> leaveprofile.ear,
          "adj" -> leaveprofile.adj,
          "uti" -> leaveprofile.uti,
          "cf" -> leaveprofile.cf,
          "cfuti" -> leaveprofile.cfuti,
          "cfexp" -> leaveprofile.cfexp,
          "bal" -> leaveprofile.bal,
          "me" -> leaveprofile.me,
          "set_ent" -> leaveprofile.set_ent,
          "sys" -> leaveprofile.sys
      )     
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_leave").getOrElse("leave")
  private val uri = Play.current.configuration.getString("mongodb_leave_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver(ActorSystem("DefaultMongoDbDriver"))
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("leaveprofile")
  val doc = LeaveProfile(
      _id = BSONObjectID.generate,
      pid = "",
      pn = "",
      lt = "",
      ent = 0,
      ear = 0,
      adj = 0,
      uti = 0,
      cf = 0,
      cfuti = 0,
      cfexp = 0,
      bal = 0,
      me = LeaveProfileMonthEarn(jan=0.0, feb=0.0, mar=0.0, apr=0.0, may=0.0, jun=0.0, jul=0.0, aug=0.0, sep = 0.0, oct=0.0, nov=0.0, dec=0.0),
      set_ent = Entitlement(e1=0, e1_s=0, e1_cf=0, e2=0, e2_s=0, e2_cf=0, e3=0, e3_s=0, e3_cf=0, e4=0, e4_s=0, e4_cf=0, e5=0, e5_s=0, e5_cf=0),
      sys=None
  )
  
  private def updateSystem(p_doc:LeaveProfile) = {
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
  
  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.map { doc => 
        val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
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
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[LeaveProfile].collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[LeaveProfile].collect[List]()
  }
	
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[LeaveProfile]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[LeaveProfile]
  }
  
  /** Custom Model Methods **/ 
  
  // Insert new document
  def insert(p_doc:LeaveProfile, p_eid:String="", p_request:RequestHeader=null)= {
    for {
      maybe_person <- if (p_eid=="") PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_doc.pid)), p_request) else PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_doc.pid), "sys.eid" -> p_eid))
      maybe_leavepolicy <- if (p_eid=="") LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt ,"pt"->maybe_person.get.p.pt), p_request) else LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt ,"pt"->maybe_person.get.p.pt, "sys.eid" -> p_eid))
      maybe_leavesetting <- if (p_eid=="") LeaveSettingModel.findOne(BSONDocument(), p_request) else LeaveSettingModel.findOne(BSONDocument("sys.eid" -> p_eid))
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val leaveearned = leavepolicy.acc match {
        case "No accrue" => 0.0
        case "Monthly" => this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, DateTime.now().getMonthOfYear())
        case "Yearly" => this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
      }
      val future = col.insert(
          p_doc.copy(
              ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
              ear = leaveearned,
              bal = leaveearned + p_doc.adj - p_doc.uti + p_doc.cf - p_doc.cfuti - p_doc.cfexp,
              me = LeaveProfileMonthEarn(
                  jan = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 1),
                  feb = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 2),
                  mar = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 3),
                  apr = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 4),
                  may = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 5),
                  jun = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 6),
                  jul = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 7),
                  aug = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 8),
                  sep = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 9),
                  oct = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 10),
                  nov = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 11),
                  dec = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 12)
              ),
              sys = SystemDataStore.creation(p_eid,p_request)
          )
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  // Update document
  def update(p_query:BSONDocument,p_doc:LeaveProfile,p_request:RequestHeader) = {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_doc.pid)), p_request)
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt ,"pt"->maybe_person.get.p.pt), p_request)
      maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument(), p_request)
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val future = col.update(
          p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), 
          p_doc.copy(
              ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
              bal = p_doc.ear + p_doc.adj - p_doc.uti + p_doc.cf - p_doc.cfuti - p_doc.cfexp,
              me = LeaveProfileMonthEarn(
                  jan = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 1),
                  feb = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 2),
                  mar = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 3),
                  apr = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 4),
                  may = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 5),
                  jun = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 6),
                  jul = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 7),
                  aug = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 8),
                  sep = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 9),
                  oct = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 10),
                  nov = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 11),
                  dec = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 12)
              ),
              sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)
          )
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  // Update document
  def update(p_query:BSONDocument, p_doc:LeaveProfile, p_eid:String) = {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id"->BSONObjectID(p_doc.pid), "sys.eid"->p_eid))
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt, "pt"->maybe_person.get.p.pt, "sys.eid"->p_eid))
      maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument("sys.eid"->p_eid))
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val future = col.update(
          p_query.++(BSONDocument("sys.eid" -> p_eid, "sys.ddat"->BSONDocument("$exists"->false))), 
          p_doc.copy(
              ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
              bal = p_doc.ear + p_doc.adj - p_doc.uti + p_doc.cf - p_doc.cfuti - p_doc.cfexp,
              me = LeaveProfileMonthEarn(
                  jan = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 1),
                  feb = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 2),
                  mar = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 3),
                  apr = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 4),
                  may = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 5),
                  jun = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 6),
                  jul = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 7),
                  aug = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 8),
                  sep = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 9),
                  oct = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 10),
                  nov = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 11),
                  dec = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 12)
              ),
              sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc))
          )
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  def isUnique(p_doc:LeaveProfile, p_request:RequestHeader) = {
    for{
      maybe_leavepolicy <- this.findOne(BSONDocument("pid" ->p_doc.pid, "lt"->p_doc.lt), p_request)
    } yield {
      if (maybe_leavepolicy.isEmpty) true else false
    }
  }
  
  def sortEligbleLeaveEntitlement(p_doc:LeaveProfile, p_request:RequestHeader) = {
        
    var eligbleleaveentitlement = ArrayBuffer.fill(5,3)(0)
    
    // Replace 0 value to 1000
    if (p_doc.set_ent.e1_s == 0) {
      eligbleleaveentitlement(0) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(0) = ArrayBuffer(p_doc.set_ent.e1_s, p_doc.set_ent.e1, p_doc.set_ent.e1_cf)
    }

    if (p_doc.set_ent.e2_s == 0) {
      eligbleleaveentitlement(1) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(1) = ArrayBuffer(p_doc.set_ent.e2_s, p_doc.set_ent.e2, p_doc.set_ent.e2_cf)
    }
    
    if (p_doc.set_ent.e3_s == 0) {
      eligbleleaveentitlement(2) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(2) = ArrayBuffer(p_doc.set_ent.e3_s, p_doc.set_ent.e3, p_doc.set_ent.e3_cf)
    }
    
    if (p_doc.set_ent.e4_s == 0) {
    	eligbleleaveentitlement(3) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(3) = ArrayBuffer(p_doc.set_ent.e4_s, p_doc.set_ent.e4, p_doc.set_ent.e4_cf)
    }
    
    if (p_doc.set_ent.e5_s == 0) {
      eligbleleaveentitlement(4) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(4) = ArrayBuffer(p_doc.set_ent.e5_s, p_doc.set_ent.e5, p_doc.set_ent.e5_cf)
    }
    
    val eligbleleaveentitlementsorted = eligbleleaveentitlement.sortBy(_(0))
    var eligbleleaveentitlementsorted_update = ArrayBuffer.fill(5,3)(0)
    
    // Replace 1000 back to 0
    for (i <- 0 to 4) {
      if (eligbleleaveentitlementsorted(i)(0) == 1000) {
        eligbleleaveentitlementsorted_update(i) = ArrayBuffer(0, 0, 0)
      } else {
        eligbleleaveentitlementsorted_update(i) = ArrayBuffer(eligbleleaveentitlementsorted(i)(0), eligbleleaveentitlementsorted(i)(1), eligbleleaveentitlementsorted(i)(2))
      }
    }
    
    eligbleleaveentitlementsorted_update
  }
  
  def getEligibleEntitlement(p_doc:LeaveProfile, p_servicemonth:Int) = { 
    p_servicemonth match {
      case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth <= p_doc.set_ent.e1_s => p_doc.set_ent.e1
      case servicemonth if servicemonth <= p_doc.set_ent.e2_s => p_doc.set_ent.e2
      case servicemonth if servicemonth <= p_doc.set_ent.e3_s => p_doc.set_ent.e3
      case servicemonth if servicemonth <= p_doc.set_ent.e4_s => p_doc.set_ent.e4
      case servicemonth if servicemonth <= p_doc.set_ent.e5_s => p_doc.set_ent.e5
      case _ => 0
    }
  }
  
  def getEligibleCarryForword(p_doc:LeaveProfile, p_servicemonth:Int) = {
    p_servicemonth match {
      case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth <= p_doc.set_ent.e1_s => p_doc.set_ent.e1_cf 
      case servicemonth if servicemonth <= p_doc.set_ent.e2_s => p_doc.set_ent.e2_cf
      case servicemonth if servicemonth <= p_doc.set_ent.e3_s => p_doc.set_ent.e3_cf
      case servicemonth if servicemonth <= p_doc.set_ent.e4_s => p_doc.set_ent.e4_cf
      case servicemonth if servicemonth <= p_doc.set_ent.e5_s => p_doc.set_ent.e5_cf
      case _ => 0
    }
  }
  
  def getEligibleCarryForwordEarn(p_leaveprofile:LeaveProfile, p_servicemonth:Int) = {
    val eligblecarryforword = this.getEligibleCarryForword(p_leaveprofile, p_servicemonth)
    if (p_leaveprofile.bal > eligblecarryforword) eligblecarryforword else p_leaveprofile.bal
  }
  
  def getMonthEntitlementEarn(p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person, p_month:Int) = {
    val servicemonth = PersonModel.getServiceMonths(p_person)
    val entitlement = this.getEligibleEntitlement(p_leaveprofile, servicemonth)
    val accruetype = p_leavepolicy.acc
    val leavecutoff_mth = p_leavesetting.cfm
    accruetype match {
      case "No accrue" => 0.0
      case "Monthly" => {
        val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth + 12 - 12 }
        val entitlementindouble = entitlement.toDouble
        if(leavecutoff_mth == p_month) {
          val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
          BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
        } else {
          val mthearn = entitlementindouble / 12
          BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
        }
      }
      case "Yearly" => if(leavecutoff_mth == p_month) { entitlement.toDouble } else { 0.0 }
    }      
  }
  
  def getEligibleCarryForwardExpired(p_leaveprofile: LeaveProfile, p_leavesetting: LeaveSetting, p_leavepolicy: LeavePolicy) = {
    if (p_leavepolicy.cexp == 0) {
      0.0
    } else {
      val now = new DateTime
      val cutoffdate = new DateTime(now.year().get(), p_leavesetting.cfm,1,0,0,0,0)
      val carryforwardexpireddate = cutoffdate.plusMonths(p_leavepolicy.cexp)
      if (now.isAfter(carryforwardexpireddate)) p_leaveprofile.cf - p_leaveprofile.cfuti else 0.0
    }
  }
    
  def getLeaveTypes(p_pid:String, p_request:RequestHeader) = {
    for {
      maybe_leavetypes <- this.find(BSONDocument("pid" -> p_pid), p_request)
    } yield {
      var typesList = List[String]()
      maybe_leavetypes.foreach(leavetype => typesList = typesList :+ leavetype.lt)
      typesList
    }
  }
  
  // Notes:
  // 1 p_modifier format: 
  //   1.1 Replace - BSONDocument
  //   1.2 Update certain field - BSONDocument("$set"->BSONDocument("[FIELD]"->VALUE))
  // 2 No SystemDataStore update
  def updateUsingBSON(p_query:BSONDocument,p_modifier:BSONDocument) = {
    val future = col.update(selector=p_query, update=p_modifier, multi=true)
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
}