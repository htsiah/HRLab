package utilities

import play.api.Play
import org.joda.time.DateTime
import scala.concurrent.duration.Duration

object Tools {
  
  val hostname = Play.current.configuration.getString("domain_name").getOrElse("https://www.hrsifu.my")
  val db_timeout = Duration(Play.current.configuration.getInt("mongodb_timeout").getOrElse(5000), "millis")
  
  // Search a value in List's value 1 with #value# and replace with List value 2 
  def replaceSubString(text: String, rmap: List[(String, String)]): String = {
    if(rmap == Nil){
      text
    }else{
      val r = rmap.head
      val newText = text.replaceAll("#" + r._1 + "#", r._2)
      replaceSubString(newText, rmap.tail)
      
    }
  }
  
  // Create random boolean based on provide probability
  // 0.25 is 1/4
  // 0.5 is 1/2
  // http://stackoverflow.com/questions/20018423/random-boolean-generator
  def getRandomBoolean (probability : Double): Boolean = {
    math.random < probability
  }
  
  def cleanCSV ( p_text:String ) : String = {
    val replacedText = p_text.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\"", "\"\"")
    if (replacedText.contains(",")) { "\"" + replacedText + "\""  } else { replacedText }
  }
  
  def transformDateList(p_fdat:DateTime, p_tdat:DateTime) : List[DateTime] = {
    if(p_tdat.isAfter(p_fdat)){
      List(p_fdat) ::: transformDateList(p_fdat.plusDays(1),p_tdat)
    }else{
      List(p_tdat)
    }
  }
  
}