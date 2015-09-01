import scala.scalajs.js
import org.scalajs.dom.raw._
import org.scalajs.dom

import upickle.default._

import scala.scalajs.js.annotation.JSExport

import panop.web.shared.models._

import org.scalajs.jquery.jQuery


/**
 * Frontend script for the dashboard.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
@JSExport
object JSDashboard extends js.JSApp {
  
  private def socketUrl(id: String) = dom.document.location.port match {
    case "9000" => s"ws://${dom.document.location.host}/async/dashboard/$id"
    case _ => s"wss://${dom.document.location.host}/async/dashboard$id" //TODO
  }

  private def populateSocket(socket: WebSocket): WebSocket = {
    socket.onopen = { (e: Event) => socket.send("ping") }
    socket.onerror = { (e: ErrorEvent) => println(s"Error while loading the dashboard WebSocket: $e") }
    socket.onclose = { (e: CloseEvent) => /* Nothing to do */ }
    socket.onmessage = { (e: MessageEvent) => 
      val tick = read[DashboardTick](e.data.toString)
      jQuery("#loading-bar").css("width", (tick.progress.percent*100).toInt + "%")
      jQuery("#loading-text").text(s"Explored ${tick.progress.nbExplored} over ${tick.progress.nbFound}, ${tick.progress.nbMatches} matches")
      val resultsList = tick.results.map {res =>
        s"""
          |<div class="row">
          |    <div class="col s12 l8">
          |      <a href="${res.url}" target="blank" class="deep-orange-text text-lighten-2">${res.url}</a></b>
          |    </div>
          |    <div class="col s12 l4">
          |      ${res.matches}
          |    </div>
          |</div>""".stripMargin
      }
      val results = resultsList match {
        case Nil => s"""NO RESULT SO FAR..."""
        case lst => s"""<div class="row"><div class="col s12 l8"><b>Link</b></div><div class="col s12 l4"><b>Matching</b></div></div>""" + lst.mkString
      }
      jQuery("#results").html(results)
    }
    socket
  }

  @JSExport
  def startOn(id: String) = {
    val socket = populateSocket(new WebSocket(socketUrl(id)))
  }


  def main(): Unit = println("Starting JSDashboard script...")
}