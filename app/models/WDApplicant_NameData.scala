package models

import anorm._
import anorm.SqlParser._
import java.util.Date

//Applicant
case class WDApplicant_NameData(
	applicant_id: Long,
	country_iso_code: String,
	legal_first_name: String,
	legal_middle_name: String,
	legal_last_name: String,
	legal_secondary_name: String,
	preferred_first_name: String,
	preferred_last_name: String,
	preferred_secondary_name: String,
	local_script: String,
	local_script_first_name: String,
	local_script_middle_name: String,
	local_script_last_name: String,
	local_script_secondary_name: String,
	prefix_type: String,
	prefix: String,
	suffix_type: String,
	suffix: String
)

object WDApplicant_NameData {
  	val applicant_namedata = {
		get[Long]("applicant_id") ~ 
		get[String]("country_iso_code") ~
		get[String]("legal_first_name") ~
		get[String]("legal_middle_name") ~
		get[String]("legal_last_name") ~
		get[String]("legal_secondary_name") ~
		get[String]("preferred_first_name") ~
		get[String]("preferred_last_name") ~
		get[String]("preferred_secondary_name") ~
		get[String]("local_script") ~
		get[String]("local_script_first_name") ~
		get[String]("local_script_middle_name") ~
		get[String]("local_script_last_name") ~
		get[String]("local_script_secondary_name") ~
		get[String]("prefix_type") ~
		get[String]("prefix") ~
		get[String]("suffix_type") ~
		get[String]("suffix") map {
		case applicant_id~country_iso_code~legal_first_name~legal_middle_name~legal_last_name~legal_secondary_name~preferred_first_name~preferred_last_name~preferred_secondary_name~local_script~local_script_first_name~local_script_middle_name~local_script_last_name~local_script_secondary_name~prefix_type~prefix~suffix_type~suffix => 
		  WDApplicant_NameData(applicant_id,country_iso_code, legal_first_name, legal_middle_name, legal_last_name, legal_secondary_name, preferred_first_name, preferred_last_name, preferred_secondary_name, local_script, local_script_first_name, local_script_middle_name, local_script_last_name, local_script_secondary_name, prefix_type, prefix, suffix_type, suffix)
		}
	}
}
