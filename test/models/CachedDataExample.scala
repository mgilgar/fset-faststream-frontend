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

package models

import java.util.UUID

import connectors.exchange.{AssessmentCentre, AssessmentScores}
import models.ApplicationData.ApplicationStatus

object CachedDataExample {
  val ActiveCandidateUser = CachedUser(UniqueIdentifier(UUID.randomUUID.toString), "firstName", "lastName",
    Some("preferredName"), email = "email@test.com", isActive = true, "")
  val LockedCandidateUser = CachedUser(UniqueIdentifier(UUID.randomUUID.toString), "firstName", "lastName",
    Some("preferredName"), email = "email@test.com", isActive = true, lockStatus = "LOCKED")
  val NonActiveCandidateUser = CachedUser(UniqueIdentifier(UUID.randomUUID.toString), "firstName", "lastName",
    Some("preferredName"), email = "email@test.com", isActive = false, "")

  val InitialProgress = Progress(false, false, false, false, false, false, false, false, false, false,
    OnlineTestProgress(false, false, false, false, false, false, false, false, false, false),
    false,
    AssessmentScores(false, false),
    AssessmentCentre(false, false, false)
  )
  val CreatedApplication = ApplicationData(
    UniqueIdentifier(UUID.randomUUID.toString),
    UniqueIdentifier(UUID.randomUUID.toString),
    ApplicationStatus.CREATED,
    InitialProgress
  )

  val InPersonalDetailsApplication = CreatedApplication.copy(applicationStatus = ApplicationStatus.IN_PROGRESS,
    progress = InitialProgress.copy(personalDetails = true))

  val InFrameworkDetailsApplication = InPersonalDetailsApplication.copy(applicationStatus = ApplicationStatus.IN_PROGRESS,
    progress = InitialProgress.copy(schemePreferences = true))

  val InAssistanceDetailsApplication = InFrameworkDetailsApplication.copy(applicationStatus = ApplicationStatus.IN_PROGRESS,
    progress = InitialProgress.copy(assistanceDetails = true))

  val ActiveCandidate = CachedData(ActiveCandidateUser, Some(CreatedApplication))
}
