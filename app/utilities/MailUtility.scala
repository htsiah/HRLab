package utilities

import org.joda.time.DateTime
import scala.util.{Success, Failure,Try}

import play.api.Play
import play.api.Play.current
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Success, Failure}

import views._

import reactivemongo.api._
import reactivemongo.bson._

case class MailTemplate (
     _id: BSONObjectID,
     k: Int,
     s: String,
     b: String,
     sys: Option[System]
)

object MailUtility {
  
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
  
  implicit object MailTemplateBSONReader extends BSONDocumentReader[MailTemplate] {
    def read(doc: BSONDocument): MailTemplate = {
      MailTemplate(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[BSONInteger]("k").get.value,
          doc.getAs[String]("s").get,
          doc.getAs[String]("b").get,
          doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_config").getOrElse("config")
  private val uri = Play.current.configuration.getString("mongodb_config_uri").getOrElse("mongodb://localhost")
  // private val driver = new MongoDriver(ActorSystem("DefaultMongoDbDriver"))
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("mailtemplate")
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[MailTemplate]
  }
  
  // MailUtility.sendEmail(List("htsiah@hotmail.com"), "Test", "Test")
  def sendEmail(p_recipient: Seq[String], p_subject: String, p_body: String) =  {
    val email = Email(
        subject = p_subject,
        from = "HRSifu <noreply@hrsifu.my>",
        to = p_recipient,
        bodyHtml = Some(html.mail.default(Tools.hostname ,p_body).toString)
    )
    val future = Future( MailerPlugin.send(email) )
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // MailUtility.sendEmail(List("htsiah@hotmail.com"), List("htsiah@hotmail.com"), "Test", "Test")
  def sendEmail(p_recipient: Seq[String], p_cc: Seq[String], p_subject: String, p_body: String) =  {
    val email = Email(
        subject = p_subject,
        from = "HRSifu <noreply@hrsifu.my>",
        to = p_recipient,
        cc = p_cc,
        bodyHtml = Some(html.mail.default(Tools.hostname ,p_body).toString)
    )
    val future = Future( MailerPlugin.send(email) )
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // MailUtility.sendEmailConfig(List("htsiah@hotmail.com"), 1, Map[Something->Something])
  def sendEmailConfig(p_recipient: Seq[String], p_key: Int, p_replaceMap: Map[String, String]) =  {
    val future = this.findOne(BSONDocument("k"->p_key))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        future.map( emailtemplate => {
          val mailsubject = Tools.replaceSubString(emailtemplate.get.s,p_replaceMap.toList)
          val mailsbody = Tools.replaceSubString(emailtemplate.get.b,p_replaceMap.toList)
          this.sendEmail(p_recipient, mailsubject, mailsbody)
        }) 
      }
    }
  }
  
  // MailUtility.sendEmailConfig(List("htsiah@hotmail.com"), 1, Map[Something->Something])
  def sendEmailConfig(p_recipient: Seq[String], p_cc: Seq[String], p_key: Int, p_replaceMap: Map[String, String]) =  {
    val future = this.findOne(BSONDocument("k"->p_key))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        future.map( emailtemplate => {
          val mailsubject = Tools.replaceSubString(emailtemplate.get.s,p_replaceMap.toList)
          val mailsbody = Tools.replaceSubString(emailtemplate.get.b,p_replaceMap.toList)
          this.sendEmail(p_recipient, p_cc, mailsubject, mailsbody)
        }) 
      }
    }
  }
}