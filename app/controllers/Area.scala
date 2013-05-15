package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._

object Area extends Controller {
    def index = Action { 
    	Ok(views.html.areas.index("Areas List"))    
  }
    def add = Action { 
    	Ok(views.html.areas.add("New Area"))    
  }
}