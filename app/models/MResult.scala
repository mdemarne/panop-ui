package panop.web.models

import anorm._
import anorm.SqlParser._
import play.api.db.DB

import anorm.RowParser._
import anorm.JodaParameterMetaData._

import play.api.Play.current

import org.joda.time.DateTime

case class MResult(id: String,
                   datetime: DateTime,
                   url: String,
                   matches: String)

/**
 * Storing results.
 * @author Mathieu Demarne (mathieu.demarne@gmail.com)
 */
object MResult extends SQLModel[MResult] {

  val tableName = "results"

  val lineParser = {
    get[String](s"${this.tableName}.id") ~
      get[DateTime](s"${this.tableName}.datetime") ~
      get[String](s"${this.tableName}.url") ~
      get[String](s"${this.tableName}.matches") map {
        case id ~ datetime ~ url ~ matches =>
          MResult(id, datetime, url, matches)
      }
  }

  def store(id: String, url: String, matches: String): Unit =
    DB.withTransaction { implicit conn =>
      if (opt('id -> id, 'url -> url).isEmpty) {
        insert('id -> id,
               'datetime -> new DateTime(),
               'url -> url,
               'matches -> matches)().commitDirect
      }
  }
  def fetchById(id: String): List[MResult] = select('id -> id).toList

  def clean(datetime: DateTime) = DbTransaction { implicit connection =>
    SQL(s"""DELETE FROM ${this.tableName} WHERE datetime < {datetime}""")
    .on('datetime -> datetime).executeUpdate()
  }
}
