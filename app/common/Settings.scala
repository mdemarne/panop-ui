package panop
package web
package common

import play.api._
import play.api.mvc._

import scala.concurrent.duration._

/**
 * Global Settings for panop-web.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 * @todo   change that to be in settings (better shared with panop-core)
 */
object Settings {
  // TODO: put that in Play configuration files
  val updateRate = 5.seconds
  val defDepth = 5
  val timeout = 60.seconds // TODO: who cares, this is a toy project!
  val cleanAfterDays = 7 // In days
}
