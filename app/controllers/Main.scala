package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import play.cache.Cache

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

import panop._
import panop.com._

import common.Settings

//TODO: there must be a much nicer way than using that bean and converting it afterwards
case class RawQuery(
  query: String,
  url: String,
  depth: Int,
  domain: String,
  mode: String,
  ignExts: String,
  topBnds: String,
  botBnds: String,
  maxSlaves: Int)

/**
 * Global controller to launch a search.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */

class Main extends Controller {
  import common.Enrichments._

  /* Helpers */

  /* Verifying that the query is in proper normal form */
  val isQueryInNormalForm: Constraint[String] = Constraint("constraints.isQueryInNormalForm")({
    plainText =>
      QueryParser(plainText) match {
        case Right(err) => Invalid(err)
        case _ => Valid
    }
  })

  /* Verifying that a regex is actually a regex */
  val isRegex: Constraint[String] = Constraint("constraints.isRegex")({
    plainText =>
      Try(new Regex(plainText)) match {
        case Failure(err) => Invalid(err.getMessage)
        case _ => Valid
      }
  })

  val launchForm = Form(
    mapping(
      "query" -> nonEmptyText.verifying(isQueryInNormalForm),
      "url" -> nonEmptyText,
      "depth" -> number.verifying(min(0), max(panop.Settings.defMaxDepth)),
      "domain" -> text,
      "mode" -> text,
      "ignExts" -> text.verifying(isRegex),
      "topBnds" -> text.verifying(isRegex),
      "botBnds" -> text.verifying(isRegex),
      "maxSlaves" -> number.verifying(min(1), max(panop.Settings.defMaxSlaves))
    )(RawQuery.apply)(RawQuery.unapply))

  val defaultForm = launchForm.fill(
    RawQuery(
      query = "", 
      url = "", 
      depth = panop.Settings.defDepth, 
      domain = "", 
      mode = panop.Settings.defMode.toString, 
      ignExts = panop.Settings.defIgnExts.regex, 
      topBnds = panop.Settings.defTopBnds.regex, 
      botBnds = panop.Settings.defBotBnds.regex, 
      maxSlaves = panop.Settings.defSlaves
    )
  )

  /* Actions */

  def home = Action (Ok(views.html.home(defaultForm)))

  def launch = Action { implicit request =>
    launchForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.home(formWithErrors)),
      rawQuery => {
        /* NB: the query here is proper, as well as all regexes, no need to check them once more */
        val id = (new Random).nextString(5)
        val asys = ActorSystem.create(s"Panop-$id")
        val master = asys.actorOf(Props(new Master(asys, rawQuery.maxSlaves)))
        val parsedQuery = QueryParser(rawQuery.query).left.get
        val domain = rawQuery.domain match {
          case "" => None
          case x => Some(x)
        }
        val mode = rawQuery.mode match {
          case "BFS" => BFSMode
          case "DFS" => DFSMode
          case "RND" => RNDMode
          case _ => panop.Settings.defMode /* Jumping back to default mode */
        }
        /* Launching a query */
        val search = Search(
          Url(rawQuery.url), 
          Query(parsedQuery._1, parsedQuery._2, rawQuery.depth, domain, mode, new Regex(rawQuery.ignExts), (new Regex(rawQuery.topBnds), new Regex(rawQuery.botBnds)))
        )
        master ! search
        Cache.set(id, (asys, master))
        Redirect(routes.Search.dashboard(id))
      }
    )
  }
}
