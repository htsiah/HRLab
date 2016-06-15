package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import scala.util.{Success, Failure}
import org.joda.time.DateTime

import utilities.{System, SystemDataStore, Tools, DbConnUtility}

case class Task (
    _id: BSONObjectID,
    pid: String, // Person id
    lk: String, // Lookup Key
    ct: String, // Content in HTML Format
    bt: String, // Buttons in HTML Format
    cf: Boolean, // Completed Flag
    sys: Option[System]
)

object TaskModel {
  
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
  
  implicit object TaskBSONReader extends BSONDocumentReader[Task] {
    def read(p_doc: BSONDocument): Task = {
      Task(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("pid").get,
          p_doc.getAs[String]("lk").get,
          p_doc.getAs[String]("ct").get,
          p_doc.getAs[String]("bt").get,
          p_doc.getAs[Boolean]("cf").getOrElse(false),
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
  
  implicit object TaskBSONWriter extends BSONDocumentWriter[Task] {
    def write(p_doc: Task): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "pid" -> p_doc.pid,
          "lk" -> p_doc.lk,
          "ct" -> p_doc.ct,
          "bt" -> p_doc.bt,
          "cf" -> p_doc.cf,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.admin_db.collection("task")
  
  val doc = Task(
      _id = BSONObjectID.generate,
      pid = "",
      lk = "",
      ct = "",
      bt = "",
      cf = false,
      sys = None
  )
  
  private def updateSystem(p_doc:Task) = {
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
        
  // Update document
  def update(p_query:BSONDocument,p_doc:Task,p_request:RequestHeader) = {
    val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument) = {
    for {
      docs <- this.find(p_query)
    } yield {
      docs.foreach { doc => 
        val future = col.update(p_query, doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc))))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
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
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[Task](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Task](ReadPreference.primary).collect[List]()
  }
  
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Task]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Task]
  }
  
  // Optional - Find all document with sorting
  def find(p_query:BSONDocument, p_sort:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).sort(p_sort).cursor[Task](ReadPreference.primary).collect[List]()
  }
  
  /** Custom Model Methods **/ 
  
  def insert(p_key:Int, p_pid:String, p_lk:String, p_contentmap:Map[String, String], p_buttonsmap:Map[String, String], p_eid:String="", p_request:RequestHeader=null) = {
    for {
      maybeconfigtask <- ConfigTaskModel.findOne(BSONDocument("k"->p_key))
    } yield {
      maybeconfigtask.map { configtask => {
        val content = Tools.replaceSubString(configtask.ct, p_contentmap.toList)
        val buttons = Tools.replaceSubString(configtask.bt, p_buttonsmap.toList)
        val future = col.insert(this.doc.copy(_id = BSONObjectID.generate, pid=p_pid, lk=p_lk, ct=content, bt=buttons, sys = SystemDataStore.creation(p_eid,p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
        }
      } }
    }
  }
  
  def setCompleted(p_lk:String, p_request:RequestHeader) = {
    for {
      maybetask <- this.findOne(BSONDocument("lk"->p_lk), p_request)
    } yield {
      maybetask.map { task => (
          this.update(BSONDocument("lk"->p_lk), task.copy(cf=true), p_request)
      ) }
    }
  }
  
  // Do it using asynchronous as multiple may take longer time.
  def setCompletedMulti(p_lk:String, p_request:RequestHeader) = {
    for {
      maybetask <- this.find(BSONDocument("lk"->p_lk), p_request)
    } yield {
      maybetask.map { task => {
          this.update(BSONDocument("_id"->task._id), task.copy(cf=true), p_request)
      } }
    }
  }
  
  def setCompleted(p_pid:String, p_lk:String, p_request:RequestHeader) = {
    for {
      maybetask <- this.findOne(BSONDocument("pid"->p_pid, "lk"->p_lk), p_request)
    } yield {
      maybetask.map { task => (
          this.update(BSONDocument("pid"->p_pid, "lk"->p_lk), task.copy(cf=true), p_request)
      ) }
    }
  }
  
}