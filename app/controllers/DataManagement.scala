package controllers

import play.api.mvc._
import play.api.mvc.BodyParsers.parse.Multipart.PartHandler
import play.api.mvc.BodyParsers.parse.Multipart.handleFilePart
import play.api.mvc.BodyParsers.parse.Multipart.FileInfo
import play.api.mvc.BodyParsers.parse.multipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.BodyParser
import play.api.mvc.MultipartFormData
import play.api.data.Forms._
import play.api.libs.iteratee.Iteratee
import play.api.db.DB
import play.api.Play.current
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.sql.Date
import java.sql.Timestamp
import java.sql.ResultSet
import java.sql.Statement
import java.util.Calendar
import java.util.ArrayList
import play.Play
import org.apache.poi.poifs.filesystem._
import org.apache.poi.poifs.crypt._
import org.apache.poi.ss.util._
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.CellRange
import org.apache.poi.xssf.usermodel._
import scala.collection.JavaConversions._
import org.apache.poi.ss.usermodel.Workbook
import models._
import scala.language.postfixOps
import play.Logger
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.slick.session.Session
import scala.slick.session.Database
import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.meta.MTable
import anorm._

object DataManagement extends Controller with Authentication with DBAccess{
	//I have hard coded here the file path
	val workbookFilePath = "\\conf\\data\\workbooks\\"
	val iLoadFilePath = "\\conf\\data\\iLoads\\"

	def generate_iLoads = TODO
	def generateDGWB = TODO
	def dataValidation = TODO
	
	/**
	 * This function removes the table of imported data and sets the table name for the corresponding workbook
	 * row to blank string
	 */
	def deleteImportedWorkbookData(filename: String) = Action {  implicit request => 
      database withSession {
        implicit session: Session =>
         DBWorkbookDAO.get(filename).map { dgwb =>
          //delete the table with the imported data 
          val dropTableQuery = "DROP TABLE " + dgwb.import_table + "\n"
          Logger.debug(dropTableQuery.toString())
          val dropTable = StaticQuery.updateNA(dropTableQuery.toString).execute
          Logger.debug(dropTable.toString)

           //update workbook entry and remove the table name from its row
          //create modified workbook object
           val updateWB = DBWorkbook(dgwb.filename,dgwb.data,dgwb.file_size,dgwb.wb_type,"",dgwb.uploaded_by,dgwb.uploaded_date)
		   //save it
           DBWorkbookDAO.save(updateWB)	
          Redirect("/datamanagement/workbooklist")
         }.getOrElse(Redirect("/datamanagement/workbooklist/error"))
      }
	}
	
	/**
	 * This function deletes the table of imported data for this workbook
	 * and then deletes the whole entry from the workbooks table as well
	 */
	def deleteImportedWorkbook(filename: String) = Action {  implicit request => 
      database withSession {
        implicit session: Session =>
         DBWorkbookDAO.get(filename).map { dgwb =>
           	  if(dgwb.import_table.length() > 0) {
	           	  //delete the imported data associated with this workbook 
		          val dropTableQuery = "DROP TABLE " + dgwb.import_table + "\n"
		          Logger.debug(dropTableQuery.toString())
		          
		          val dropTable = StaticQuery.updateNA(dropTableQuery.toString).execute
		          Logger.debug(dropTable.toString)
           	  }
	          //delete the entry from the workbooks table
	          DBWorkbookDAO.delete(filename)
          	Redirect("/datamanagement/workbooklist")
         }.getOrElse(Redirect("/datamanagement/workbooklist/error"))
      }
	}
	
	
	def viewTableData(tablename:String) = Action {  implicit request => 
      database withSession {
        implicit session: Session =>
          	//read the column names from this table - it's dynamcially created so it doesn't map to a case class
          //liuke the users table for example
          	val columnNames = StaticQuery.queryNA[(String)]("select column_name from INFORMATION_SCHEMA.COLUMNS where table_name = '" + tablename + "'").list

          	val selectAll = new StringBuilder
          	selectAll ++= "SELECT * FROM " + tablename + "\n"
          	selectAll ++= "OFFSET 0\n"
          	selectAll ++= "FETCH FIRST 25 ROWS ONlY" 
          	
          //get first 25 rows as per statement above from the passed in table name
      	  val statement = session.conn.createStatement()
		  val rs = statement.executeQuery(selectAll.toString)
		  
		  //build a new array list of an array of strings
		  var results:ArrayList[Array[String]] = new ArrayList
	      var count = 0
		  
	      //parse the result set into this list
		  while(rs.hasNext) {
		    val rowData = new Array[String](columnNames.length)
		    for(s <- columnNames.zipWithIndex) {
		    	rowData(s._2) = rs.getString(s._1)
		    	count += 1
		    }
		    results.add(rowData)
		  }
          	
		DB.withConnection { implicit c=>
		  	val selectPage = SQL(selectAll.toString).resultSet
		  	
		  	Logger.debug(selectPage.toString())
		  	//return the view table view with the data and column names for display
		  	Ok(views.html.datamanagement.viewworkbook("Workbook Data",results,columnNames))
		}
      }
	}
	
