package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._

object Client extends Controller {
    def index = Action { 
    	Ok(views.html.clients.index("General Client Info"))    
  }

   def manage = Action { 
    	Ok(views.html.clients.management("Client Management Tasks/Info"))    
  }

    
    def add = Action { 
    	Ok(views.html.clients.add("Request New Client"))    
  }
}
