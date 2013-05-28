package models

import org.mindrot.jbcrypt._
import annotation.tailrec
import java.sql.Timestamp
import java.util.Date
import java.text.SimpleDateFormat
import util.Random
import org.apache.poi.ss.util._
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.CellRange
import play.Logger
import play.api.db.DB
import play.api.Play.current
import scala.slick.session.Session
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.slick.session.Database
import scala.slick.jdbc.StaticQuery

object Util {
	lazy val database = scala.slick.session.Database.forDataSource(DB.getDataSource())
  
	def encryptPassword(password:String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt())
  }


  def checkPassword(passwordToCheck:String, password:String):Boolean = {
    BCrypt.checkpw(passwordToCheck, password)
  }

  def generatePassword(): String = {
    uniqueRandomKey(10)
  }

  lazy val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ ("-!£$")

  def uniqueRandomKey(length: Int) : String =
  {

    val newKey = (1 to length).map(
      x =>
      {
        val index = Random.nextInt(chars.length)
        chars(index)
      }
    ).mkString("")

    newKey

  }

  def slugify(str: String): String = {
    import java.text.Normalizer
    Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\w ]", "").replace(" ", "-").toLowerCase
  }

  @tailrec
  def generateUniqueSlug(slug: String, existingSlugs: Seq[String]): String = {
    if (!(existingSlugs contains slug)) {
      slug
    } else {
      val EndsWithNumber = "(.+-)([0-9]+)$".r
      slug match {
        case EndsWithNumber(s, n) => generateUniqueSlug(s + (n.toInt + 1), existingSlugs)
        case s => generateUniqueSlug(s + "-2", existingSlugs)
      }
    }
  }

  def formatDate(time:Timestamp):String = {
    val dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm")
    dateFormat.format(time)
  }
  	def insertDataForDGWBSheet(sheet:Sheet,tableName:String,startRow:Int, endRow:Int) = { 
		for(rr <- startRow to endRow) {
		  val dataRow = sheet.getRow(rr)
		  val firstCol = dataRow.getFirstCellNum().toInt
		  val lastCol = dataRow.getLastCellNum().toInt 
		  val insertCommand = new StringBuilder
		  insertCommand ++= "insert into " + tableName + " values("
		  for(cc <- firstCol to lastCol-1) {
			  if(dataRow.getCell(cc)!=null){
				  insertCommand ++= "'" + getCellString(dataRow.getCell(cc)).toString.replaceAll("\'", "\'\'") + "'"
			  } else {
			    insertCommand ++= "NULL" 
			  }
			  if(cc<lastCol-1)
			    insertCommand ++= ","
		  }
		  insertCommand ++= ")"
		
		  Logger.debug(insertCommand.toString)		  
		  database withSession {
			implicit session: Session =>		  
		  	StaticQuery.updateNA(insertCommand.toString).execute
		  }
		}
	}
	
	def createDatabaseTableFromDGWBSheet(sheet: Sheet, tableName: String) = {
        Logger.debug("Entered create table function")
		
		//find out how many rows of data we have
		val rowStart = sheet.getFirstRowNum()
		val rowEnd = sheet.getLastRowNum()
        
		//create some response text to show the user
		val tableCreateSQL = new StringBuilder
				
		//get the data areas row
		val areaTypesRow = sheet.getRow(1)
		//get the data types row
		val dataTypesRow = sheet.getRow(2)
		//get the header row
		val headerNamesRow = sheet.getRow(3)
		
		//find out how many columns we have
		val firstCol = dataTypesRow.getFirstCellNum().toInt
		val lastCol = dataTypesRow.getLastCellNum().toInt

		//store merged cells in row index 1 i.e what is called row 2 in Excel
		val mergedRegions = { for (a <- 0 to sheet.getNumMergedRegions()-1) 
		  yield sheet.getMergedRegion(a)
		 }
		
		Logger.debug(mergedRegions.toString)
		
		//loop through columns and get data types
		val HeaderDataTypes = {	for (a <- firstCol to lastCol-1 ) 
		  yield if(dataTypesRow.getCell(a)!=null) {
		    val test = getCellString(dataTypesRow.getCell(a)).toString
			  if(test.equals("Text")) {
			    "varchar(255)"
			  } else if (test.equals("MM/DD/YYYY")) {
			    "date"
			  }	else if (test.equals("Y/N")) {
			    "boolean"
			  } else {
			    "varchar(255)"
			  }		    
		  }
		    else { "###"
		    }
		}  

		//loop through columns and get header names
		val HeaderNames = new Array[String](lastCol)
		for (a <- firstCol to lastCol-1 ) {
			val currentCell = headerNamesRow.getCell(a)
			if(currentCell!=null) {
			  //get the header name cell
			  val headerCell = headerNamesRow.getCell(a)
			  //convert to lower case and replace space with _ to make valid column name
			  val check = getCellString(headerNamesRow.getCell(a)).toString.toLowerCase().replace(' ', '_').replace('(','_').replace(')','_').replace('#','_')

			  var prefix =""
			  for(region <- mergedRegions) {
			    if(region.isInRange(1, currentCell.getColumnIndex())) {
			    	val startCol = region.getFirstColumn()
			    	prefix = sheet.getRow(1).getCell(startCol).getStringCellValue().trim().toLowerCase().replace(' ','_') + "_"
			    }
			  }
			    Logger.debug("Region name: " + prefix)
			    		
			    HeaderNames(a) = prefix + check
			  }
			  else { 
			    HeaderNames(a) = "YYY"
			  }
		}
		tableCreateSQL ++= "CREATE TABLE " + tableName + "(\n"
		for(i <- 0 to HeaderNames.length-1) {
		  tableCreateSQL ++= HeaderNames(i) + " " + HeaderDataTypes(i) 
		  if(i<HeaderNames.length-1) {
		    tableCreateSQL ++= ","
		  }
		  tableCreateSQL ++= "\n"
		}
		tableCreateSQL ++= ")\n"
		
		Logger.debug(tableCreateSQL.toString)
		
		database withSession {
			implicit session: Session =>		  
			val createTable = StaticQuery.updateNA(tableCreateSQL.toString).execute
		}
	}
	
	def getCellString(cell: Cell) = {
          cell.getCellType() match {
            case Cell.CELL_TYPE_NUMERIC => 
              if(DateUtil.isCellDateFormatted(cell)) {
                val dateVal = cell.getDateCellValue()
                val format = new java.text.SimpleDateFormat("dd/MM/yyy")
                format.format(dateVal)
              }
              else {
                cell.getNumericCellValue()
              }
            case Cell.CELL_TYPE_STRING => 
              cell.getStringCellValue()
            case Cell.CELL_TYPE_FORMULA =>
              //val evaluator = wb.getCreationHelper().createFormulaEvaluator()
              //(new DataFormatter()).formatCellValue(cell, evaluator)
              " "
            case _ => " "
          }
	}
}