	//Handling default requests. to load workbook form
	def listWorkbooks = Action {  implicit request => 
      database withSession {
        implicit session: Session =>
			val workbooks = DBWorkbookDAO.listWorkbooks
				Ok(views.html.datamanagement.listworkbooks("Workbook home - List",workbooks))
      }
	}

	//as above
	def listiLoads = Action {  implicit request => 
      database withSession {
        implicit session: Session =>
			val iLoads = DBWorkbookDAO.listiLoads		
				Ok(views.html.datamanagement.listiloads("iLoad home - List",iLoads))
      	}
      }

	
	//show upload wb page
	def uploadWorkbookForm = Action {  implicit request =>
		Ok(views.html.datamanagement.uploadworkbook("Workbook upload"))
	}

	//show upload iload page
	def uploadiLoadForm = Action {  implicit request =>
		Ok(views.html.datamanagement.uploadiload("iLoad upload"))
	}

	/**
	 * Streams content from the form upload using the multipart byte handler below
	 * to save the uploaded Workbook directly into the database
	 */
	def uploadWorkbook = Action(multipartFormDataAsBytes) { request =>
	  Application.getCurrentUser(request).map { user =>
	    val username = user.username
	    val currenttime:Calendar = Calendar.getInstance();
	    val sqldate:Date = new Date((currenttime.getTime()).getTime());
	    val timestamp:Timestamp = new Timestamp(sqldate.getTime())
	      database withSession {
		      implicit session: Session =>
		    	request.body.files foreach { 
		    		case FilePart(key,filename,contentType,bytes) =>
		    		val dbwb = DBWorkbook(filename, bytes, bytes.length, "dgwb", "", username , timestamp)
		    		Logger.debug("Adding new workbook: " + dbwb.toString)
		    		DBWorkbookDAO.save(dbwb)
		    	}
			 }
	      }		    
			
		Redirect(controllers.routes.DataManagement.listWorkbooks).
				flashing("message" -> "Workbook uploaded successfully !!!")
//		}.getOrElse {
//			//send error message
//			Redirect(controllers.routes.DataManagement.uploadWorkbookForm).
//			flashing("errormessage" -> "File Missing")
//		}
	}
	
	/**
	 * Streams content from the form upload using the multipart byte handler below
	 * to save the uploaded iLoad directly into the database
	 */
	def uploadiLoad = Action(multipartFormDataAsBytes) { request =>
	  Application.getCurrentUser(request).map { user =>
	    val username = user.username
	    val currenttime:Calendar = Calendar.getInstance();
	    val sqldate:Date = new Date((currenttime.getTime()).getTime());
	    val timestamp:Timestamp = new Timestamp(sqldate.getTime())
      database withSession {
        implicit session: Session =>
		request.body.files foreach { 
		  case FilePart(key,filename,contentType,bytes) =>
			val dbwb = DBWorkbook(filename,bytes,bytes.length,"iload","",username,timestamp)
		    DBWorkbookDAO.save(dbwb)
		  }
		}
      }		    
		Redirect(controllers.routes.DataManagement.listiLoads).
				flashing("message" -> "iLoad uploaded successfully !!!")
//		}.getOrElse {
//			//send error message
//			Redirect(controllers.routes.DataManagement.uploadiLoadForm).
//			flashing("errormessage" -> "File Missing")
//		}
	}

	/**
	 * Returns a list of files in a given folder of a given type
	 */
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
	
