package controllers

import models.ClaimFileModel

import javax.inject.Inject

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}
import play.modules.reactivemongo.MongoController.readFileReads

import play.api.libs.json.{ Json, JsString }
import play.modules.reactivemongo.json._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson._

import scala.concurrent.Future

import utilities.{DbLoggerUtility}

class ClaimFileController @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with ReactiveMongoComponents with Secured {
  
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  
  def insert(p_lk: String) = withAuth (parse.maxLength(5 * 1014 * 1024, gridFSBodyParser(ClaimFileModel.gridFS))) { username => implicit request => {
    
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
            err.printStackTrace()
            DbLoggerUtility.error("Error upload receipt in Claim #" + p_lk + ". " +  err.printStackTrace(), request)
            Ok(Json.obj("status" -> "error upload file")).as("application/json")
          }
        }
        
        // when the upload is complete, we add the some metadata
        val futureUpdate = for {
          file <- futureFile
          
          // here, the file is completely uploaded, so it is time to update the article
          updateResult <- {        
            ClaimFileModel.gridFS.files.update(
                Json.obj("_id" -> file.id),
                Json.obj("$set" -> Json.obj("metadata" -> Json.obj(
                    "eid" -> request.session.get("entity"), 
                    "filename" -> file.filename, 
                    "lk" -> p_lk, 
                    "f" -> "claim", 
                    "cby" -> request.session.get("username")
                )))
            )
          }
        } yield updateResult
        
        // Return error on update metadata
        futureUpdate.onFailure{
          case err => {
            err.printStackTrace();
            DbLoggerUtility.error("Error update medata in Claim #" + p_lk + ". " +  err.printStackTrace(), request)
            Ok(Json.obj("status" -> "error update metadata")).as("application/json")
          }
        }    
         
        // Return success
        for {
          file <- futureFile
        } yield {
          val id = file.id.toString()
          Ok(Json.obj("status"->"success", "id"->id.substring(1, id.length()-1))).as("application/json")
        }
        
      }
    }
            
  }}
  
  def view(p_id: String) = Action.async { request => {
    
    // find the matching attachment
    val file = ClaimFileModel.findById(p_id, request)
    
    // Stream to client
    serve[JsString, JSONReadFile](ClaimFileModel.gridFS)(file)
    
  }}
  
  def viewByLK(p_lk: String) = Action.async { request => {
    
    // find the matching attachment, if any, and streams it to the client   
    val file = ClaimFileModel.findByLK(p_lk, request)
    
    // Stream to client
    serve[JsString, JSONReadFile](ClaimFileModel.gridFS)(file)
    
  }}
  
  def delete(p_id:String) = withAuth { username => implicit request => {
        
    ClaimFileModel.removeById(p_id, request)
    Future(Ok(Json.obj("status" -> "success")).as("application/json"))
    
  }}
  
  def deleteByLK(p_lk:String) = withAuth { username => implicit request => {
        
    ClaimFileModel.removeByLK(p_lk, request)
    Future(Ok(Json.obj("status" -> "success")).as("application/json"))
    
  }}
   
  
}