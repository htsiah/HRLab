package controllers

import play.api.mvc._
import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}

import play.api.libs.json.{ Json }
import play.modules.reactivemongo.json._

import play.api.libs.mailer._
import com.github.tototoshi.csv._

import scala.concurrent.{Future}
import javax.inject.Inject

import java.io.{FileInputStream}

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.api.gridfs.{ ReadFile, DefaultFileToSave }
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import reactivemongo.bson._

import models.PersonBulkImportModel

class PersonBulkImportController @Inject() (mailerClient: MailerClient, val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with ReactiveMongoComponents with Secured {

  // type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  
  def create = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Future.successful(Ok(views.html.personbulkimport.form()))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))    
    }
  }}
  
  def insert = withAuth (parse.maxLength(1 * 1014 * 1024, parse.multipartFormData)) { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      
      request.body match {
        case Left(MaxSizeExceeded(length)) => {     
          // Return error
          Future(Ok(Json.obj("status" -> "exceed file size limit")).as("application/json"))
        }
        case Right(multipartForm) => {
          val csv = multipartForm.file("csv").get            
          val futureFile: Future[ReadFile[BSONSerializationPack.type, BSONValue]] = PersonBulkImportModel.gridFSBSON.writeFromInputStream(DefaultFileToSave(csv.filename, csv.contentType), new FileInputStream(csv.ref.file))
            
          // Return error on file upload
          futureFile.onFailure {
            case err => {
              err.printStackTrace();
              Ok(Json.obj("status" -> "error import employee")).as("application/json")
            }            
          }
        
          // when the upload is complete, we add the some metadata
          val futureUpdate = for {
            file <- futureFile
          
            // here, the file is completely uploaded, so it is time to update the metadata
            updateResult <- {
              PersonBulkImportModel.gridFS.files.update(
                  BSONDocument("_id" -> file.id),
                  Json.obj("$set" -> Json.obj("metadata" -> Json.obj(
                      "eid" -> request.session.get("entity"), 
                      "filename" -> file.filename, 
                      "lk" -> "", 
                      "f" -> "personbulkimport", 
                      "cby" -> request.session.get("username")
                  )))
              )
            }
          } yield updateResult
            
          // Start importing employee
          futureUpdate.map { s =>
            
            
            val reader = CSVReader.open(csv.ref.file)
            println (csv.filename)
            println (csv.contentType)
            // println (reader.all())
            
            
            
            Ok(Json.obj("status"->"success")).as("application/json")
          }.recover {
            case err => {
              err.printStackTrace();
              Ok(Json.obj("status" -> "error update metadata")).as("application/json")
            }
          }
          
  
          
        } 
      }
      
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}