package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.json._
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
    empid: String,
    fn: String,
    ln: String,
    em: String,
    nem: Boolean,
    pt: String,
    mgrid: String,
    smgrid:String,
    g: String,
    ms: String,
    dpm: String,
    off: String,
    edat: Option[DateTime],
    rl: List[String]
)

case class Workday (
    wd1: String,
    wd2: String,
    wd3: String,
    wd4: String,
    wd5: String,
    wd6: String,
    wd7: String
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
          p_doc.getAs[String]("empid").get,
          p_doc.getAs[String]("fn").get,
          p_doc.getAs[String]("ln").get,
          p_doc.getAs[String]("em").get,
          p_doc.getAs[Boolean]("nem").getOrElse(false),
          p_doc.getAs[String]("pt").get,
          p_doc.getAs[String]("mgrid").get,
          p_doc.getAs[String]("smgrid").get,
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
          p_doc.getAs[String]("wd1").get,
          p_doc.getAs[String]("wd2").get,
          p_doc.getAs[String]("wd3").get,
          p_doc.getAs[String]("wd4").get,
          p_doc.getAs[String]("wd5").get,
          p_doc.getAs[String]("wd6").get,
          p_doc.getAs[String]("wd7").get
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
          "empid" -> p_doc.empid,
          "fn" -> p_doc.fn,
          "ln" -> p_doc.ln,
          "em" -> p_doc.em,
          "nem" -> p_doc.nem,
          "pt" -> p_doc.pt,
          "mgrid" -> p_doc.mgrid,
          "smgrid" -> p_doc.smgrid,
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
      p = Profile(empid="", fn="", ln="", em="", nem=false, pt="", mgrid="", smgrid="", g="", ms="", dpm="", off="", edat=Some(new DateTime(DateTime.now().getYear, 1, 1, 0, 0, 0, 0)), rl=List("")),
      wd = Workday(wd1="Full", wd2="Full", wd3="Full", wd4="Full", wd5="Full", wd6="Off", wd7="Off"),
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
      docs.foreach { doc => 
        val future = col.update(BSONDocument("_id" -> doc._id, "sys.ddat"->BSONDocument("$exists"->false)), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {
                    
            // Remove Person from claim category
            ClaimCategoryModel.removePersonInApp(doc.p.fn  + " " + doc.p.ln + "@|@" + doc._id.stringify, p_request)
            
            // Remove Person from claim workflow
            ClaimWorkflowModel.removePersonInApp(doc.p.fn  + " " + doc.p.ln + "@|@" + doc._id.stringify, p_request)
            
          }
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

  // Find all documents with projection
  def find(p_query:BSONDocument, p_projection:BSONDocument) = {
    col.find(p_query, p_projection).cursor[BSONDocument](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents with projection using session
  def find(p_query:BSONDocument, p_projection:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_projection).cursor[BSONDocument](ReadPreference.primary).collect[List]()
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
                    EntitlementValue(leavepolicy.ent.e1.e, leavepolicy.ent.e1.s, leavepolicy.ent.e1.cf),
                    EntitlementValue(leavepolicy.ent.e2.e, leavepolicy.ent.e2.s, leavepolicy.ent.e2.cf),
                    EntitlementValue(leavepolicy.ent.e3.e, leavepolicy.ent.e3.s, leavepolicy.ent.e3.cf),
                    EntitlementValue(leavepolicy.ent.e4.e, leavepolicy.ent.e4.s, leavepolicy.ent.e4.cf),
                    EntitlementValue(leavepolicy.ent.e5.e, leavepolicy.ent.e5.s, leavepolicy.ent.e5.cf),
                    EntitlementValue(leavepolicy.ent.e6.e, leavepolicy.ent.e6.s, leavepolicy.ent.e6.cf),
                    EntitlementValue(leavepolicy.ent.e7.e, leavepolicy.ent.e7.s, leavepolicy.ent.e7.cf),
                    EntitlementValue(leavepolicy.ent.e8.e, leavepolicy.ent.e8.s, leavepolicy.ent.e8.cf),
                    EntitlementValue(leavepolicy.ent.e9.e, leavepolicy.ent.e9.s, leavepolicy.ent.e9.cf),
                    EntitlementValue(leavepolicy.ent.e10.e, leavepolicy.ent.e10.s, leavepolicy.ent.e10.cf)
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
  def insertOnNewSignUp(p_doc:Person, p_country:String,  p_eid:String)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,null)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        // Create person's leave profile from CONFIG LEAVE POLICY
        Thread sleep 3000  // Wait 3 second make sure leave policy and setting is created
        ConfigLeavePolicyModel.find(
            BSONDocument(
                "ct" -> p_country,
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
                    EntitlementValue(configleavepolicy.ent.e1.e, configleavepolicy.ent.e1.s, configleavepolicy.ent.e1.cf),
                    EntitlementValue(configleavepolicy.ent.e2.e, configleavepolicy.ent.e2.s, configleavepolicy.ent.e2.cf),
                    EntitlementValue(configleavepolicy.ent.e3.e, configleavepolicy.ent.e3.s, configleavepolicy.ent.e3.cf),
                    EntitlementValue(configleavepolicy.ent.e4.e, configleavepolicy.ent.e4.s, configleavepolicy.ent.e4.cf),
                    EntitlementValue(configleavepolicy.ent.e5.e, configleavepolicy.ent.e5.s, configleavepolicy.ent.e5.cf),
                    EntitlementValue(configleavepolicy.ent.e6.e, configleavepolicy.ent.e6.s, configleavepolicy.ent.e6.cf),
                    EntitlementValue(configleavepolicy.ent.e7.e, configleavepolicy.ent.e7.s, configleavepolicy.ent.e7.cf),
                    EntitlementValue(configleavepolicy.ent.e8.e, configleavepolicy.ent.e8.s, configleavepolicy.ent.e8.cf),
                    EntitlementValue(configleavepolicy.ent.e9.e, configleavepolicy.ent.e9.s, configleavepolicy.ent.e9.cf),
                    EntitlementValue(configleavepolicy.ent.e10.e, configleavepolicy.ent.e10.s, configleavepolicy.ent.e10.cf)
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
          
          if (oldperson.get.p.fn != p_doc.p.fn || oldperson.get.p.ln != p_doc.p.ln) {
            
            // Update name on leave
            LeaveModel.find(
                BSONDocument(
                    "$or" -> BSONArray(
                        BSONDocument("pid"->p_doc._id.stringify),
                        BSONDocument("wf.aprid"->BSONDocument("$in"->List(p_doc._id.stringify))),
                        BSONDocument("wf.aprbyid"->BSONDocument("$in"->List(p_doc._id.stringify))),
                        BSONDocument("wf.rjtbyid"->p_doc._id.stringify),  
                        BSONDocument("wf.cclbyid"->p_doc._id.stringify)
                    )
                ),
                p_request
            ).map { leaves =>  
              leaves.foreach { leave => {
                val pn = if (leave.pid == p_doc._id.stringify) { p_doc.p.fn + " " + p_doc.p.ln } else { leave.pn }                
                val aprn = if ( leave.wf.aprid.indexOf(p_doc._id.stringify)!= -1 ) { leave.wf.aprn.updated(leave.wf.aprid.indexOf(p_doc._id.stringify), p_doc.p.fn + " " + p_doc.p.ln) } else { leave.wf.aprn }
                val aprbyn = if (leave.wf.aprbyn.isDefined) { if ( leave.wf.aprbyid.get.indexOf(p_doc._id.stringify)!= -1 ) { Option(leave.wf.aprbyn.get.updated(leave.wf.aprbyid.get.indexOf(p_doc._id.stringify), p_doc.p.fn + " " + p_doc.p.ln)) } else { leave.wf.aprbyn } } else { None }
                val rjtbyn = if (leave.wf.rjtbyn.isDefined) { if ( leave.wf.rjtbyid.get == p_doc._id.stringify ) { Option(p_doc.p.fn + " " + p_doc.p.ln) } else { leave.wf.rjtbyn } } else { None }
                val cclbyn = if (leave.wf.cclbyn.isDefined) { if ( leave.wf.cclbyid.get == p_doc._id.stringify ) { Option(p_doc.p.fn + " " + p_doc.p.ln) } else { leave.wf.cclbyn } } else { None }
                LeaveModel.update(BSONDocument("_id" -> leave._id), leave.copy(pn=pn, wf=leave.wf.copy(aprn=aprn, aprbyn=aprbyn, rjtbyn=rjtbyn, cclbyn=cclbyn)), p_request)
              } }  
            }
                      
            // Update event
            EventModel.find(BSONDocument("lrr"->BSONDocument("$in"->List(oldperson.get.p.fn + " " + oldperson.get.p.ln + "@|@" + oldperson.get._id.stringify))), p_request).map { events => {
              events.foreach { event => {
                val lrr = event.lrr.map { lrr => if (lrr==oldperson.get.p.fn + " " + oldperson.get.p.ln + "@|@" + oldperson.get._id.stringify) p_doc.p.fn + " " + p_doc.p.ln + "@|@" + p_doc._id.stringify else lrr}
                EventModel.update(BSONDocument("_id" -> event._id), event.copy(lrr=lrr), p_request)
              } }
            } } 
            
            // Update claim
            ClaimModel.find(BSONDocument("p.id"->oldperson.get._id.stringify), p_request).map { claims => {
              claims.foreach { claim => {
                ClaimModel.update(BSONDocument("_id" -> claim._id), claim.copy(p=claim.p.copy(n=p_doc.p.fn + " " + p_doc.p.ln)), p_request)
              } }
            } }
            
            // Update claim category
            ClaimCategoryModel.find(BSONDocument("app"->BSONDocument("$in"->List(oldperson.get.p.fn + " " + oldperson.get.p.ln + "@|@" + oldperson.get._id.stringify))), p_request).map { claimcategories => {
              claimcategories.foreach { claimcategory => {
                val app = claimcategory.app.map { app => if (app==oldperson.get.p.fn + " " + oldperson.get.p.ln + "@|@" + oldperson.get._id.stringify) p_doc.p.fn + " " + p_doc.p.ln + "@|@" + p_doc._id.stringify else app}
                ClaimCategoryModel.update(BSONDocument("_id" -> claimcategory._id), claimcategory.copy(app=app), p_request)
              } }
            } }
            
            // Update claim workflow            
            ClaimWorkflowModel.updatePersonName(oldperson.get.p.fn + " " + oldperson.get.p.ln + "@|@" + oldperson.get._id.stringify, p_doc.p.fn + " " + p_doc.p.ln + "@|@" + p_doc._id.stringify, p_request)
            
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
      case 1 => if(p_doc.wd.wd1 != "Off"){ return true }
      case 2 => if(p_doc.wd.wd2 != "Off"){ return true } 
      case 3 => if(p_doc.wd.wd3 != "Off"){ return true } 
      case 4 => if(p_doc.wd.wd4 != "Off"){ return true } 
      case 5 => if(p_doc.wd.wd5 != "Off"){ return true } 
      case 6 => if(p_doc.wd.wd6 != "Off"){ return true }
      case 7 => if(p_doc.wd.wd7 != "Off"){ return true }
    }
    return false
  }
  
  def isHalfWorkDay(p_doc: Person, p_Date: DateTime): Boolean = {
    p_Date.getDayOfWeek() match {
      case 1 => if(p_doc.wd.wd1 == "Half"){ return true }
      case 2 => if(p_doc.wd.wd2 == "Half"){ return true } 
      case 3 => if(p_doc.wd.wd3 == "Half"){ return true } 
      case 4 => if(p_doc.wd.wd4 == "Half"){ return true } 
      case 5 => if(p_doc.wd.wd5 == "Half"){ return true } 
      case 6 => if(p_doc.wd.wd6 == "Half"){ return true }
      case 7 => if(p_doc.wd.wd7 == "Half"){ return true }
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
  
  def isSubstituteManagerFor(p_id:String, p_smid:String, p_request:RequestHeader) = {
    val maybe_person = Await.result(PersonModel.findOne(BSONDocument("_id"->BSONObjectID(p_id)), p_request), Tools.db_timeout)
    val person = maybe_person.getOrElse(PersonModel.doc)
    if ( person.p.smgrid == p_smid) {
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
  	
  def buildReporting(p_id:String, p_request:RequestHeader) : JsValue  = {
    val person = Await.result(this.findOne(BSONDocument("_id"->BSONObjectID(p_id)), p_request), Tools.db_timeout).get
    val reporting_persons = Await.result(this.find(BSONDocument("_id" -> BSONDocument("$ne" -> BSONObjectID(p_id)), "p.mgrid" -> p_id), p_request), Tools.db_timeout)
    if (reporting_persons.isEmpty) {
      Json.parse("{\"name\": \"" + person.p.fn + " " + person.p.ln + "\",\"title\": \"" + person.p.pt + "\"}")
    } else {
      val reporting_persons_json_list = reporting_persons.map { reporting_person => this.buildReporting(reporting_person._id.stringify, p_request) }
      Json.obj("name" -> (person.p.fn + " " + person.p.ln), "title" -> person.p.pt, "children" -> Json.toJson(reporting_persons_json_list))
    }
  }
  
  
  // Optional - Find all document with projection and sorting
  // def find(p_query:BSONDocument,p_projection:BSONDocument,p_sort:BSONDocument) = {}
  
}