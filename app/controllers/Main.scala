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
import play.api.libs.Crypto
import play.libs.Akka

import scala.util.{Try, Failure, Success}
import scala.util.matching.Regex

import akka.actor._

import panop._
import panop.web.workers.Persistence

// TODO: there must be a much nicer way than using that bean and converting it
// afterwards, but it does not matter right now.
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
  import panop.web.common.Enrichments._
  import panop.web.common.Settings

  /* Helpers */

  /* Verifying that the query is in proper normal form */
  val isQueryInNormalForm: Constraint[String] =
  Constraint("constraints.isQueryInNormalForm")({
    plainText =>
      com.QueryParser(plainText) match {
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

  // TODO: use the unapply from the case class above might
  // avoid having to write that down again!
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
        /* NB: the query here is proper, as well as all regexes, no need to
         * check them once more */
        val id = Crypto.generateToken
        val asys = ActorSystem.create(s"Panop$id")
        val master = asys.actorOf(Props(new Master(asys, rawQuery.maxSlaves)))
        val parsedQuery = com.QueryParser(rawQuery.query).left.get
        val domain = rawQuery.domain match {
          case "" => None
          case x => Some(x)
        }
        val mode = rawQuery.mode match {
          case "BFSMode" => com.BFSMode
          case "DFSMode" => com.DFSMode
          case "RNDMode" => com.RNDMode
          case _ => panop.Settings.defMode /* Jumping back to default mode */
        }
        /* Launching a query */
        val search = com.Search(
          com.Url(rawQuery.url),
          com.Query(
            poss = parsedQuery._1,
            negs = parsedQuery._2,
            maxDepth = rawQuery.depth,
            domain = domain,
            mode = mode,
            ignoredFileExtensions = new Regex(rawQuery.ignExts),
            boundaries = (new Regex(rawQuery.topBnds),
              new Regex(rawQuery.botBnds))
          )
        )
        val persistence = Akka.system.actorOf(Props(
          new Persistence(id, master)))
        master ! search
        persistence ! "persist"
        Cache.set(id, (asys, master))
        Redirect(routes.Search.dashboard(id))
      }
    )
  }
}
