package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._

object SAT extends Controller {
    def index = Action { implicit request =>
    	Ok(views.html.sat("Suite, Area or Task maintenance"))    
  }
    def addsuite = Action { implicit request =>
    	Ok(views.html.suites.index("Suites"))    
  }

    def addarea = TODO
    def addtask = TODO
}
