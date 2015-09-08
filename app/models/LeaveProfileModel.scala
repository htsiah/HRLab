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

case class LeaveProfile (
    _id: BSONObjectID,
    pid: String,	// person object id
    pn: String,
    lt: String,
    cal: LeaveProfileCalculation,
    me: LeaveProfileMonthEarn,
    set_ent: Entitlement, // Case class from leave policy model
    sys: Option[System]
)

case class LeaveProfileCalculation (
    ent: Int,
    ear: Double,
    adj: Double,
    uti: Double,
    cf: Double,
    cfuti: Double,
    cfexp: Double,
    papr: Double,
    bal: Double,
    cbal: Double
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
  
  implicit object LeaveProfileCalculationBSONReader extends BSONDocumentReader[LeaveProfileCalculation] {
    def read(doc: BSONDocument): LeaveProfileCalculation = {
      LeaveProfileCalculation(
          doc.getAs[Int]("ent").get,
          doc.getAs[Double]("ear").get,
          doc.getAs[Double]("adj").get,
          doc.getAs[Double]("uti").get,
          doc.getAs[Double]("cf").get,
          doc.getAs[Double]("cfuti").get,
          doc.getAs[Double]("cfexp").get,
          doc.getAs[Double]("papr").get,
          doc.getAs[Double]("bal").get,
          doc.getAs[Double]("cbal").get
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
          doc.getAs[LeaveProfileCalculation]("cal").get,
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
  
  implicit object LeaveProfileCalculationBSONWriter extends BSONDocumentWriter[LeaveProfileCalculation] {
    def write(leaveprofilecalculation: LeaveProfileCalculation): BSONDocument = {
      BSONDocument(
          "ent" -> leaveprofilecalculation.ent,
          "ear" -> leaveprofilecalculation.ear,
          "adj" -> leaveprofilecalculation.adj,
          "uti" -> leaveprofilecalculation.uti,
          "cf" -> leaveprofilecalculation.cf,
          "cfuti" -> leaveprofilecalculation.cfuti,
          "cfexp" -> leaveprofilecalculation.cfexp,
          "papr" -> leaveprofilecalculation.papr,
          "bal" -> leaveprofilecalculation.bal,
          "cbal" -> leaveprofilecalculation.cbal
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
          "cal" -> leaveprofile.cal,
          "me" -> leaveprofile.me,
          "set_ent" -> leaveprofile.set_ent,
          "sys" -> leaveprofile.sys
      )     
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_leave").getOrElse("leave")
  private val uri = Play.current.configuration.getString("mongodb_leave_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
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
      cal = LeaveProfileCalculation (ent = 0, ear = 0.0, adj = 0.0, uti = 0.0, cf = 0.0, cfuti = 0.0, cfexp = 0.0, papr = 0.0, bal = 0.0, cbal = 0.0),
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
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[LeaveProfile](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[LeaveProfile](ReadPreference.primary).collect[List]()
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
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
          }
        }
      }
      val balance = leaveearned + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
      val cbalance = leavepolicy.set.acc match {
        case "No accrue" => balance
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          } else {
            // Earn calculate from previous cut of date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          } else {
            // Earn calculate from previous cut of date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          }
        }
        case "Yearly" => balance
      }
      val future = col.insert(
          p_doc.copy(
              cal = p_doc.cal.copy(
                  ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
                  ear = leaveearned,
                  bal = BigDecimal(balance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble,
                  cbal = BigDecimal(cbalance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
              ),
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
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => p_doc.cal.ear
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
          }
        }
      }
      val balance = leaveearned + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
      val cbalance = leavepolicy.set.acc match {
        case "No accrue" => balance
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          }
        }
        case "Yearly" => balance
      }
      val future = col.update(
          p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), 
          p_doc.copy(
              cal = p_doc.cal.copy(
                  ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
                  ear = leaveearned,
                  bal = BigDecimal(balance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble,
                  cbal = BigDecimal(cbalance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
              ), 
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
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => p_doc.cal.ear
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
          }
        }
      }
      val balance = leaveearned + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
      val cbalance = leavepolicy.set.acc match {
        case "No accrue" => balance
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp
          }
        }
        case "Yearly" => balance
      }
      val future = col.update(
          p_query.++(BSONDocument("sys.eid" -> p_eid, "sys.ddat"->BSONDocument("$exists"->false))), 
          p_doc.copy(
              cal = p_doc.cal.copy(
                  ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
                  ear = leaveearned,
                  bal = BigDecimal(balance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble,
                  cbal = BigDecimal(cbalance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
              ),
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
  
  // Get entitlement using leave profile
  // For update leave profile
  def getEligibleEntitlement(p_doc:LeaveProfile, p_servicemonth:Int) = { 
    p_servicemonth match {
      // case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth < p_doc.set_ent.e1_s => p_doc.set_ent.e1
      case servicemonth if servicemonth <= p_doc.set_ent.e2_s => p_doc.set_ent.e2
      case servicemonth if servicemonth <= p_doc.set_ent.e3_s => p_doc.set_ent.e3
      case servicemonth if servicemonth <= p_doc.set_ent.e4_s => p_doc.set_ent.e4
      case servicemonth if servicemonth <= p_doc.set_ent.e5_s => p_doc.set_ent.e5
      case _ => 0
    }
  }
  
  // Get entitlement using leave policy
  // For new leave profile
  def getEligibleEntitlement(p_leavepolicy:LeavePolicy, p_servicemonth:Int) = { 
    p_servicemonth match {
      // case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth < p_leavepolicy.ent.e1_s => p_leavepolicy.ent.e1
      case servicemonth if servicemonth <= p_leavepolicy.ent.e2_s => p_leavepolicy.ent.e2
      case servicemonth if servicemonth <= p_leavepolicy.ent.e3_s => p_leavepolicy.ent.e3
      case servicemonth if servicemonth <= p_leavepolicy.ent.e4_s => p_leavepolicy.ent.e4
      case servicemonth if servicemonth <= p_leavepolicy.ent.e5_s => p_leavepolicy.ent.e5
      case _ => 0
    }
  }
  
  def getEligibleCarryForword(p_doc:LeaveProfile, p_servicemonth:Int) = {
    p_servicemonth match {
      // case servicemonth if servicemonth < 0 => 0
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
    if (p_leaveprofile.cal.cbal > eligblecarryforword) eligblecarryforword else p_leaveprofile.cal.cbal
  }
  
  // Get total leave earn from previous cut off date or employee start date until cut off date.
  // Get entitlement using leave policy
  // For update leave profile
  def getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate:DateTime, p_date:DateTime, p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_date.isAfter(p_cutoffdate.minusMonths(2).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate, p_date.plusMonths(1), p_leaveprofile, p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  // Get total leave earn from previous cut off date or employee start date until cut off date.
  // Get entitlement using leave policy
  // For new leave profile
  def getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate:DateTime, p_date:DateTime, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_date.isAfter(p_cutoffdate.minusMonths(2).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate, p_date.plusMonths(1), p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
    
  // Get total leave earn from given parameter date until now.
  // Get entitlement using leave profile
  // For update leave profile
  def getTotalMonthlyEntitlementEarn(p_date:DateTime, p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_person.p.edat.get.dayOfMonth().withMaximumValue().isAfter(DateTime.now().dayOfMonth().withMaximumValue())) {
      0.0
    } else if (p_date.isAfter(DateTime.now().minusMonths(1).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarn(p_date.plusMonths(1), p_leaveprofile, p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
      
  // Get total leave earn from given parameter date until now.
  // Get entitlement using leave policy
  // For new leave profile
  def getTotalMonthlyEntitlementEarn(p_date:DateTime, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_person.p.edat.get.dayOfMonth().withMaximumValue().isAfter(DateTime.now().dayOfMonth().withMaximumValue())) {
      0.0
    } else if (p_date.isAfter(DateTime.now().minusMonths(1).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarn(p_date.plusMonths(1), p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
   
  // Get entitlement using leave profile
  // Entitlement is 0 when employee join after cut of month.
  // For update leave profile
  def getMonthEntitlementEarn(p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person, p_month:Int) = {
    val cutoffmonth = p_leavesetting.cfm
    val nextcutofdate = LeaveSettingModel.getCutOffDate(p_leavesetting.cfm)
    val previouscutofdate = LeaveSettingModel.getPreviousCutOffDate(p_leavesetting.cfm)
    val calculate_date = if (cutoffmonth <= p_month) { previouscutofdate.plusMonths(p_month - cutoffmonth) } else { nextcutofdate.minusMonths(cutoffmonth - p_month) }
    val accruetype = p_leavepolicy.set.acc
    if (p_person.p.edat.get.isAfter(calculate_date.dayOfMonth().withMaximumValue()) && accruetype!="Yearly") {
      // Entitlement is 0 when employee join after cut of month.
      0.0      
    } else if (p_person.p.edat.get.isAfter(nextcutofdate.minusMonths(1).dayOfMonth().withMaximumValue()) && accruetype=="Yearly") {
      0.0
    } else {
      val servicemonth = PersonModel.getServiceMonths(p_person)
      val entitlement = this.getEligibleEntitlement(p_leaveprofile, servicemonth)
      val leavecutoff_mth = p_leavesetting.cfm
      accruetype match {
        case "No accrue" => 0.0
        case "Monthly - utilisation based on earned" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
            val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          } else {
            val mthearn = entitlementindouble / 12
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
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
  }
    
  // Get entitlement using leave policy
  // For new leave profile  
  def getMonthEntitlementEarn(p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person, p_month:Int) = {
    val cutoffmonth = p_leavesetting.cfm
    val nextcutofdate = LeaveSettingModel.getCutOffDate(p_leavesetting.cfm)
    val previouscutofdate = LeaveSettingModel.getPreviousCutOffDate(p_leavesetting.cfm)
    val calculate_date = if (cutoffmonth <= p_month) { previouscutofdate.plusMonths(p_month - cutoffmonth) } else { nextcutofdate.minusMonths(cutoffmonth - p_month) }
    val accruetype = p_leavepolicy.set.acc
    if (p_person.p.edat.get.isAfter(calculate_date.dayOfMonth().withMaximumValue()) && accruetype!="Yearly") {
      // Entitlement is 0 when employee join after cut of month.
      0.0      
    } else if (p_person.p.edat.get.isAfter(nextcutofdate.minusMonths(1).dayOfMonth().withMaximumValue()) && accruetype=="Yearly") {
      0.0
    } else {
      val servicemonth = PersonModel.getServiceMonths(p_person)
      val entitlement = this.getEligibleEntitlement(p_leavepolicy, servicemonth)
      val leavecutoff_mth = p_leavesetting.cfm
      accruetype match {
        case "No accrue" => 0.0
        case "Monthly - utilisation based on earned" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
            val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          } else {
            val mthearn = entitlementindouble / 12
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
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
  }
  
  def getEligibleCarryForwardExpired(p_leaveprofile: LeaveProfile, p_leavesetting: LeaveSetting, p_leavepolicy: LeavePolicy) = {
    if (p_leavepolicy.set.cexp == 0) {
      0.0
    } else {
      val now = new DateTime
      val cutoffdate = new DateTime(now.year().get(), p_leavesetting.cfm,1,0,0,0,0)
      val carryforwardexpireddate = cutoffdate.plusMonths(p_leavepolicy.set.cexp)
      if (now.isAfter(carryforwardexpireddate)) p_leaveprofile.cal.cf - p_leaveprofile.cal.cfuti else 0.0
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