package controllers

import play.api._
import libs.json.Json
import play.api.mvc._
import models._
import play.api.Play.current
import scala.concurrent.Future
import scala.slick.session.Session
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.typesafe.config.ConfigFactory

object ClientController extends Controller with Authentication with DBAccess {
    
  def index = Authenticated { 
    implicit request =>
      database withSession {
        implicit session: Session =>
          val promise = Future {
            (request.user.isAdmin, ClientDAO.list)
          }
          
          Async {
            promise.map {
            	result => if(result._1) Ok(views.html.clients.index(result._2,"Client List")) else Redirect("/") 
            }
          }
       }
   }

   def manage = Authenticated { implicit request =>
    	Ok(views.html.clients.management("Client Management Tasks/Info"))    
  }

    
    def add = Authenticated { implicit request =>
    	Ok(views.html.clients.add("Request New Client"))    
  }
}
