package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, Tools, DbConnUtility}

import scala.util.{Success,Failure}
import scala.util.control.Breaks._
import scala.concurrent.{Future,Await}
import scala.collection.mutable.ArrayBuffer
import org.joda.time.DateTime

case class Leave (
    _id: BSONObjectID,
    docnum: Int,
    pid: String,
    pn: String,
    lt: String,
    dt: String,
    fdat: Option[DateTime],
    tdat: Option[DateTime],
    r: String,
    uti: Double,
    cfuti: Double,
    ld: Boolean,
    wf: Workflow,
    sys: Option[System]
)

case class Workflow (
    s: String,
    aprid: List[String],
    aprn: List[String],
    aprbyid: Option[List[String]],
    aprbyn: Option[List[String]],
    rjtbyid: Option[String],
    rjtbyn: Option[String],
    cclbyid: Option[String],
    cclbyn: Option[String],
    aprmthd: String
)

object LeaveModel {

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
  
  implicit object WorkflowBSONReader extends BSONDocumentReader[Workflow] {
    def read(p_doc: BSONDocument): Workflow = {
      Workflow(
          p_doc.getAs[String]("s").get,
          p_doc.getAs[List[String]]("aprid").get,
          p_doc.getAs[List[String]]("aprn").get,
          p_doc.getAs[List[String]]("aprbyid").map(v => v),
          p_doc.getAs[List[String]]("aprbyn").map(v => v),
          p_doc.getAs[String]("rjtbyid").map(v => v),
          p_doc.getAs[String]("rjtbyn").map(v => v),
          p_doc.getAs[String]("cclbyid").map(v => v),
          p_doc.getAs[String]("cclbyn").map(v => v),
          p_doc.getAs[String]("aprmthd").get
      )
    }
  }
    
  implicit object LeaveBSONReader extends BSONDocumentReader[Leave] {
    def read(p_doc: BSONDocument): Leave = {
      Leave(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[Int]("docnum").get,
          p_doc.getAs[String]("pid").get,
          p_doc.getAs[String]("pn").get,
          p_doc.getAs[String]("lt").get,
          p_doc.getAs[String]("dt").get,
          p_doc.getAs[BSONDateTime]("fdat").map(dt => new DateTime(dt.value )),
          p_doc.getAs[BSONDateTime]("tdat").map(dt => new DateTime(dt.value )),
          p_doc.getAs[String]("r").get,
          p_doc.getAs[Double]("uti").get,
          p_doc.getAs[Double]("cfuti").get,
          p_doc.getAs[Boolean]("ld").getOrElse(true),
          p_doc.getAs[Workflow]("wf").get,
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
  
  implicit object WorkflowBSONWriter extends BSONDocumentWriter[Workflow] {
    def write(p_doc: Workflow): BSONDocument = {
      BSONDocument(
          "s" -> p_doc.s,
          "aprid" -> p_doc.aprid,
          "aprn" -> p_doc.aprn,
          "aprbyid" -> p_doc.aprbyid,
          "aprbyn" -> p_doc.aprbyn,
          "rjtbyid" -> p_doc.rjtbyid,
          "rjtbyn" -> p_doc.rjtbyn,
          "cclbyid" -> p_doc.cclbyid,
          "cclbyn" -> p_doc.cclbyn,
          "aprmthd" -> p_doc.aprmthd
      )     
    }
  }
  
  implicit object LeaveBSONWriter extends BSONDocumentWriter[Leave] {
    def write(p_doc: Leave): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "docnum" -> p_doc.docnum,
          "pid" -> p_doc.pid,
          "pn" -> p_doc.pn,
          "lt" -> p_doc.lt,
          "dt" -> p_doc.dt,
          "fdat" -> p_doc.fdat.map(date => BSONDateTime(date.getMillis)),
          "tdat" -> p_doc.tdat.map(date => BSONDateTime(date.getMillis)),
          "r" -> p_doc.r,
          "uti" -> p_doc.uti,
          "cfuti" -> p_doc.cfuti,
          "ld" -> p_doc.ld,
          "wf" -> p_doc.wf,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.leave_db.collection("leave")
  
  val doc = Leave(
      _id = BSONObjectID.generate,
      docnum = 0,
      pid = "",
      pn = "",
      lt = "",
      dt = "Full day",
      fdat = Some(new DateTime()),
      tdat = Some(new DateTime()),
      r = "",
      uti = 0.0,
      cfuti = 0.0,
      ld = false,
      wf = Workflow(
          s = "New",
          aprid = List(""),
          aprn = List(""),
          aprbyid = None,
          aprbyn = None,
          rjtbyid = None,
          rjtbyn = None,
          cclbyid =None,
          cclbyn = None,
          aprmthd = "Only manager is authorized to approve leave request"
      ),
      sys=None
  )
  
  private def updateSystem(p_doc:Leave) = {
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
  def insert(p_doc:Leave, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
	
  // Update document
  def update(p_query:BSONDocument, p_doc:Leave) = {
    val future = col.update(p_query, p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc))))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // Update document
  def update(p_query:BSONDocument,p_doc:Leave,p_request:RequestHeader) = {
    val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
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
    col.find(p_query).cursor[Leave](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Leave](ReadPreference.primary).collect[List]()
  }
	
  // Find and sort all documents using session
  def find(p_query:BSONDocument, p_sort:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).sort(p_sort).cursor[Leave](ReadPreference.primary).collect[List]()
  }
  
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Leave]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Leave]
  }
    
  /** Custom Model Methods **/ 
  
