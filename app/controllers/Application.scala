package controllers

import play.api._
import play.api.mvc._

import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.validation.Constraints._

import panop._
import panop.com._

case class RawQuery(
  query: String,
  url: String,
  depth: Int,
  domain: String,
  mode: String,
  ignoredExts: String,
  boundariesTop: String,
  boundariesBottom: String,
  maxSlaves: Int)

class Application extends Controller {

  /* Helpers */

  /* Verifying that the query is in proper normal form */
  val isQueryInNormalForm: Constraint[String] = Constraint("constraints.isQueryInNormalForm")({
  plainText =>
    QueryParser(plainText) match {
      case Right(r) => Invalid(r)
      case _ => Valid
    }
  })

  val launchForm = Form(
    mapping(
      "query" -> nonEmptyText.verifying(isQueryInNormalForm),
      "url" -> nonEmptyText,
      "depth" -> number.verifying(min(0), max(100)),
      "domain" -> text,
      "mode" -> text,
      "ignoredExts" -> text,
      "boundariesTop" -> text,
      "boundariesBottom" -> text,
      "maxSlaves" -> number.verifying(min(1), max(Query.defMaxSlaves))
    )(RawQuery.apply)(RawQuery.unapply))

  val defaultForm = launchForm.fill(RawQuery("", "", 5, "", "BFS", Query.defIgnExts.regex, Query.defTopBnds.regex, Query.defBotBnds.regex, Query.defSlaves))

  /* Actions */

  def home = Action {
    Ok(views.html.home(defaultForm))
  }

  def launch = Action { implicit request =>
    launchForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.home(formWithErrors)),
      rawQuery => {
        Ok(views.html.home(launchForm))
      }
    )
  }

}
