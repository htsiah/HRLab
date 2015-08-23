package models

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore}

import scala.util.{Success, Failure,Try}
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
    def read(doc: BSONDocument): Person = {
      Person(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[Profile]("p").get,
          doc.getAs[Workday]("wd").get,
          doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  implicit object ProfileBSONReader extends BSONDocumentReader[Profile] {
    def read(doc: BSONDocument): Profile = {
      Profile(
          doc.getAs[String]("fn").get,
          doc.getAs[String]("ln").get,
          doc.getAs[String]("em").get,
          doc.getAs[String]("pt").get,
          doc.getAs[String]("mgrid").get,
          doc.getAs[String]("g").get,
          doc.getAs[String]("ms").get,
          doc.getAs[String]("dpm").get,
          doc.getAs[String]("off").get,
          doc.getAs[BSONDateTime]("edat").map(dt => new DateTime(dt.value )),
          doc.getAs[List[String]]("rl").get
      )
    }
  }
  
  implicit object WorkdayBSONReader extends BSONDocumentReader[Workday] {
    def read(doc: BSONDocument): Workday = {
      Workday(
          doc.getAs[Boolean]("wd1").getOrElse(true),
          doc.getAs[Boolean]("wd2").getOrElse(true),
          doc.getAs[Boolean]("wd3").getOrElse(true),
          doc.getAs[Boolean]("wd4").getOrElse(true),
          doc.getAs[Boolean]("wd5").getOrElse(true),
          doc.getAs[Boolean]("wd6").getOrElse(false),
          doc.getAs[Boolean]("wd7").getOrElse(false)  
      )
    }
  }
  
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
  
  // Use Writer to serialize document automatically
  implicit object PersonBSONWriter extends BSONDocumentWriter[Person] {
    def write(person: Person): BSONDocument = {
      BSONDocument(
          "_id" -> person._id,
          "p" -> person.p,
          "wd" -> person.wd,
          "sys" -> person.sys
      )     
    }
  }
  
  implicit object ProfileBSONWriter extends BSONDocumentWriter[Profile] {
    def write(profile: Profile): BSONDocument = {
      BSONDocument(
          "fn" -> profile.fn,
          "ln" -> profile.ln,
          "em" -> profile.em,
          "pt" -> profile.pt,
          "mgrid" -> profile.mgrid,
          "g" -> profile.g,
          "ms" -> profile.ms,
          "dpm" -> profile.dpm,
          "off" -> profile.off,
          "edat" -> profile.edat.map(date => BSONDateTime(date.getMillis)),
          "rl" -> profile.rl
      )     
    }
  }
  
  implicit object WorkdayBSONWriter extends BSONDocumentWriter[Workday] {
    def write(workday: Workday): BSONDocument = {
      BSONDocument(
          "wd1" -> workday.wd1,
          "wd2" -> workday.wd2,
          "wd3" -> workday.wd3,
          "wd4" -> workday.wd4,
          "wd5" -> workday.wd5,
          "wd6" -> workday.wd6,
          "wd7" -> workday.wd7
      )     
    }
  }
    
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

  private val dbname = Play.current.configuration.getString("mongodb_directory").getOrElse("directory")
  private val uri = Play.current.configuration.getString("mongodb_directory_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("person")
  val doc = Person(
      _id = BSONObjectID.generate,
      p = Profile(fn="", ln="", em="", pt="", mgrid="", g="", ms="", dpm="", off="", edat=Some(new DateTime()), rl=List("")),
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
                "pt" -> p_doc.p.pt,
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
            val leaveprofile_doc = LeaveProfileModel.doc.copy(
                _id = BSONObjectID.generate,
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
                "pt" -> "Manager",
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
            val leaveprofile_doc = LeaveProfileModel.doc.copy(
                _id = BSONObjectID.generate,
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
          // Update name on leave and leave profile
          if (oldperson.get.p.fn != p_doc.p.fn || oldperson.get.p.ln != p_doc.p.ln) {
            LeaveModel.updateUsingBSON(BSONDocument("pid"->p_doc._id.stringify), BSONDocument("$set"->BSONDocument("pn"->(p_doc.p.fn + " " + p_doc.p.ln))))
            LeaveModel.updateUsingBSON(BSONDocument("w_aprid"->p_doc._id.stringify), BSONDocument("$set"->BSONDocument("w_aprn"->(p_doc.p.fn + " " + p_doc.p.ln))))
            LeaveProfileModel.updateUsingBSON(BSONDocument("pid"->p_doc._id.stringify), BSONDocument("$set"->BSONDocument("pn"->(p_doc.p.fn + " " + p_doc.p.ln))))
          }
          
          // Update leave profiles 
          LeaveProfileModel.find(BSONDocument("pid" -> p_doc._id.stringify), p_request).map { leaveprofiles =>
            leaveprofiles.foreach { leaveprofile => {
                  LeaveProfileModel.update(BSONDocument("_id" -> leaveprofile._id), leaveprofile, p_request)
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