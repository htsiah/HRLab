package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ Json, JsObject, JsString }
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}

import models.{LeaveModel, LeaveFileModel}

import reactivemongo.bson.{BSONObjectID,BSONDocument}
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson._

class LeaveReportController @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with ReactiveMongoComponents with Secured {
  
  import MongoController.readFileReads
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleave <- LeaveModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybefiles <- LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> maybeleave.get.docnum.toString(), "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]()
    } yield {
      maybeleave.map( leave => {
        val filename = if ( maybefiles.isEmpty ) { "" } else { maybefiles.head.metadata.value.get("filename").getOrElse("") }
        Ok(views.html.leavereport.view(leave, filename.toString().replaceAll("\"", "")))
      }).getOrElse(NotFound(views.html.error.onhandlernotfound()))
    }
  }}
    
}