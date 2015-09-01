package panop
package web
package shared
/**
 * Shared bean for backend-frontend communication using ScalaJs
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
object Utils {
  def representResult(url: String, matches: String) = {
    s"""
      |<div class="row">
      |    <div class="col s12 l8">
      |      <a href="${url}" target="blank" class="deep-orange-text text-lighten-2">${url}</a></b>
      |    </div>
      |    <div class="col s12 l4">
      |      ${matches}
      |    </div>
      |</div>""".stripMargin
  }
}