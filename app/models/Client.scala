package models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.TypeMapper._
import scala.slick.session.Session

case class Client(name: String, notes: String)

object ClientDAO extends Table[Client]("client") {

  // -- Parsers
  def name = column[String]("NAME",O.PrimaryKey)
  def notes = column[String]("NOTES")

  def * = name ~ notes <> (Client, Client.unapply _)
  // -- Queries

  def get(name:String)(implicit session:Session):Option[Client] = {
    val q = for (c <- ClientDAO if c.name === name) yield c
    
    q.list().headOption
  }
  
  def list(implicit session:Session):Seq[Client] = {
    val q = for(u <- ClientDAO) yield u
    q.list
  }
    
   def save(client:Client)(implicit session:Session){
    if(get(client.name) == None){
      val userToSave = client.copy()
      this.insert(userToSave)
    }else{
      val q = for(c <- ClientDAO if c.name === c.name) yield c
      q.update(client)
    }
  }
}
