package jobs

import scala.util.{Success, Failure,Try}
import org.joda.time.DateTime

import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.nodeset.Authenticate

case class Job (
    _id: BSONObjectID,
    n: String,
    sdat: Option[DateTime],
    edat: Option[DateTime]
)

object Job {
  
  // Use Reader to deserialize document automatically
  implicit object JobBSONReader extends BSONDocumentReader[Job] {
    def read(doc: BSONDocument): Job = {
      Job(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("_id").get,
          doc.getAs[BSONDateTime]("sdat").map(dt => new DateTime(dt.value )),
          doc.getAs[BSONDateTime]("edat").map(dt => new DateTime(dt.value ))
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object JobBSONWriter extends BSONDocumentWriter[Job] {
    def write(job: Job): BSONDocument = {
      BSONDocument(
          "_id" -> job._id,
          "n" -> job.n,
          "sdat" -> job.sdat.map(date => BSONDateTime(date.getMillis)),
          "edat" -> job.edat.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_job").getOrElse("job")
  private val uri = Play.current.configuration.getString("mongodb_job_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
     parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("job")
  
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