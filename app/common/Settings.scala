package common

import play.api._
import play.api.mvc._

import scala.concurrent.duration._

/**
 * Global Settings for panop-web.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
object Settings {
  val updateRate = 500.milliseconds // TODO: put that in Play configuration files
  val defDepth = 5 // TODO: idem
}