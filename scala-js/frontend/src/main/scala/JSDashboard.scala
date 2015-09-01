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
      //jQuery("#loading-bar").cssContent("width", tick.progress.percent)
      // TODO 
    }
    socket
  }

  @JSExport
  def startOn(id: String) = {
    val socket = populateSocket(new WebSocket(socketUrl(id)))
  }


  def main(): Unit = {
    jQuery("#results").append("Hi dear!")
  }
}