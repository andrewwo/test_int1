import play.api.GlobalSettings

import models._
import play.api.db.DB
import play.api.Application
import play.api.Play.current
import play.Logger

import scala.slick.driver.H2Driver.simple._
// import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.slick.jdbc.meta.MTable


object Global extends GlobalSettings {

	override def onStart(app: Application) {

		//    lazy val database = Database.forDataSource(DB.getDataSource())
		//
		//    database withSession {
		//      // Create the tables, including primary and foreign keys
		//      val ddl = (Users.ddl)
		//
		//      val tableList = MTable.getTables.list()
		//      val tableMap = tableList.map {
		//        t => (t.name.name.toUpperCase, t)
		//      }
		//     
		//      Logger.info(tableMap.toString)
		//      Logger.info(tableMap.contains("USER").toString)
		//      
		//      if (!tableMap.contains("USER")) {
		//    	  ddl.create      
		//    	  Users.insertAll(
		//	        User("a.mack@kainos.com", "Angus Mack", "secret"),
		//	        User("guillaume@sample.com", "Guillaume Bort", "secret"),
		//	        User("maxime@sample.com", "Maxime Dantec", "secret"),
		//	        User("sadek@sample.com", "Sadek Drobi", "secret"),
		//	        User("erwan@sample.com", "Erwan Loisant", "secret")
		//    	)
		//      }
		//    }
		//createTablesIfMissing
	}

	private def makeTableMap(session: Session): Map[String, MTable] = {
			val tableList = MTable.getTables.list()(session)
					tableList.map(t => (t.name.name.toUpperCase, t)).toMap
	}

	lazy val tables = List(Users)

			val addUsers: Boolean = false

			def createTablesIfMissing {
		Database.forDataSource(DB.getDataSource()).withSession { session: Session =>
		val tableMap = makeTableMap(session)
		tables.foreach(t => {
			if (!(tableMap.contains(t.tableName.toUpperCase))) {
				//t.ddl.drop(session)
				t.ddl.create(session)
					t.insertAll(
						User("a.mack@kainos.com", "Angus Mack", "secret"),
						User("guillaume@sample.com", "Guillaume Bort", "secret"),
						User("maxime@sample.com", "Maxime Dantec", "secret"),
						User("sadek@sample.com", "Sadek Drobi", "secret"),
						User("erwan@sample.com", "Erwan Loisant", "secret")
						)(session)
			}
		})
		}
	}
}

