package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._

object Suite extends Controller {
    def index = Action { 
    	Ok(views.html.suites.index("Suites List"))    
  }
    def add = Action { 
    	Ok(views.html.suites.add("New Suite"))    
  }
}