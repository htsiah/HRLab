package models

import play.api.Play
import scala.util.Try

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import play.modules.reactivemongo.json._

import reactivemongo.api._
import reactivemongo.api.gridfs.GridFS

object LeaveFileModel {
  
  private val dbname = Play.current.configuration.getString("mongodb_leave_file").getOrElse("leave_file")
  private val uri = Play.current.configuration.getString("mongodb_leave_file_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  val gridFS = this.getGridFS
  val gridFSBSON = GridFS(db)
  
  private def getGridFS = {
    import play.modules.reactivemongo.json.collection._
    GridFS[JSONSerializationPack.type](db)
  }
  
  def init() = {
    // let's build an index on our gridfs chunks collection if none
    gridFS.ensureIndex().onComplete {
      case index =>
        Logger.info(s"Checked index, result is $index")
    }
  }
  
  def close() = {
    driver.close()
  }
    
}