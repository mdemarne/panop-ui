package panop
package web
package common

import play.libs.Akka
import play.api.libs.concurrent.Execution.Implicits._

import akka.util.Timeout
import akka.actor.{ ActorRef, Props }
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/**
 * Enrichments for application-specific Akka interactions.
 * Courtesy of CrossStream.ch
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
object Enrichments {
  val defaultTimeoutDuration = 60.seconds
  implicit val defaultTimeout = Timeout(defaultTimeoutDuration)

  implicit class RichActorRef(actorRef: ActorRef) {
    /** Send a message and retrieve the answer in a blocking manner, using default timeout. */
    def !?(mess: Any) = {
      val resultProm = actorRef ? mess
      Await.result(resultProm, defaultTimeoutDuration)
    }
    /** Schedule a message to be sent in the future after a specific duration. */
    def >!(mess: Any, duration: FiniteDuration) = {
      Akka.system.scheduler.scheduleOnce(duration, actorRef, mess)
    }

    /* Schedule a message to be send in the future and periodically */
    def >>!(mess: Any, start: FiniteDuration, period: FiniteDuration) = {
      Akka.system.scheduler.schedule(start, period, actorRef, mess)
    }
  }
}