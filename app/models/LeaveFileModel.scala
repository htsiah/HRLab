package models

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import play.modules.reactivemongo.json._

import reactivemongo.api._
import reactivemongo.api.gridfs.GridFS

import utilities.DbConnUtility

object LeaveFileModel {
  
  val gridFS = this.getGridFS
  val gridFSBSON = GridFS(DbConnUtility.leave_file_db)
  
  // let's build an index on our gridfs chunks collection if none
  gridFS.ensureIndex().onComplete {
    case index =>
      Logger.info(s"Checked index, result is $index")
  }
  
  private def getGridFS = {
    import play.modules.reactivemongo.json.collection._
    GridFS[JSONSerializationPack.type](DbConnUtility.leave_file_db)
  }
    
}