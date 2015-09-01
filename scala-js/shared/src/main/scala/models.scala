package panop
package web
package shared
/**
 * Shared bean for backend-frontend communication using ScalaJs
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
package object models {

  case class Result(url: String, matches: String)
  case class Progress(percent: Double, nbExplored: Int, nbFound: Int, nbMatches: Int)

  case class DashboardTick(res: List[Result], progress: Progress)
}