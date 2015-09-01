package panop
package web
package shared
/**
 * Shared bean for backend-frontend communication using ScalaJs
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
package object models {

  case class SProgress(percent: Double, nbExplored: Int, nbFound: Int, nbMatches: Int, nbMissed: Int)
  case class SResult(url: String, matches: String)

  case class DashboardTick(progress: SProgress, results: List[SResult])
}