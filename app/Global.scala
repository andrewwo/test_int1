import play.Logger
import play.api._
import models._
import play.api.db.DB
import play.api.GlobalSettings
import play.api.Application
import play.api.Play.current
import scala.slick.session.{ Database, Session }
import Database.threadLocalSession
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable
import play.api.mvc._
import play.api.mvc.Results._
import slick.jdbc.StaticQuery

object Global extends GlobalSettings {
	override def onStart(app: Application) {
		InitialData.init()
	}
	
	object InitialData {

		def init() {
			lazy val database = Database.forDataSource(DB.getDataSource())

			database withSession {
				val tableList = MTable.getTables.list()
						val tableMap = tableList.map {
					t => (t.name.name, t)
				}.toMap

				if (!tableMap.contains("user")) {
					UserDAO.ddl.create
					UserDAO.save(User("admin","admin","a.mack@kainos.com", "Angus", "Mack", true))
					UserDAO.save(User("awood","awood","a.wood@kainos.com", "Andrew", "Wood", true))
					UserDAO.save(User("rmutter","rmutter","r.mutter@kainos.com", "Richard", "Mutter", true))
				}
				if (!tableMap.contains("client")) {
					ClientDAO.ddl.create
					ClientDAO.save(Client("test client 1","some notes"))
					ClientDAO.save(Client("test client 2","some more notes"))
				}
				if (!tableMap.contains("uploaded_workbooks")) {
					DBWorkbookDAO.ddl.create
				}
			}
		}
	}
}