	/**
	 * Parses and stores the uploaded workbook data in the database
	 */
	def importWorkbookData(filepath: String) = Action { implicit request =>
      database withSession {
        implicit session: Session =>
		
		DBWorkbookDAO.get(filepath).map { dgwb =>
			//create a map of table names
			//we use this later to determine whether this data has been imported yet or not
			val tableList = MTable.getTables.list()
				val tableMap = tableList.map {
					t => (t.name.name, t)
				}.toMap
			
			//get the binary data from the database workbook object
			val bArray = dgwb.data
			Logger.debug("Got bytes: " + bArray.length.toString)
			
			//create a new input stream and then create a POI Workbook factory from this
			val bais = new ByteArrayInputStream(bArray)
			val wb = WorkbookFactory.create(bais)
			
			//get the seventh sheet - the one where the data starts
			//TODO - make this automatic on by user input
			val sheet = wb.getSheetAt(7)
			//val sheet = wb.getSheet("Applicant-Personal&Contact")
			
			//create a list of sheet names
			val sheetNames = { for(a<- 0 to wb.getNumberOfSheets()-1) 
			  yield wb.getSheetName(a)
			}

			val tableName = (filepath + " " + sheet.getSheetName()).toLowerCase().replace('&','_').replace(' ','_').replace('.','_').replace('-','_')
			Logger.debug("Created safe table name: " + tableName)

				
			//create some debug HTML for now to show to the user
			val text = new StringBuilder
			text ++= "<h2>Parsing sheet: " + sheet.getSheetName() + " with " 
				+ (sheet.getLastRowNum()-sheet.getFirstRowNum() - 3) + " data rows.</h2>"
			text ++= "<p/>"						
			//text ++= "List of Sheets: " + sheetNames.mkString("<ul><li>", "</li><li>", "</li></ul>") + "\n"
			//text ++= "<h2>Number of data columns: " + sheet.getlas + "</h2>\n"
			
			//if the table doesn't exist already create it
			Logger.debug("About to create table: " + tableName)
			if(!tableMap.contains(tableName)){
				Util.createDatabaseTableFromDGWBSheet(sheet,tableName)
			}
			
			//insert the data from the DGWB
			Logger.debug("About to insert rows")
			Util.insertDataForDGWBSheet(sheet,tableName,4,sheet.getLastRowNum())
			
			//record the name of the table we stored the data it in the database object
			val updateWB = DBWorkbook(dgwb.filename,dgwb.data,dgwb.file_size,dgwb.wb_type,tableName,dgwb.uploaded_by,dgwb.uploaded_date)
			DBWorkbookDAO.save(updateWB)	
	        
			//return some info to the user
			//Ok(views.html.datamanagement.participant(sheetNames,new play.api.templates.Html(text)))
			//return back to list of uploaded workbooks
			Redirect("/datamanagement/workbooklist")
		}.getOrElse(Ok(views.html.datamanagement.participant(null,new play.api.templates.Html(new StringBuilder("<h1>Error</h1>")))))
      }
     }
	
	/**
	 * parses and stores the uploaded iLoad data
	 */
	def importiLoadData(filepath: String) = Action { implicit request =>
      database withSession {
        implicit session: Session =>
		DBWorkbookDAO.get(filepath).map { dgwb =>
			val bArray = dgwb.data

			Logger.debug("Got bytes: " + bArray.length.toString)
			var wb:XSSFWorkbook = null
			var sheet:XSSFSheet = null
//			try {
			  if(isWorkbookPasswordProtected(filepath)) {
					val bais = new ByteArrayInputStream(bArray)
		
			        val fs:POIFSFileSystem = new POIFSFileSystem(bais);
		
					val info:EncryptionInfo = new EncryptionInfo(fs);
					val d: Decryptor = Decryptor.getInstance(info);
					d.verifyPassword("Kainos20!3");
					wb = new XSSFWorkbook(d.getDataStream(fs));
					
					sheet = wb.getSheetAt(0)
			  } else {
	  			val bArray = dgwb.data
	
				Logger.debug("Got bytes: " + bArray.length.toString)
				
				val bais = new ByteArrayInputStream(bArray)
				wb = new XSSFWorkbook(bais)
				sheet = wb.getSheetAt(0)
			  }

			val sheetNames = { for(a <- 0 to wb.getNumberOfSheets()-1) 
			  yield wb.getSheetName(a)
			}
			
			val text = new StringBuilder
			text ++= "<h2>Parsing sheet: " + sheet.getSheetName() + " with " 
				+ (sheet.getLastRowNum()-sheet.getFirstRowNum() - 3) + " data rows.</h2>"
			text ++= "<p/>"						
			
			// fixed table name for now
			val tableName = "dgwb_test_2_xlsx_applicant___address"
			
			val populatedSheet = populateiLoadWithTableData(sheet,tableName)
			
			val baos = new ByteArrayOutputStream
			wb.write(baos)
			baos.close
			Logger.debug("About to return " + baos.size.toString + " bytes to user.")
			//Ok(views.html.datamanagement.participant(sheetNames,new play.api.templates.Html(text)))
			Ok(baos.toByteArray).as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

			//			}
//			catch {
//			  case e: Exception => 
//			    Ok(views.html.datamanagement.participant(null,new play.api.templates.Html(new StringBuilder(e.getMessage() + e.toString()))))
//			}
		}.getOrElse(Ok(views.html.datamanagement.participant(null,new play.api.templates.Html(new StringBuilder("<h1>Error!</h1>")))))
      }
     }

	/**
	 * Treat a java.sql.ResultSet as an Iterator, allowing operations like filter,
	 * map, etc.
	 *
	 * Sample usage:
	 * val resultSet = statement.executeQuery("...")
	 * resultSet.map {
	 *   resultSet =>
	 *   // ...
	 * }
	 */
	implicit class ResultSetIterator(resultSet: ResultSet)
		extends Iterator[ResultSet] {
		def hasNext: Boolean = resultSet.next()
		def next() = resultSet
	}

