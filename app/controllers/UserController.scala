package controllers

import play.api._
import libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.http._
import models.{UserDAO, User, Util}
import com.typesafe.config._
import play.api.Play.current
import play.api.db.DB
import scala.slick.session.Session
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object UserController extends Controller with Authentication with Logging with DBAccess {
	def compfn1(e1: User, e2: User) = (e1.username compareToIgnoreCase e2.username) < 0
  
	def list = Authenticated {
    implicit request =>
      database withSession {
        implicit session: Session =>
        val promise = Future {

          (request.user.isAdmin, UserDAO.list.sortWith(compfn1))
        }

        Async {
          promise.map {
            result => if (result._1) Ok(views.html.userlist(result._2)) else Redirect("/")
          }
        }
      }
  }


  def delete(username: String) = Authenticated {
    implicit request =>
      database withSession {
        implicit session: Session =>

            val promise = Future {
              if(request.user.isAdmin){
                UserDAO.delete(username)
                true
              }else
                false
            }

          Async {
            promise.map { result => if(result) Redirect("/admin/users").flashing("success" -> "Deleted Successfully") else Unauthorized }
          }
      }
  }


  def makeAdmin(username: String) = Authenticated {
    implicit request =>
      database withSession {
        implicit session: Session =>

        val promise = Future {
          if(request.user.isAdmin){
            UserDAO.makeAdmin(username)
            true
          }else
          false
        }

      Async {
        promise.map { result => if(result) Redirect("/useraccounts").flashing("success" -> "Admin Rights Updated") else Unauthorized }
      }
      }
  }

}

