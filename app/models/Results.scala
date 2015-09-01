package panop.models


import anorm._
import anorm.SqlParser._
import play.api.db.DB
import anorm.RowParser._

import play.api.Play.current

case class Result(id: String, url: String, matches: String)

/**
 * Storing results.
 *
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
object Result extends SQLModel[Result] {

  val tableName = "results"

  val lineParser = {
    get[String](s"${this.tableName}.id") ~
    get[String](s"${this.tableName}.url") ~
    get[String](s"${this.tableName}.matches") map {
      case id ~ url ~ matches => Result(id, url, matches)
    }
  }

  def store(id: String, url: String, matches: String): Unit = DB.withTransaction { implicit conn =>
    insert('id -> id, 'url -> url, 'matches -> matches)().commitDirect
  }
  def fetchById(id: String): List[Result] = select('id -> id).toList
}
