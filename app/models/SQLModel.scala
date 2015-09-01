package panop.web.models

import java.sql.Connection

import anorm._
import play.api.db.DB
import play.api.libs.json.Writes
import scala.language.postfixOps
import play.api.Play.current
import anorm.SqlParser._

/** Courtesy of Damien Engels, see https://github.com/paullepoulpe/musicbot/blob/master/app/models/SQLModel.scala */
trait SQLModel[T] {
    protected def tableName: String
    protected def lineParser: RowParser[T]

    private def toSQLSelectors(params: Seq[NamedParameter]) = params.map(np => s"${np.name} = {${np.name}}").mkString(" AND ")
    private def toSQLUpdater(params: Seq[NamedParameter]) = params.map(np => s"${np.name} = {${np.name}}").mkString(" , ")
    private def toSQLFormal(params: Seq[NamedParameter]) = params.map(np => np.name).mkString("(", ", ", ")")
    private def toSQLParams(params: Seq[NamedParameter]) = params.map(np => s"{${np.name}}").mkString("(", ", ", ")")

    /** Returns all the elements in the table */
    protected[models] def all: DbTransaction[Seq[T]] = DbTransaction { implicit connection =>
        SQL(
            s"""
          SELECT *
          FROM ${this.tableName}
       """).as(lineParser *)

    }

    /** Inserts a new element, if no keyParser is specified, a generic string parser is used **/
    protected[models] def insert[K](params: NamedParameter*)(generatedKeyParser: ResultSetParser[K] = str(1).singleOpt) = DbTransaction { implicit connection =>
        SQL(
            s"""
          INSERT INTO ${this.tableName}
          ${toSQLFormal(params)}
          VALUES
          ${toSQLParams(params)}
       """).on(
                params: _*).executeInsert[K](generatedKeyParser)
    }

    protected[models] def drop[K](params: NamedParameter*) = DbTransaction { implicit connection =>
        SQL(
            s"""
       DELETE FROM ${this.tableName}
       WHERE
       ${toSQLSelectors(params)}
     """).on(
                params: _*).executeUpdate()
    }

    /** Updates the fields specified by toUpdate on all elements specified by the selectors **/
    protected[models] def update(toUpdate: NamedParameter*)(selectors: NamedParameter*): DbTransaction[Int] = DbTransaction { implicit connection =>
        SQL(
            s"""
          UPDATE ${this.tableName}
          SET
          ${toSQLUpdater(toUpdate)}
          WHERE
          ${toSQLSelectors(selectors)}
       """).on(
                toUpdate ++ selectors: _*).executeUpdate()
    }

    /** Returns an optional unique element that matches the parameters, returns None in case there is no such element **/
    protected[models] def opt(params: NamedParameter*): DbTransaction[Option[T]] = DbTransaction {
        implicit connection =>
            SQL(
                s"""
          SELECT *
          FROM ${this.tableName}
          WHERE
          ${toSQLSelectors(params)}
          LIMIT 1
       """).on(
                    params: _*).as(lineParser singleOpt)
    }

    /** Returns a unique element that matches the parameters, throws an exception in case there is no such element **/
    protected[models] def unique(params: NamedParameter*): DbTransaction[T] = opt(params: _*).map(_.get)

    /** Returns a count of elements that match the selection parameters **/
    protected[models] def count(params: NamedParameter*): DbTransaction[Int] = DbTransaction {
        val selectors = params match {
            case Nil => ""
            case _ =>
                s"""
                WHERE
                ${toSQLSelectors(params)}
              """
        }
        implicit connection =>
            SQL(
                s"""
          SELECT COUNT(*) AS count
          FROM ${this.tableName}
          ${selectors}
       """).on(
                    params: _*).as(int("count").single)
    }

    /** Normal Select statement, uses line parser to return a sequence of all elements retrieved **/
    protected[models] def select[A](params: NamedParameter*): DbTransaction[Seq[T]] = DbTransaction {
        implicit connection =>
            SQL(
                s"""
          SELECT *
          FROM ${this.tableName}
          WHERE
          ${toSQLSelectors(params)}
       """).on(
                    params: _*).as(lineParser *)
    }
}

/** Monadic representation of a Database Transaction */
protected[models] trait DbTransaction[+A] {
    def unit: Connection => A

    def map[B](f: A => B): DbTransaction[B] = DbTransaction {
        connection => f(unit(connection))
    }

    def flatMap[B](f: A => DbTransaction[B]): DbTransaction[B] = DbTransaction {
        connection => f(unit(connection)).unit(connection)
    }

    import scala.util.{ Try, Success, Failure }

    def commitBlocking(connection: Connection): Try[A] = Try { commitDirect(connection) }

    def commitDirect(implicit connection: Connection): A = {
        connection.setAutoCommit(false)
        val a = unit(connection)
        connection.commit()
        a
    }

    import scala.concurrent.{ Future, ExecutionContext }

    def commit(connection: Connection)(implicit ec: ExecutionContext): Future[A] = Future {
        connection.setAutoCommit(false)
        val a = unit(connection)
        connection.commit()
        a
    }
}

object DbTransaction {

    /** Helper to force monadic transaction to run on db */
    import scala.language.implicitConversions
    implicit def trans2data[T](trans: DbTransaction[T]) = DB.withTransaction(trans.commitDirect(_))

    /** Generate automatic writes for DBTransaction that will force the commit **/
    implicit def dbWrites[T](implicit writes: Writes[T]): Writes[DbTransaction[T]] = Writes {
        trans => writes.writes(trans)
    }

    def apply[A](f: Connection => A): DbTransaction[A] = new DbTransaction[A] {
        def unit = f
    }

    def constant[A](f: A): DbTransaction[A] = new DbTransaction[A] {
        def unit = _ => f
    }
}
