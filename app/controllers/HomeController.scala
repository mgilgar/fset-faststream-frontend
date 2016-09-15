/*
 * Copyright 2016 HM Revenue & Customs
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

import _root_.forms.WithdrawApplicationForm
import config.CSRHttp
import connectors.ApplicationClient.{ CannotWithdraw, OnlineTestNotFound }
import connectors.exchange.{FrameworkId, WithdrawApplicationRequest }
import connectors.ApplicationClient
import helpers.NotificationType._
import models.ApplicationData.ApplicationStatus
import models.page.DashboardPage
import models.{ CachedData, CachedDataWithApp }
import security.Roles
import security.Roles._

import scala.concurrent.Future

object HomeController extends HomeController(ApplicationClient) {
  val http = CSRHttp
}

abstract class HomeController(applicationClient: ApplicationClient) extends BaseController(applicationClient) {
  val Withdrawer = "Candidate"

  val present = CSRSecureAction(ActiveUserRole) { implicit request => implicit cachedData =>
    val dashboard = for {
      phase1TestProfile <- applicationClient.getPhase1TestProfile(cachedData.user.userID)
      allocationDetails <- applicationClient.getAllocationDetails(cachedData.application.get.applicationId)
      // TODO Work out a better way to invalidate the cache across the site
      app = CachedDataWithApp(cachedData.user, cachedData.application.get)
      updatedData <- refreshCachedUser()(app, hc, request)
    } yield {
        val dashboardPage = DashboardPage(updatedData, allocationDetails, Some(phase1TestProfile))
        Ok(views.html.home.dashboard(updatedData, dashboardPage, Some(phase1TestProfile), allocationDetails))
    }

    dashboard recover {
      case e: OnlineTestNotFound =>
        val applicationSubmitted = !cachedData.application.forall { app =>
          app.applicationStatus == ApplicationStatus.CREATED || app.applicationStatus == ApplicationStatus.IN_PROGRESS
        }
        val isDashboardEnabled = faststreamConfig.applicationsSubmitEnabled || applicationSubmitted

        if (isDashboardEnabled) {
          val dashboardPage = DashboardPage(cachedData, None, None)
          Ok(views.html.home.dashboard(cachedData, dashboardPage, None, None))
        } else {
          Ok(views.html.home.submit_disabled(cachedData))
        }
    }
  }

  val resume = CSRSecureAppAction(ActiveUserRole) { implicit request =>
    implicit user =>
      Future.successful(Redirect(Roles.userJourneySequence.find(_._1.isAuthorized(user)).map(_._2).getOrElse(routes.HomeController.present())))
  }

  val create = CSRSecureAction(ApplicationStartRole) { implicit request =>
    implicit user =>
      for {
        response <- applicationClient.createApplication(user.user.userID, FrameworkId)
        _ <- env.userService.save(user.copy(application = Some(response)))
        if faststreamConfig.applicationsSubmitEnabled
      } yield {
        Redirect(routes.PersonalDetailsController.presentAndContinue())
      }
  }

  val presentWithDraw = CSRSecureAppAction(WithdrawApplicationRole) { implicit request =>
    implicit user =>
      Future.successful(
        Ok(views.html.application.withdraw(WithdrawApplicationForm.form))
      )
  }

  val withdraw = CSRSecureAppAction(WithdrawApplicationRole) { implicit request =>
    implicit user =>

      def updateStatus(data: CachedData) = data.copy(application =
        data.application.map(_.copy(applicationStatus = ApplicationStatus.WITHDRAWN)))

      WithdrawApplicationForm.form.bindFromRequest.fold(
        invalidForm => Future.successful(Ok(views.html.application.withdraw(invalidForm))),
        data => {
          applicationClient.withdrawApplication(user.application.applicationId, WithdrawApplicationRequest(data.reason, data.otherReason,
            Withdrawer)).flatMap { _ =>
            updateProgress(updateStatus)(_ => Redirect(routes.HomeController.present()).flashing(success("application.withdrawn")))
          }.recover {
            case _: CannotWithdraw => Redirect(routes.HomeController.present()).flashing(danger("error.cannot.withdraw"))
          }
        }
      )
  }

  def confirmAlloc = CSRSecureAction(UnconfirmedAllocatedCandidateRole) { implicit request =>
    implicit user =>

      applicationClient.confirmAllocation(user.application.get.applicationId).map { _ =>
        Redirect(controllers.routes.HomeController.present).flashing(success("success.allocation.confirmed"))
      }
  }
}
