package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, Tools, DbConnUtility}

import scala.concurrent.Await

import scala.util.{Success, Failure}
import org.joda.time.DateTime
import org.joda.time.Months

case class Person (
     _id: BSONObjectID,
     p: Profile,
     wd: Workday,
     sys: Option[System]
)

case class Profile (
    fn: String,
    ln: String,
    em: String,
    nem: Boolean,
    pt: String,
    mgrid: String,
    g: String,
    ms: String,
    dpm: String,
    off: String,
    edat: Option[DateTime],
    rl: List[String]
)

case class Workday (
    wd1: Boolean,
    wd2: Boolean,
    wd3: Boolean,
    wd4: Boolean,
    wd5: Boolean,
    wd6: Boolean,
    wd7: Boolean
)

object PersonModel {
  
  // Use Reader to deserialize document automatically
  implicit object PersonBSONReader extends BSONDocumentReader[Person] {
    def read(p_doc: BSONDocument): Person = {
      Person(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[Profile]("p").get,
          p_doc.getAs[Workday]("wd").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  implicit object ProfileBSONReader extends BSONDocumentReader[Profile] {
    def read(p_doc: BSONDocument): Profile = {
      Profile(
          p_doc.getAs[String]("fn").get,
          p_doc.getAs[String]("ln").get,
          p_doc.getAs[String]("em").get,
          p_doc.getAs[Boolean]("nem").getOrElse(false),
          p_doc.getAs[String]("pt").get,
          p_doc.getAs[String]("mgrid").get,
          p_doc.getAs[String]("g").get,
          p_doc.getAs[String]("ms").get,
          p_doc.getAs[String]("dpm").get,
          p_doc.getAs[String]("off").get,
          p_doc.getAs[BSONDateTime]("edat").map(dt => new DateTime(dt.value )),
          p_doc.getAs[List[String]]("rl").get
      )
    }
  }
  
  implicit object WorkdayBSONReader extends BSONDocumentReader[Workday] {
    def read(p_doc: BSONDocument): Workday = {
      Workday(
          p_doc.getAs[Boolean]("wd1").getOrElse(true),
          p_doc.getAs[Boolean]("wd2").getOrElse(true),
          p_doc.getAs[Boolean]("wd3").getOrElse(true),
          p_doc.getAs[Boolean]("wd4").getOrElse(true),
          p_doc.getAs[Boolean]("wd5").getOrElse(true),
          p_doc.getAs[Boolean]("wd6").getOrElse(false),
          p_doc.getAs[Boolean]("wd7").getOrElse(false)  
      )
    }
  }
  
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
  
  // Use Writer to serialize document automatically
  implicit object PersonBSONWriter extends BSONDocumentWriter[Person] {
    def write(p_doc: Person): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "p" -> p_doc.p,
          "wd" -> p_doc.wd,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  implicit object ProfileBSONWriter extends BSONDocumentWriter[Profile] {
    def write(p_doc: Profile): BSONDocument = {
      BSONDocument(
          "fn" -> p_doc.fn,
          "ln" -> p_doc.ln,
          "em" -> p_doc.em,
          "nem" -> p_doc.nem,
          "pt" -> p_doc.pt,
          "mgrid" -> p_doc.mgrid,
          "g" -> p_doc.g,
          "ms" -> p_doc.ms,
          "dpm" -> p_doc.dpm,
          "off" -> p_doc.off,
          "edat" -> p_doc.edat.map(date => BSONDateTime(date.getMillis)),
          "rl" -> p_doc.rl
      )     
    }
  }
  
  implicit object WorkdayBSONWriter extends BSONDocumentWriter[Workday] {
    def write(p_doc: Workday): BSONDocument = {
      BSONDocument(
          "wd1" -> p_doc.wd1,
          "wd2" -> p_doc.wd2,
          "wd3" -> p_doc.wd3,
          "wd4" -> p_doc.wd4,
          "wd5" -> p_doc.wd5,
          "wd6" -> p_doc.wd6,
          "wd7" -> p_doc.wd7
      )     
    }
  }
    
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
    
  private val col = DbConnUtility.dir_db.collection("person")
  
  val doc = Person(
      _id = BSONObjectID.generate,
      p = Profile(fn="", ln="", em="", nem=false, pt="", mgrid="", g="", ms="", dpm="", off="", edat=Some(new DateTime(DateTime.now().getYear, 1, 1, 0, 0, 0, 0)), rl=List("")),
      wd = Workday(wd1=true, wd2=true, wd3=true, wd4=true, wd5=true, wd6=false, wd7=false),
      sys = None
  )
  
  private def updateSystem(p_doc:Person) = {
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
    col.find(p_query).cursor[Person](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Person](ReadPreference.primary).collect[List]()
  }

  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Person]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Person]
  }
  
  /** Custom Model Methods **/ 
  
// Insert new document
  def insert(p_doc:Person, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        // Create person's leave profile from LEAVE POLICY
        LeavePolicyModel.find(            
            BSONDocument(
                "$or" -> BSONArray(
                    BSONDocument("set.g"->(p_doc.p.g + " only")),
                    BSONDocument("set.g"->"Applicable for all")
                ),
                "$or" -> BSONArray(
                    BSONDocument("set.ms"->(p_doc.p.ms + " only")),
                    BSONDocument("set.ms"->"Applicable for all")
                )
            ),
            p_request
        ).map(leavepolicies => {
          leavepolicies.map( leavepolicy => {
            val doc_objectID = BSONObjectID.generate
            val leaveprofile_doc = LeaveProfileModel.doc.copy(
                _id = doc_objectID,
                pid = p_doc._id.stringify,
                pn = p_doc.p.fn + " " + p_doc.p.ln,
                lt = leavepolicy.lt,
                set_ent = Entitlement(
                    leavepolicy.ent.e1,
                    leavepolicy.ent.e1_s,
                    leavepolicy.ent.e1_cf,
                    leavepolicy.ent.e2,
                    leavepolicy.ent.e2_s,
                    leavepolicy.ent.e2_cf,
                    leavepolicy.ent.e3,
                    leavepolicy.ent.e3_s,
                    leavepolicy.ent.e3_cf,
                    leavepolicy.ent.e4,
                    leavepolicy.ent.e4_s,
                    leavepolicy.ent.e4_cf,
                    leavepolicy.ent.e5,
                    leavepolicy.ent.e5_s,
                    leavepolicy.ent.e5_cf
                )
            )
            LeaveProfileModel.insert(leaveprofile_doc, p_eid, p_request)
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id=BSONObjectID.generate, pid="", pn="System", lk=doc_objectID.stringify, c="Create document."), p_request=p_request)
          })
        })
      }
    }
  }
  
  // Add registrator's person record during new Sign Up
  def insertOnNewSignUp(p_doc:Person, p_eid:String)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,null)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        // Create person's leave profile from CONFIG LEAVE POLICY
        Thread sleep 3000  // Wait 3 second make sure leave policy and setting is created
        ConfigLeavePolicyModel.find(
            BSONDocument(
                "$or" -> BSONArray(
                    BSONDocument("set.g"->(p_doc.p.g + " only")),
                    BSONDocument("set.g"->"Applicable for all")
                ),
                "$or" -> BSONArray(
                    BSONDocument("set.ms"->(p_doc.p.ms + " only")),
                    BSONDocument("set.ms"->"Applicable for all")
                )
            )
        ).map(configleavepolicies => {
          configleavepolicies.map( configleavepolicy => {
            val leaveprofile_objectID = BSONObjectID.generate
            val leaveprofile_doc = LeaveProfileModel.doc.copy(
                _id = leaveprofile_objectID,
                pid = p_doc._id.stringify,
                pn = p_doc.p.fn + " " + p_doc.p.ln,
                lt = configleavepolicy.lt,
                set_ent = Entitlement(
                    configleavepolicy.ent.e1,
                    configleavepolicy.ent.e1_s,
                    configleavepolicy.ent.e1_cf,
                    configleavepolicy.ent.e2,
                    configleavepolicy.ent.e2_s,
                    configleavepolicy.ent.e2_cf,
                    configleavepolicy.ent.e3,
                    configleavepolicy.ent.e3_s,
                    configleavepolicy.ent.e3_cf,
                    configleavepolicy.ent.e4,
                    configleavepolicy.ent.e4_s,
                    configleavepolicy.ent.e4_cf,
                    configleavepolicy.ent.e5,
                    configleavepolicy.ent.e5_s,
                    configleavepolicy.ent.e5_cf                
                )
            )
            LeaveProfileModel.insert(leaveprofile_doc, p_eid)
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=leaveprofile_objectID.stringify, c="Create document."), p_eid=p_eid)
          })
        })
      }
    }
  }

  // Update document
  def update(p_query:BSONDocument,p_doc:Person,p_request:RequestHeader) = {
    for {
      oldperson <- this.findOne(BSONDocument("_id"->p_doc._id), p_request)
    } yield {
      val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          // Update name on leave
          if (oldperson.get.p.fn != p_doc.p.fn || oldperson.get.p.ln != p_doc.p.ln) {
            LeaveModel.updateUsingBSON(BSONDocument("pid"->p_doc._id.stringify), BSONDocument("$set"->BSONDocument("pn"->(p_doc.p.fn + " " + p_doc.p.ln))))
            LeaveModel.updateUsingBSON(BSONDocument("w_aprid"->p_doc._id.stringify), BSONDocument("$set"->BSONDocument("w_aprn"->(p_doc.p.fn + " " + p_doc.p.ln))))
          }
          
          // Update leave profiles - recalculate leave entitlement
          LeaveProfileModel.find(BSONDocument("pid" -> p_doc._id.stringify), p_request).map { leaveprofiles =>
            leaveprofiles.foreach { leaveprofile => {
                  LeaveProfileModel.update(BSONDocument("_id" -> leaveprofile._id), leaveprofile.copy(pn=p_doc.p.fn + " " + p_doc.p.ln), p_request)
            } }
          }
        }
      }
    }
  }
  
  // Required during authentication
  def findOneByEmail(p_email:String) = {
    col.find(BSONDocument("p.em" -> p_email, "sys.ddat"->BSONDocument("$exists"->false))).one[Person]
  }
  
  // Return: 
  // - Calculation include day. 
  // - Example of today is 27 Jan 2015:
  //   1. Employment is 27 Dec 2014 - Service months is 1.
  //   2. Employment is 26 Jan 2015 - Service months is 0.
  //   3. Employment is 27 Jan 2015 - Service months is 0.
  //   4. Employment is 28 Jan 2015 - Service months is -1.
  //   5. Employment is 27 Feb 2015 - Service months is -1.
  //   6. Employment is 28 Feb 2015 - Service months is -2.
  def getServiceMonths(p_doc:Person) = {
    if (p_doc.p.edat.get.isAfter(DateTime.now())) {
      Months.monthsBetween(p_doc.p.edat.get, DateTime.now()).getMonths() - 1
    } else {
      Months.monthsBetween(p_doc.p.edat.get, DateTime.now()).getMonths()
    }
  }
  
  def isWorkDay(p_doc: Person, p_Date: DateTime): Boolean = {
    p_Date.getDayOfWeek() match {
      case 1 => if(p_doc.wd.wd1 == true){ return true }
      case 2 => if(p_doc.wd.wd2 == true){ return true } 
      case 3 => if(p_doc.wd.wd3 == true){ return true } 
      case 4 => if(p_doc.wd.wd4 == true){ return true } 
      case 5 => if(p_doc.wd.wd5 == true){ return true } 
      case 6 => if(p_doc.wd.wd6 == true){ return true }
      case 7 => if(p_doc.wd.wd7 == true){ return true }
    }
    return false
  }
  
  def isLastAdmin(p_id:String, p_request:RequestHeader) = {
    for {
      persons <- PersonModel.find(BSONDocument("p.rl"->BSONDocument("$in"->List("Admin"))), p_request)
    } yield {
      if (persons.length == 1 && persons.head._id.stringify == p_id) {
        true
      } else {
        false
      }
    }
  }
  
  def isManagerFor(p_id:String, p_mid:String, p_request:RequestHeader) = {
    val maybe_person = Await.result(PersonModel.findOne(BSONDocument("_id"->BSONObjectID(p_id)), p_request), Tools.db_timeout)
    val person = maybe_person.getOrElse(PersonModel.doc)
    if ( person.p.mgrid == p_mid) {
      true
    } else {
      false
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
  
  // Optional - Find all document with filter
  // def find(p_query:BSONDocument,p_filter:BSONDocument) = {}
	
  // Optional - Find all document with filter and sorting
  // def find(p_query:BSONDocument,p_filter:BSONDocument,p_sort:BSONDocument) = {}
  
}