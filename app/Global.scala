import play.api._
import play.api.mvc._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka

import akka.actor.Actor
import akka.actor.Props

import scala.concurrent.duration._
import jobs._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
     
    // Parameter 1: initial delat
    // Parameter 2: interval
    Akka.system(app).scheduler.schedule(1 seconds, 1 seconds) {
      // MonthlyLeaveProfileUpdateJob.run
    }
    
    Akka.system(app).scheduler.schedule(1 seconds, 30 seconds) {
      MonthlyLeaveProfileUpdateJob.run
    }
    
    Akka.system(app).scheduler.schedule(1 seconds, 4 hours) {
      // MonthlyLeaveProfileUpdateJob.run
    }
      	
  }
}