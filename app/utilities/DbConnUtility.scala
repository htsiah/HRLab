package utilities

import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api.{MongoDriver, MongoConnection}

import scala.util.Try

object DbConnUtility {
  
  private val uri = Play.current.configuration.getString("mongodb_directory_uri").getOrElse("mongodb://localhost")
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val dir_db_name = Play.current.configuration.getString("mongodb_directory").getOrElse("directory")
  private val config_db_name = Play.current.configuration.getString("mongodb_config").getOrElse("config")
  private val admin_db_dbname = Play.current.configuration.getString("mongodb_admin").getOrElse("admin")
  private val logger_db_name = Play.current.configuration.getString("mongodb_dblogger").getOrElse("dblogger")
  private val calendar_db_name = Play.current.configuration.getString("mongodb_calendar").getOrElse("calendar")
  private val leave_db_dbname = Play.current.configuration.getString("mongodb_leave").getOrElse("leave")
  private val leave_file_db_name = Play.current.configuration.getString("mongodb_leave_file").getOrElse("leave_file")
  private val job_db_name = Play.current.configuration.getString("mongodb_job").getOrElse("job")
  private val audit_log_db_name = Play.current.configuration.getString("mongodb_audit_log").getOrElse("audit_log")
  
  val dir_db = connection.get.db(dir_db_name)
  val config_db = connection.get.db(config_db_name)
  val admin_db = connection.get.db(admin_db_dbname)
  val logger_db = connection.get.db(logger_db_name)
  val calendar_db = connection.get.db(calendar_db_name)
  val leave_db = connection.get.db(leave_db_dbname)
  val leave_file_db = connection.get.db(leave_file_db_name)
  val job_db = connection.get.db(job_db_name)
  val audit_log_db = connection.get.db(audit_log_db_name)

  def close() = {
    driver.close()
  }
  
}