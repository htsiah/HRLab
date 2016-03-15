package controllers

import models.LeaveFileModel

import javax.inject.Inject

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}

import play.api.libs.json.{ Json, JsObject, JsString }
import play.modules.reactivemongo.json._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson._

import org.joda.time.DateTime

import scala.concurrent.Future

class LeaveFileController @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with ReactiveMongoComponents with Secured {
  
  import MongoController.readFileReads
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  def insert(p_lk: String) = withAuth (parse.maxLength(1 * 1014 * 1024, gridFSBodyParser(LeaveFileModel.gridFS))) { username => implicit request => {
    
    request.body match {
      case Left(MaxSizeExceeded(length)) => {     
        
        // Return error
        Future(Ok(Json.obj("status" -> "exceed file size limit")).as("application/json"))
        
      }
      case Right(multipartForm) => {
        
        // here is the future file!
        val futureFile = request.body.right.get.files.head.ref
        
        // Return error on file upload
        futureFile.onFailure {
          case err => {
            err.printStackTrace();
            Ok(Json.obj("status" -> "error upload file")).as("application/json")
          }
        }
        
        // when the upload is complete, we add the some metadata
        val futureUpdate = for {
          file <- futureFile
          
          // here, the file is completely uploaded, so it is time to update the article
          updateResult <- {        
            LeaveFileModel.gridFS.files.update(
                Json.obj("_id" -> file.id),
                Json.obj("$set" -> Json.obj("metadata" -> Json.obj(
                    "eid" -> request.session.get("entity"), 
                    "lk" -> p_lk, 
                    "f" -> "leave", 
                    "cby" -> request.session.get("username")
                )))
            )
          }
        } yield updateResult
        
        // Return success
        futureUpdate.map { s =>
          Ok(Json.obj("status"->"success")).as("application/json")
        }.recover {
          case err => {
            err.printStackTrace();
            Ok(Json.obj("status" -> "error update metadata")).as("application/json")
          }
        }
      }
    }
            
  }}
  
  def view(p_id: String) = Action.async { request => {
    
    // find the matching attachment, if any, and streams it to the client
    val file = LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("_id" -> p_id))
    serve[JsString, JSONReadFile](LeaveFileModel.gridFS)(file)
    
  }}
  
  def viewByLK(p_lk: String) = Action.async { request => {
    
    // find the matching attachment, if any, and streams it to the client
    val file = LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> p_lk, "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false)))
    serve[JsString, JSONReadFile](LeaveFileModel.gridFS)(file)
    
  }}
    
  def delete(p_id:String) = withAuth { username => implicit request => {
    
    LeaveFileModel.gridFS.remove(Json toJson p_id).map(_ => 
      Ok(Json.obj("status" -> "success")).as("application/json")
    ).recover {
      case err => {
        err.printStackTrace();
        Ok(Json.obj("status" -> "error")).as("application/json")
      }
    }
    
  }}
  
  def deleteByLK(p_lk:String) = withAuth { username => implicit request => {
        
    LeaveFileModel.gridFS.find[JsObject, JSONReadFile](Json.obj("metadata.lk" -> p_lk, "metadata.f" -> "leave", "metadata.dby" -> Json.obj("$exists" -> false))).collect[List]().map { files =>
      val filesWithId = files.map { file => {
        LeaveFileModel.gridFS.files.update(
            Json.obj("_id" -> file.id),
            Json.obj("$set" -> Json.obj("metadata" -> Json.obj(     
                "eid" -> file.metadata.value.get("eid").get,
                "lk" -> file.metadata.value.get("lk").get,
                "f" -> file.metadata.value.get("f").get,
                "cby" -> file.metadata.value.get("cby").get,
                "ddat" -> BSONDateTime(new DateTime().getMillis),
                "dby" -> request.session.get("username")
            )))
        )
      }}
    }
    
    Future(Ok(Json.obj("status" -> "success")).as("application/json"))
    
  }}

}