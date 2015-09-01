package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import panop.web.models.MResult
import panop.web.common.Settings

import org.joda.time.DateTime

/**
 * Default controller. Redirect in case of error, bad request, page not found, etc.
 * More importantly, cleanup the DB of old searches.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
object Defaults extends GlobalSettings {

  override def onBadRequest(request: RequestHeader, error: String) = Future.successful {
    Redirect(routes.Main.home())
  }

  override def onError(request: RequestHeader, ex: Throwable) = Future.successful {
    Redirect(routes.Main.home())
  }

  override def onHandlerNotFound(request: RequestHeader) = Future.successful {
    Redirect(routes.Main.home())
  }

  override def onStart(app: Application) = {
    val oldDate = new DateTime().minusDays(Settings.cleanAfterDays)
    if (Try(MResult.clean(oldDate)).isFailure) Logger.error("Could not cleanup data on startup...")
  }
}
