import scala.scalajs.js
//import js.Dynamic.{ global => g }
//import upickle._
//import js.annotation.JSExport
import org.scalajs.dom.raw._
import org.scalajs.dom

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
      
    }
    socket
  }

  def startOn(id: String) = {
    val socket = populateSocket(new WebSocket(socketUrl(id)))
  }


  def main(): Unit = {
    dom.document.getElementById("results").textContent = "Hi dear!"
  }
}