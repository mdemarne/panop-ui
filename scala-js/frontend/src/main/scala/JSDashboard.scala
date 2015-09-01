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
      jQuery("#loading-bar").css(s"width:${tick.progress.percent}%")
      jQuery("#loading-text").text(s"Explored ${tick.progress.nbExplored} over ${tick.progress.nbFound}, ${tick.progress.nbMatches} matches")
      val results = tick.results.map {res =>
        s"""<p><b><a href="${res.url}" target="blank">${res.url}</a></b> matching ${res.matches}</p>"""
      }
      jQuery("#results").text(results.mkString)
    }
    socket
  }

  @JSExport
  def startOn(id: String) = {
    val socket = populateSocket(new WebSocket(socketUrl(id)))
  }


  def main(): Unit = println("Starting JSDashboard script...")
}