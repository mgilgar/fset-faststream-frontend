/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.CSRHttp
import connectors.ApplicationClient
import play.api.mvc.Action
import security.SilhouetteComponent

import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

/**
 * Provide all the peripheral links from this controller, like T&C link
 */
object ApplicationController extends ApplicationController(ApplicationClient) {
  val http = CSRHttp
  lazy val silhouette = SilhouetteComponent.silhouette
}

abstract class ApplicationController(applicationClient: ApplicationClient)
  extends BaseController {

  def index = Action {
    Redirect(routes.SignInController.signIn())
  }

  def terms = CSRUserAwareAction { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.index.terms()))
  }

  def privacy = CSRUserAwareAction { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.index.privacy()))
  }

  def helpdesk = CSRUserAwareAction { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.index.helpdesk()))
  }

}
