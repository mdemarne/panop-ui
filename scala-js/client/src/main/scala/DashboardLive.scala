import scala.scalajs.js
//import js.Dynamic.{ global => g }
//import upickle._
//import js.annotation.JSExport
import org.scalajs.dom

object DashboardLive extends js.JSApp {
  def main(): Unit = {
    dom.document.getElementById("results").textContent = "Hi dear!"
  }
}