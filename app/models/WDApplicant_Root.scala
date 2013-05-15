package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import java.util.Date
import play.api.Play.current

//Applicant
case class WDApplicant_Root(
	id: Pk[Long],
	employee_id: String,
	entered_date: Date
)

object WDApplicant_Root {
  	val applicant = {
			get[Pk[Long]]("id") ~ 
			get[String]("employee_id") ~
			get[Date]("entered_date") map {
			case id~employee_id~entered_date => WDApplicant_Root(id, employee_id, entered_date)
			}
	}
  	
  	def create(id: String, date: Date) {
		DB.withConnection { implicit c =>
			SQL("insert into applicant (employee_id,entered_date) values ({id},{date})").on(
			    'employee_id -> id
			    ).executeUpdate()
		}
	}
}
