package models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.TypeMapper._
import scala.slick.session.Session
import java.sql.Timestamp
import org.apache.poi.ss.usermodel.Workbook
import play.Logger

case class DBWorkbook(filename: String, data: Array[Byte], file_size: Int, wb_type: String, import_table: String, uploaded_by: String, uploaded_date: Timestamp)

object DBWorkbookDAO extends Table[DBWorkbook]("uploaded_workbooks") {

  // -- Parsers
  def filename = column[String]("file_name",O.PrimaryKey)
  def data = column[Array[Byte]]("file_data")
  def file_size = column[Int]("file_size")
  def wb_type = column[String]("workbook_type")
  def import_table = column[String]("import_table")
  def uploaded_by = column[String]("uploaded_by")
  def uploaded_date = column[Timestamp]("uploaded_date")

  def * = filename ~ data ~ file_size ~ wb_type ~ import_table ~ uploaded_by ~ uploaded_date <> (DBWorkbook, DBWorkbook.unapply _)
  // -- Queries

  def get(filename:String)(implicit session:Session):Option[DBWorkbook] = {
    val q = for (c <- DBWorkbookDAO if c.filename === filename) yield c
    
    q.list().headOption
  }

  def listiLoads(implicit session:Session):Seq[DBWorkbook] = {
    val q = for (c <- DBWorkbookDAO if c.wb_type === "iload") yield c
    
    q.list
  }
  
  def listWorkbooks(implicit session:Session):Seq[DBWorkbook] = {
    val q = for (c <- DBWorkbookDAO if c.wb_type === "dgwb") yield c
    
    q.list
  }

  def list(implicit session:Session):Seq[DBWorkbook] = {
    val q = for(u <- DBWorkbookDAO) yield u
    q.list
  }
  
  def updateTableName(wb:DBWorkbook,tableName:String)(implicit session:Session) {
	  //val q = for(c <- DBWorkbookDAO if c.filename === wb.filename) yield c.tableName
	  //q.update(tableName)
    DBWorkbookDAO.filter(a => a.filename === wb.filename).map(a => a.tableName).update(tableName)
  }

  def delete(filename:String)(implicit session:Session) = {
    val q = for(u <- DBWorkbookDAO if u.filename=== filename) yield u
    q.delete
  }

  
   def save(wb:DBWorkbook)(implicit session:Session) {
    if(get(wb.filename) == None){
      val wbToSave = wb.copy()
      this.insert(wbToSave)
    }else{
      val q = for(c <- DBWorkbookDAO if c.filename === wb.filename) yield c
      Logger.debug(q.toString())
      q.update(wb)
    }
  }   
}
