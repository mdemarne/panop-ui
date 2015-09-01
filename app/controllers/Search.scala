package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import play.api.cache._

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.validation.Constraints._

import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.libs.Akka

import scala.util.{Try, Failure, Success}
import scala.util.matching.Regex
import scala.util.Random

import akka.actor._

import upickle._

import panop._
import panop.web.shared.models._

import panop.web.common.Settings

/**
 * Global controller for a running search.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
class Search extends Controller {
  import panop.web.common.Enrichments._

  /* Actions */

  def dashboard(id: String) = Action { implicit request =>
    Cache.get(id) match {
      case Some((_, master: ActorRef)) => Ok(views.html.dashboard(id, staticResults = None))
      case _ => 
        Logger.error(s"Search with id $id does not exist.")
        Redirect(routes.Main.home()) // TODO
    }
  }

  def stop(id: String) = Action { implicit request =>
    Cache.get(id) match {
      case Some((asys: ActorSystem, _)) => 
        asys.shutdown()
        Redirect(routes.Main.home())
      case _ => Redirect(routes.Search.dashboard(id))
    }
    Logger.error(s"Search: cannot stop id $id, which must already be stopped or does not exist.")
    Redirect(routes.Search.dashboard(id))
  }

  /* Web Sockets */

  // TODO: there is no need of connecting twice to the master, but this is the way panop is buid. Should be changed in the future.
  def dashboardSocket(id: String) = WebSocket.acceptWithActor[String, JsValue](request => out => Props { new Actor {
    def receive = {
      /* Internal controls */
      case "ping" =>
        Cache.get(id) match {
          case Some((_, master: ActorRef)) =>
            (master !? com.AskProgress, master !? com.AskResults) match {
              case (pp: com.AswProgress, pr: com.AswResults) =>
                val tick = DashboardTick(SProgress(pp.percent, pp.nbExplored, pp.nbFound, pp.nbMatches), pr.results.map(r => SResult(r.search.url.link, com.Query.printNormalForm(r.matches))))
                out ! write(tick)
              case _ => Logger.error(s"Inconsistent message from Master.")
            }
          case _ => Logger.error(s"Search with id $id is not live!")
        }
        out ! JsNull
        self >! ("ping", Settings.updateRate) // TODO
      case othr => Logger.error(s"Display: web socket receiving inconsistent message: ${othr}.")
    }
  }})

}