	/**
	 * Helper function to set a cell value 
	 */
	
	def safeSetCellValue(row: org.apache.poi.ss.usermodel.Row, index: Int, value: String) = {
		//get the cell
	     var cc = row.getCell(index)
	    //create if not exist
	     if(cc==null) {
	       cc = row.createCell(index)
	     }
	     
		 cc.setCellValue(value)	  
	}
	
	/**
	 * Start of the main functionality of this project
	 * Loops through the data we have saved from DGWB into our table 
	 */
	def populateiLoadWithTableData(sheet: Sheet, tableName: String):Sheet = {
      database withSession {
        implicit session: Session =>
      	  Logger.debug("Importing sheet: " + sheet + " from table: " + tableName)

      	  val columnNames = StaticQuery.queryNA[(String)]("select column_name from INFORMATION_SCHEMA.COLUMNS where table_name = '" + tableName + "'").list
      	  Logger.debug("Column Names: " + columnNames.toString)
      	  
      	  //fetch the data from the table
      	  val statement = session.conn.createStatement()

      	  val selectDataQuery = new StringBuilder
	      selectDataQuery ++= "SELECT * FROM " + tableName + "\n"
	      selectDataQuery ++= "OFFSET 0\n"
	      selectDataQuery ++= "FETCH FIRST 20 ROWS ONlY" 
      	  
		  val rs = statement.executeQuery(selectDataQuery.toString)
		  
		  // zero based offset for row index
		  val startRow = 11
		  // a counter for row number 
		  var count = 0
		  
		  //loop through the result set of this data
		  while (rs.hasNext) {
			 //get the row in the iLoad
		    //check there is row data in there 
		    var row = sheet.getRow(startRow+count)
		     if(row==null) {
		       //if not create a row
		       row = sheet.createRow(startRow+count)
		     }

		     //set the iLoad row number in column index 1
		     safeSetCellValue(row, 1, (count+1).toString)

		     //set employee id in column index 4
		     safeSetCellValue(row, 4, rs.getString("employee_id"))
		     
		     count += 1
		  }
		  
		  return sheet
      	}
      }
	
//	def streamFromResultSet[T](rs:ResultSet)(func: ResultSet => T):Stream[T] = {
//	   if (rs.next())
//	      func(rs) #:: streamFromResultSet(rs)(func)
//	   else
//	      rs.close()
//	      Stream.empty
//	}
//	
//	def fillMap(statement:java.sql.Statement,selectStatement: String):Map[String,Set[String]] = {
//	   case class CategoryValue(category:String, property:String)
//	
//	   val resultSet = statement.executeQuery(selectStatement)
//	
//	   val queryResult = streamFromResultSet(resultSet){rs =>
//	      CategoryValue(rs.getString(1),rs.getString(2))
//	   }
//	
//	   queryResult.groupBy(_.category).mapValues(_.map(_.property).toSet)
//	}
	
	/**
	 * Tests whether a workbook is password protected by trying to create a POIFSFileSystem
	 * If an exception is raised it isn't password protected,
	 * otherwise it is
	 */
	def isWorkbookPasswordProtected(filepath:String):Boolean = {
      database withSession {
        implicit session: Session =>
		DBWorkbookDAO.get(filepath).map { dgwb =>
			val bArray = dgwb.data

			try {
				val bais = new ByteArrayInputStream(bArray)
	
		        val fs:POIFSFileSystem = new POIFSFileSystem(bais);
				
				return true
			}
			catch {
			  case oxfe: OfficeXmlFileException =>  
			    return false
			}
		}.getOrElse(return false)
      }
	} 
	
	/**
	 * Default index action for data management
	 */
	def index = Action { implicit request =>
		Ok(views.html.datamanagement.index("Data Management for Account ###"))    
	}
	
	/**
	 * The Multipart object of the BodyParsers does a lot of work for us. 
	 * This is the handler for the FilePart. 
	 * We want the file parts an Array[Byte].
	 */
	def handleFilePartAsByteArray: PartHandler[FilePart[Array[Byte]]] =
		handleFilePart {
			case FileInfo(partName, filename, contentType) =>
			// simply write the data to the a ByteArrayOutputStream
			Iteratee.fold[Array[Byte], ByteArrayOutputStream](
					new ByteArrayOutputStream()) { (os, data) =>
					os.write(data)
					os
			}.mapDone { os =>
				os.close()
				os.toByteArray
			}
		}
	/**
	 * This defines our body parser:
	 */
	def multipartFormDataAsBytes:BodyParser[MultipartFormData[Array[Byte]]] = 
			multipartFormData(handleFilePartAsByteArray)
}