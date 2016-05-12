package jobs

import scala.util.{Success, Failure}
import org.joda.time.DateTime

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.nodeset.Authenticate

import utilities.DbConnUtility

case class Job (
    _id: BSONObjectID,
    n: String,
    sdat: Option[DateTime],
    edat: Option[DateTime]
)

object Job {
  
  // Use Reader to deserialize document automatically
  implicit object JobBSONReader extends BSONDocumentReader[Job] {
    def read(p_doc: BSONDocument): Job = {
      Job(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("_id").get,
          p_doc.getAs[BSONDateTime]("sdat").map(dt => new DateTime(dt.value )),
          p_doc.getAs[BSONDateTime]("edat").map(dt => new DateTime(dt.value ))
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object JobBSONWriter extends BSONDocumentWriter[Job] {
    def write(p_doc: Job): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "n" -> p_doc.n,
          "sdat" -> p_doc.sdat.map(date => BSONDateTime(date.getMillis)),
          "edat" -> p_doc.edat.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  private val col = DbConnUtility.job_db.collection("job")
  
  val doc = Job(
      _id = BSONObjectID.generate,
      n = "",
      sdat = Some(new DateTime()),
      edat = Some(new DateTime())
  )
  
  def insert(p_doc:Job)= {
    val future = col.insert(p_doc)
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
}