  def getAppliedDuration(p_leave:Leave, p_leavepolicy:LeavePolicy, p_person:Person, p_request:RequestHeader) = {    
    val isvalidonnonworkday = p_leavepolicy.set.nwd
    val applieddates = Tools.transformDateList(p_leave.fdat.get , p_leave.tdat.get)
    var appliedduration : Double = 0

    applieddates.map( applieddate => {
      if (isvalidonnonworkday) {
        if ( p_leave.dt=="Full day" ) {
          appliedduration = appliedduration + 1
        } else {
          appliedduration = appliedduration + 0.5
        }
      } else {
        val iscompanyholiday =  Await.result(CompanyHolidayModel.isCompanyHoliday(applieddate, p_person.p.off, p_request), Tools.db_timeout)
        if (PersonModel.isWorkDay(p_person, applieddate) && iscompanyholiday==false) {
          if ( p_leave.dt=="Full day" ) {
            if (PersonModel.isHalfWorkDay(p_person, applieddate)){
              appliedduration = appliedduration + 0.5
            } else {
              appliedduration = appliedduration + 1
            }
          } else {
            val HasPendingApprovedleave = Await.result(this.hasPendingApprovedleave(p_person._id.stringify, applieddate, p_request), Tools.db_timeout) 
            if (PersonModel.isHalfWorkDay(p_person, applieddate) && HasPendingApprovedleave){
              appliedduration = appliedduration + 0
            } else {
              appliedduration = appliedduration + 0.5
            }
          }
        }
      }
    })
    appliedduration
  }
     
  def isOnleave(p_pid:String, p_dt:String, p_date: DateTime, p_request:RequestHeader) = {
    for {
      leave <- this.findOne(BSONDocument("pid"->p_pid, "dt"->p_dt, "wf.s"->"Approved", "fdat"->BSONDocument("$lte"->BSONDateTime(p_date.getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(p_date.getMillis()))), p_request)
    } yield {
      if (leave.isEmpty) false else true
    }
  }
  
  def isOnleave(p_pid:String, p_date: DateTime, p_request:RequestHeader) = {
    for {
      leave <- this.findOne(BSONDocument("pid"->p_pid, "wf.s"->"Approved", "fdat"->BSONDocument("$lte"->BSONDateTime(p_date.getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(p_date.getMillis()))), p_request)
    } yield {
      if (leave.isEmpty) false else true
    }
  }
  
  def hasPendingApprovedleave(p_pid:String, p_date: DateTime, p_request:RequestHeader) = {
    for {
      leave <- this.findOne(BSONDocument("pid"->p_pid, "wf.s"->BSONDocument("$in"->List("Approved", "Pending Approval")), "fdat"->BSONDocument("$lte"->BSONDateTime(p_date.getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(p_date.getMillis()))), p_request)
    } yield {
      if (leave.isEmpty) false else true
    }
  }
  
  def isOverlap(p_leave:Leave, p_request:RequestHeader) = {
    val overlap = p_leave.dt match {
      case "Full day" => {
        val applieddates = Tools.transformDateList(p_leave.fdat.get , p_leave.tdat.get)
        var result : Boolean = false
        breakable { applieddates.foreach ( applieddate => {
          val leave =  Await.result(this.findOne(BSONDocument("pid"->p_leave.pid, "wf.s"->BSONDocument("$in"->List("Approved", "Pending Approval")), "fdat"->BSONDocument("$lte"->BSONDateTime(applieddate.getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(applieddate.getMillis()))), p_request), Tools.db_timeout)
          if (!leave.isEmpty) {
            result = true
            break
          }
        })}
        result
      }
      case "1st half" => {
        val leave = Await.result(this.findOne(BSONDocument("pid"->p_leave.pid, "dt"->BSONDocument("$in"->List(p_leave.dt, "Full day")), "wf.s"->BSONDocument("$in"->List("Approved", "Pending Approval")), "fdat"->BSONDocument("$lte"->BSONDateTime(p_leave.fdat.get.getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(p_leave.fdat.get.getMillis()))), p_request), Tools.db_timeout)
        if (leave.isEmpty) false else true
      }
      case "2nd half" => {
        val leave = Await.result(this.findOne(BSONDocument("pid"->p_leave.pid, "dt"->BSONDocument("$in"->List(p_leave.dt, "Full day")), "wf.s"->BSONDocument("$in"->List("Approved", "Pending Approval")), "fdat"->BSONDocument("$lte"->BSONDateTime(p_leave.fdat.get.getMillis())), "tdat"->BSONDocument("$gte"->BSONDateTime(p_leave.fdat.get.getMillis()))), p_request), Tools.db_timeout)
        if (leave.isEmpty) false else true
      }
    }
    
    overlap
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
  
  def setLockDown(p_query:BSONDocument) = {
    val f_leaves = this.find(p_query++(BSONDocument("ld"->false)))
    f_leaves.map { leaves => 
      leaves.map { leave => {
        this.update(BSONDocument("_id"->leave._id), leave.copy(ld=true))
        TaskModel.remove(BSONDocument("lk"->leave._id.stringify))
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid="", pn="System", lk=leave._id.stringify, c="Lock document. No action can be performed further."), leave.sys.get.eid.get)
      }}
    }
  }
  
  def setLockDown(p_query:BSONDocument, p_request:RequestHeader) = {
    val f_leaves = this.find(p_query++(BSONDocument("ld"->false)), p_request)
    f_leaves.map { leaves => 
      leaves.map { leave => {
        this.update(BSONDocument("_id"->leave._id), leave.copy(ld=true), p_request)
        TaskModel.remove(BSONDocument("lk"->leave._id.stringify), p_request)
        AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=p_request.session.get("id").get, pn=p_request.session.get("name").get, lk=leave._id.stringify, c="Lock document. No action can be performed further."), p_request=p_request)
      }}
    }
  }
  
} 