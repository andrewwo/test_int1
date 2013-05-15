package models

import play.api.db.DB
import play.api.Play.current
import scala.slick.driver.H2Driver.simple._

case class Account(id: Int, name: String)

object Accounts extends Table[Account]("ACCOUNT") {
  lazy val database = Database.forDataSource(DB.getDataSource())

  // -- Parsers
  def id = column[Int]("ID", O.PrimaryKey,O.AutoInc)
  def name = column[String]("NAME")

  def * = id ~ name  <> (Account.apply _, Account.unapply _)
  // -- Queries

  /**
   * Retrieve a Account from email.
   */
  def findById(id: Int): Option[Account] = {
    database withSession { implicit session =>
      val q1 = for (u <- Accounts if u.id === id) yield u
      q1.list.headOption.asInstanceOf[Option[Account]]
    }
  }

  /**
   * Retrieve all accounts.
   */
  def findAll = {
    for (u <- Accounts) yield u
  }
}
