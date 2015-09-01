package panop.web.workers

import akka.actor._
import scala.util.{ Try, Success, Failure }

import scalaj.http._

import panop.web.models.MResult
import panop.web.common.Settings

import panop._

/**
 * Extracts data and do a local search once at a time.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
class Persistence(id: String, master: ActorRef) extends Actor with ActorLogging {
  import com._
  import panop.web.common.Enrichments._

  def receive = {
    case "persist" =>
      (master !? com.AskProgress, master !? com.AskResults) match {
        case (pp: com.AswProgress, pr: com.AswResults) =>
          pr.results.foreach {res =>
            if (Try(MResult.store(id, res.search.url.link, com.Query.printNormalForm(res.matches))).isFailure) log.error("Could not save results") /* TODO: better loggin */
          }
        if (pp.percent < 1.0 || pp.nbExplored == 1) self >! ("persist", Settings.updateRate) else self ! PoisonPill
        case _ => log.error("Could not save results")
    }
  }
}