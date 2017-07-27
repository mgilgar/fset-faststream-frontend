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

import config.CSRCache
import connectors.ApplicationClient
import connectors.ApplicationClient.CannotSubmit
import connectors.exchange.referencedata.SchemeId
import forms.SchemeSpecificQuestionsForm
import helpers.NotificationType._
import models.ApplicationData.ApplicationStatus.SUBMITTED
import models.ApplicationRoute.{ ApplicationRoute, Faststream }
import org.joda.time.DateTime
import security.Roles.{ AbleToWithdrawApplicationRole, SchemeSpecificQuestionsRole, SubmitApplicationRole }

import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import security.SilhouetteComponent

object SchemeSpecificQuestionsController extends SchemeSpecificQuestionsController(ApplicationClient, CSRCache) {
  val appRouteConfigMap = config.FrontendAppConfig.applicationRoutesFrontend
  lazy val silhouette = SilhouetteComponent.silhouette
}

abstract class SchemeSpecificQuestionsController(applicationClient: ApplicationClient, cacheClient: CSRCache)
  extends BaseController(applicationClient, cacheClient) with CampaignAwareController {

  def presentSchemeForm(schemeId: SchemeId) = CSRSecureAppAction(SchemeSpecificQuestionsRole) { implicit request =>
    implicit user =>
      if (canAnswersBeModified()) {
        Future.successful(Ok(views.html.application.additionalquestions.schemespecific(SchemeSpecificQuestionsForm.form)))
      } else {
        Future.successful(Redirect(routes.HomeController.present()))
      }
  }

  def saveSchemeForm(schemeId: SchemeId) = CSRSecureAppAction(SchemeSpecificQuestionsRole) { implicit request =>
    implicit user =>
      if (canApplicationBeSubmitted(user.application.overriddenSubmissionDeadline)(user.application.applicationRoute)) {
        applicationClient.submitApplication(user.user.userID, user.application.applicationId).flatMap { _ =>
          updateProgress(data =>
            data.copy(application = data.application.map(_.copy(applicationStatus = SUBMITTED))))(_ =>
            Redirect(routes.SubmitApplicationController.success()))
        }.recover {
          case _: CannotSubmit => Redirect(routes.PreviewApplicationController.present()).flashing(danger("error.cannot.submit"))
        }
      } else {
        Future.successful(Redirect(routes.HomeController.present()))
      }
  }

  def presentSummary = CSRSecureAppAction(SchemeSpecificQuestionsRole) { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.application.success()))
  }

  def submit = CSRSecureAppAction(SchemeSpecificQuestionsRole) { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.application.success()))
  }


  private def canAnswersBeModified(): Boolean = {
    true
  }
}
