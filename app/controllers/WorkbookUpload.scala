package controllers

import play.api.mvc._
import play.api.data.Forms._
import java.io.FileReader
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.BufferedInputStream
import java.io.File
import play.Play
import java.io.ByteArrayInputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import scala.collection.JavaConversions._
import org.apache.poi.ss.usermodel.Workbook
import models._
//removes feature warning on != operator here ***  
import scala.language.postfixOps

object WorkbookUpload extends Controller with Secured {
	//I have hard coded here the file path
	val filePath = "\\conf\\data\\"

	def generate_iLoads = TODO
	def generateDGWB = TODO
	def dataValidation = TODO
	def index = TODO
	  
	//Handling default requests. to load workbook form
	def list = Action {  implicit request => 
		val srcDir = new File( Play.application.path + filePath )
		val htmlFiles = findFiles( _.getName endsWith ".xlsx" )(srcDir)
		
		Ok(views.html.workbookupload.list("Workbook home - List",htmlFiles))
	}
	
	def uploadForm = Action {  implicit request =>
	Ok(views.html.workbookupload.upload("Workbook upload"))
	}

	//
	def upload = Action(parse.multipartFormData) { request =>
	request.body.file("fileupload").map { file =>
	val filename = file.filename
	val contentType = file.contentType

	//moving xls file to application folder
	file.ref.moveTo(new File(Play.application.path + 
			filePath + filename), true)
			
			//import data
			//importData(file.toString)
			
			//send message
			Redirect(controllers.routes.WorkbookUpload.index).
			flashing("message" -> "Workbook uploaded successfully !!!")
	}.getOrElse {
		//send error message
		Redirect(controllers.routes.WorkbookUpload.uploadForm).
		flashing("errormessage" -> "File Missing")
	}
	}
	
	def findFiles(fileFilter: (File) => Boolean = (f) => true)(f: File): List[File] = {
		val ss = f.list()
				val list = if (ss == null) {
					Nil
				} else {
					ss.toList.sorted
				}
		val visible = list.filter(_.charAt(0) != '.')
				val these = visible.map(new File(f, _))
				these.filter(fileFilter) ++ these.filter(_.isDirectory).flatMap(findFiles(fileFilter))
	}
	
	def importData(filepath: String) = Action { implicit request =>
		val bis = new BufferedInputStream(new FileInputStream(filepath))
		//*** see command at import section
		val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray

		val wb = WorkbookFactory.create(new ByteArrayInputStream(bArray))
		val sheet = wb.getSheetAt(4)
		
		//find out how many rows of data we have
		val rowStart = sheet.getFirstRowNum()
		val rowEnd = sheet.getLastRowNum()
        
		//create some response text to show the user
		val text = new StringBuilder
		text ++= "<h2>Parsing sheet: " + sheet.getSheetName() + " with " + (rowEnd - 3) + " data rows.</h2>"
		text ++= "<p/><table>"						
		
		  //loop through from row 5 to the end to get each applicant
		for (rowNum <- 5 to rowEnd ) {
			text ++= "<tr>"
			val r = sheet.getRow(rowNum)
			
			//get start and end column indices
			val firstCol = r.getFirstCellNum().toInt
			val lastCol = r.getLastCellNum().toInt
			
			//get the application id and entered date from the sheet
			val appID = getCellString(r.getCell(0)).toString
			val format = new java.text.SimpleDateFormat("dd/MM/yyy")
			val enteredDate = format.parse(getCellString(r.getCell(1)).toString)
			
			for(i <- 0 to 7) {
				val cc = r.getCell(i)
				if(cc==null) {
					text ++= "<td width=\"30\">-</td>"
				}
				else {
					text ++= "<td width=\"30\">" + getCellString(cc).toString + "</td>"
				}
			}
			text ++= "</tr>"
			//insert this into the database
			//WDApplicant_Root.create(appID,enteredDate)
		}
        text ++= "</table>"
        Ok(views.html.workbookupload.participant(new play.api.templates.Html(text)))
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
