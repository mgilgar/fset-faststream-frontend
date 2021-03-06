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

package models.page

import java.util.UUID

import connectors.ReferenceDataExamples._
import connectors.exchange.{ EventsExamples, SchemeEvaluationResult }
import connectors.exchange.candidateevents.CandidateAllocationWithEvent
import connectors.exchange.referencedata.SchemeId
import connectors.exchange.sift.SiftAnswersStatus
import controllers.UnitSpec
import helpers.{ CachedUserWithSchemeData, CurrentSchemeStatus }
import models.ApplicationData.ApplicationStatus
import models._
import models.events.AllocationStatuses

class PostOnlineTestsPageSpec extends UnitSpec {

  "PostOnlineTestsPage" should {

    def randUUID = UniqueIdentifier(UUID.randomUUID().toString)

    val userDataWithApp = CachedDataWithApp(
      CachedUser(randUUID, "firstname", "lastname", Some("prefName"), "email@email.com", isActive = true, "unlocked"),
      ApplicationData(randUUID, randUUID,
        ApplicationStatus.PHASE3_TESTS_PASSED_NOTIFIED, ApplicationRoute.Faststream, ProgressResponseExamples.siftEntered,
        civilServiceExperienceDetails = None, edipCompleted = None, overriddenSubmissionDeadline = None
      )
    )

    "be correctly built candidates after phase 3" in {
      val phase3Results = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Red.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Red.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Green.toString) ::
      Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application, Schemes.AllSchemes, phase3Results)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.userDataWithSchemes.successfulSchemes mustBe CurrentSchemeStatus(Schemes.HR, SchemeStatus.Green, failedAtStage = None) :: Nil

      page.userDataWithSchemes.failedSchemes mustBe  CurrentSchemeStatus(Schemes.Commercial, SchemeStatus.Red, failedAtStage
        = Some("online tests")) :: CurrentSchemeStatus(Schemes.DaT, SchemeStatus.Red, failedAtStage = Some("online tests")) :: Nil

      page.userDataWithSchemes.withdrawnSchemes mustBe Nil
      page.userDataWithSchemes.hasNumericRequirement mustBe false
      page.userDataWithSchemes.hasFormRequirement mustBe false
      page.fsacStage mustBe PostOnlineTestsStage.OTHER
    }

    "application is in correct stage" in {
      val app = userDataWithApp.application.copy(
        progress = userDataWithApp.application.progress.copy(
        assessmentCentre = userDataWithApp.application.progress.assessmentCentre.copy(allocationUnconfirmed = true)
      ))
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, app, Schemes.AllSchemes, Seq.empty)

      val allocation = CandidateAllocationWithEvent(
        cachedUserMetadata.application.applicationId.toString,
        "",
        AllocationStatuses.UNCONFIRMED,
        EventsExamples.Event1
      )
      val page = PostOnlineTestsPage(cachedUserMetadata, Seq(allocation), None, hasAnalysisExercise = false, List.empty)

      page.fsacStage mustBe PostOnlineTestsStage.ALLOCATED_TO_EVENT
    }

    "indicate all schemes are failed when allSchemesFailed is called and all schemes are red" in {
      val phase3ResultsAllRed = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Red.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Red.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Red.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, phase3ResultsAllRed)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.allSchemesFailed mustBe true
    }

    "not indicate all schemes are failed when allSchemesFailed is called and one scheme is green" in {
      val phase3ResultsOneGreen = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Red.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Red.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Green.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, phase3ResultsOneGreen)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.allSchemesFailed mustBe false
    }

    "indicate passed when firstResidualPreferencePassed is called and schemes are: Green, Green, Green" in {
      val schemeResults = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Green.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Green.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Green.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, schemeResults)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.firstResidualPreferencePassed mustBe true
    }

    "indicate passed when firstResidualPreferencePassed is called and schemes are: Red, Red, Green" in {
      val schemeResults = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Red.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Red.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Green.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, schemeResults)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.firstResidualPreferencePassed mustBe true
    }

    "indicate failed when firstResidualPreferencePassed is called and schemes are: Red, Red, Red" in {
      val schemeResults = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Red.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Red.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Red.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, schemeResults)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.firstResidualPreferencePassed mustBe false
    }

    "indicate failed when firstResidualPreferencePassed is called and schemes are: Amber, Amber, Amber" in {
      val schemeResults = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Amber.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Amber.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Amber.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, schemeResults)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.firstResidualPreferencePassed mustBe false
    }

    "indicate failed when firstResidualPreferencePassed is called and schemes are: Amber, Green, Red" in {
      val schemeResults = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Amber.toString)  ::
        SchemeEvaluationResult(SchemeId("DigitalAndTechnology"), SchemeStatus.Green.toString) ::
        SchemeEvaluationResult(SchemeId("HumanResources"), SchemeStatus.Red.toString) ::
        Nil
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userDataWithApp.application,
        Schemes.AllSchemes, schemeResults)

      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = false, List.empty)

      page.firstResidualPreferencePassed mustBe false
    }

    "indicate final failed when assessment_centre failed and no Green schemes" in {
      val schemeResults = SchemeEvaluationResult(SchemeId("Commercial"), SchemeStatus.Red.toString) :: Nil
      val userApp = userDataWithApp.application.copy(
        progress = userDataWithApp.application.progress.copy(
          assessmentCentre = userDataWithApp.application.progress.assessmentCentre.copy(failed = true)
        ))
      val cachedUserMetadata = CachedUserWithSchemeData(userDataWithApp.user, userApp, Schemes.AllSchemes, schemeResults)
      val page = PostOnlineTestsPage(cachedUserMetadata, Seq.empty, None, hasAnalysisExercise = true, List.empty)

      page.isFinalFailure mustBe true
    }
  }
}
