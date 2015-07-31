package models

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore}

import scala.util.{Success, Failure,Try}
import org.joda.time.DateTime

case class ConfigLeavePolicy (
    _id: BSONObjectID,
    lt: String,
    pt: String,
    set: ConfigLeavePolicySetting,
    ent: ConfigEntitlement,
    sys: Option[System]
)

case class ConfigLeavePolicySetting (
    g: String,
    acc: String,
    ms: String,
    dt: String,
    nwd: Boolean,
    cexp: Int,
    scal: Boolean
)

case class ConfigEntitlement (
    e1: Int,
    e1_s: Int,
    e1_cf: Int,
    e2: Int,
    e2_s: Int,
    e2_cf: Int,
    e3: Int,
    e3_s: Int,
    e3_cf: Int,
    e4: Int,
    e4_s: Int,
    e4_cf: Int,
    e5: Int,
    e5_s: Int,
    e5_cf: Int
)

object ConfigLeavePolicyModel {

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
  
  implicit object ConfigLeavePolicySettingBSONReader extends BSONDocumentReader[ConfigLeavePolicySetting] {
    def read(doc: BSONDocument): ConfigLeavePolicySetting = {
      ConfigLeavePolicySetting(
          doc.getAs[String]("g").get,
          doc.getAs[String]("acc").get,
          doc.getAs[String]("ms").get,
          doc.getAs[String]("dt").get,
          doc.getAs[Boolean]("nwd").get,
          doc.getAs[Int]("cexp").get,
          doc.getAs[Boolean]("scal").get
      )
    }
  }
  
  implicit object ConfigEntitlementBSONReader extends BSONDocumentReader[ConfigEntitlement] {
    def read(doc: BSONDocument): ConfigEntitlement = {
      ConfigEntitlement(
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
  
  implicit object ConfigLeavePolicyBSONReader extends BSONDocumentReader[ConfigLeavePolicy] {
    def read(doc: BSONDocument): ConfigLeavePolicy = {
      ConfigLeavePolicy(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("lt").get,
          doc.getAs[String]("pt").get,
          doc.getAs[ConfigLeavePolicySetting]("set").get,
          doc.getAs[ConfigEntitlement]("ent").get,
          doc.getAs[System]("sys").map(o => o)
      )
    }
  }

  private val dbname = Play.current.configuration.getString("mongodb_config").getOrElse("config")
  private val uri = Play.current.configuration.getString("mongodb_config_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("leavepolicy")

  private def updateSystem(p_doc:ConfigLeavePolicy) = {
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
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ConfigLeavePolicy](ReadPreference.primary).collect[List]()
  }
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ConfigLeavePolicy]
  }
  
}