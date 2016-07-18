package utilities

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

case class SKeyword (
     _id: BSONObjectID,
     n: String,
     v: Option[List[String]]
)

object KeywordUtility {

    implicit object ConfigKeywordBSONReader extends BSONDocumentReader[SKeyword] {
      def read(p_doc: BSONDocument): SKeyword = {
        SKeyword(
            p_doc.getAs[BSONObjectID]("_id").get,
            p_doc.getAs[String]("n").get,
            p_doc.getAs[List[String]]("v").map(v => v)
        )
      }
    }
    
    private val col = DbConnUtility.config_db.collection("skeyword")
    
    def findOne(p_query:BSONDocument) = {
      col.find(p_query).one[SKeyword]
    }

}