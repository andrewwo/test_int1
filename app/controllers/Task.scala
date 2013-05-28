package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._

object Task extends Controller {
    def index = Action { implicit request =>
    	Ok(views.html.tasks.index("Tasks List"))    
  }
    def add = Action { implicit request =>
    	Ok(views.html.tasks.add("New Task"))    
  }
}