package models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.TypeMapper._
import scala.slick.session.Session
import java.sql.Date

case class ApplicantPersonalAndContact(employee_id: String, applicant_entered_date: Date, applicant_source_name: String, hire_source_category_name: String, country_iso_code: String, legal_first_name: String, legal_middle_name: String, legal_last_name: String, legal_secondary_name: String, preferred_first_name: String, preferred_last_name: String, preferred_secondary_name: String, local_script: String, local_script_first_name: String, local_script_middle_name: String, local_script_last_name: String, local_script_secondary_name: String, prefix_type: String, prefix: String, suffix_type: String, suffix: String)

object ApplicantPersonalAndContactDAO extends Table[ApplicantPersonalAndContact]("applicant_personal_and_contact") {

  def employee_id = column[String]("user_name", O.PrimaryKey, O.DBType("varchar(50)"))
  def applicant_entered_date = column[Date]("applicant_entered_date")
  def applicant_source_name = column[String]("applicant_source_name")
  def hire_source_category_name = column[String]("hire_source_category_name")
  def country_iso_code = column[String]("country_iso_code")
  def legal_first_name = column[String]("legal_first_name")
  def legal_middle_name = column[String]("legal_middle_name") 
  def legal_last_name = column[String]("legal_last_name")
  def legal_secondary_name = column[String]("legal_secondary_name")
  def preferred_first_name = column[String]("preferred_first_name")
  def preferred_last_name = column[String]("preferred_last_name")
  def preferred_secondary_name = column[String]("preferred_secondary_name") 
  def local_script = column[String]("local_script")
  def local_script_first_name = column[String]("local_script_first_name") 
  def local_script_middle_name = column[String]("local_script_middle_name") 
  def local_script_last_name = column[String]("local_script_last_name")
  def local_script_secondary_name = column[String]("local_script_secondary_name")
  def prefix_type = column[String]("prefix_type")
  def prefix = column[String]("prefix")
  def suffix_type = column[String]("suffix_type") 
  def suffix = column[String]("suffix")

  def * = employee_id ~ applicant_entered_date ~ applicant_source_name ~ hire_source_category_name ~ country_iso_code ~ legal_first_name ~ legal_middle_name ~ legal_last_name ~ legal_secondary_name ~ preferred_first_name ~ preferred_last_name ~ preferred_secondary_name ~ local_script ~ local_script_first_name ~ local_script_middle_name ~ local_script_last_name ~ local_script_secondary_name ~ prefix_type ~ prefix ~ suffix_type ~ suffix <> (ApplicantPersonalAndContact, ApplicantPersonalAndContact.unapply _)
  
  
}