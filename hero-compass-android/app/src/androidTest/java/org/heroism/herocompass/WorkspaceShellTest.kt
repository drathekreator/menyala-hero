package org.heroism.herocompass

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import org.heroism.herocompass.auth.MentorAuthRepository
import org.heroism.herocompass.children.AddChildResult
import org.heroism.herocompass.children.ChildRecord
import org.heroism.herocompass.children.ChildrenRepository
import org.heroism.herocompass.children.BirthDatePicker
import org.heroism.herocompass.children.ChildGender
import org.heroism.herocompass.assessment.AssessmentSaveResult
import org.heroism.herocompass.assessment.AssessmentPhase
import org.heroism.herocompass.assessment.IdentityForeclosureAssessment
import org.heroism.herocompass.assessment.IdentityForeclosureRepository
import org.heroism.herocompass.assessment.IdentityForeclosureRules
import org.heroism.herocompass.assessment.FutureOrientationAssessment
import org.heroism.herocompass.assessment.FutureOrientationCategory
import org.heroism.herocompass.assessment.FutureOrientationRepository
import org.heroism.herocompass.assessment.FutureOrientationRules
import org.heroism.herocompass.assessment.FutureOrientationSaveResult
import org.heroism.herocompass.assessment.HeroAsaAssessment
import org.heroism.herocompass.assessment.HeroAsaCategory
import org.heroism.herocompass.assessment.HeroAsaRepository
import org.heroism.herocompass.assessment.HeroAsaRules
import org.heroism.herocompass.assessment.HeroAsaSaveResult
import org.heroism.herocompass.assessment.HeroAksiAssessment
import org.heroism.herocompass.assessment.HeroAksiCategory
import org.heroism.herocompass.assessment.HeroAksiInstrument
import org.heroism.herocompass.assessment.HeroAksiRepository
import org.heroism.herocompass.assessment.HeroAksiRules
import org.heroism.herocompass.assessment.HeroAksiSaveResult
import org.heroism.herocompass.assessment.HeroRasaAssessment
import org.heroism.herocompass.assessment.HeroRasaCategory
import org.heroism.herocompass.assessment.HeroRasaInstrument
import org.heroism.herocompass.assessment.HeroRasaRepository
import org.heroism.herocompass.assessment.HeroRasaRules
import org.heroism.herocompass.assessment.HeroRasaSaveResult
import org.heroism.herocompass.intervention.InterventionRecord
import org.heroism.herocompass.intervention.InterventionRepository
import org.heroism.herocompass.intervention.SaveInterventionResult
import org.heroism.herocompass.ml.BaselineModelInput
import org.heroism.herocompass.ml.ExperimentalBaselineModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabsShortcutAndLockAreAvailable() {
        var lockRequested = false
        val children = InMemoryChildrenRepository()

        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = InMemoryIdentityForeclosureRepository()) { lockRequested = true }
            }
        }

        composeRule.onNodeWithText("Dampingi setiap langkah.").assertIsDisplayed()
        composeRule.onNodeWithTag("workspace-tab-home").assertIsSelected()
        composeRule.onNodeWithText("Belum ada anak").assertIsDisplayed()
        composeRule.onNodeWithText("—").assertIsDisplayed()
        composeRule.onNodeWithTag("home-program-stages").assertIsDisplayed()
        composeRule.onNodeWithText("Pra-program").assertIsDisplayed()
        composeRule.onNodeWithText("Intervensi").assertIsDisplayed()
        composeRule.onNodeWithText("Pasca-program").assertIsDisplayed()
        composeRule.onNodeWithText("ToT → Pre-test").assertDoesNotExist()
        composeRule.onNodeWithText("Asa → Aksi → Rasa → Karsa").assertDoesNotExist()

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("workspace-tab-search").assertIsSelected()
        composeRule.onNodeWithTag("search-child-query").assertIsDisplayed()
        composeRule.onNodeWithText("Cari anak").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("workspace-tab-children").assertIsSelected()
        composeRule.onNodeWithTag("child-add").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-tab-guide").performClick()
        composeRule.onNodeWithTag("workspace-tab-guide").assertIsSelected()
        composeRule.onNodeWithTag("assessment-guide-page").assertIsDisplayed()
        composeRule.onNodeWithText("Panduan program").assertIsDisplayed()
        composeRule.onNodeWithText("Identity Foreclosure").assertIsDisplayed()
        composeRule.onNodeWithText("Orientasi Masa Depan").assertIsDisplayed()
        composeRule.onNodeWithTag("guide-program-cycle").assertIsDisplayed()
        composeRule.onNodeWithText("ToT awal · pre-test Identitas dan Orientasi Masa Depan").assertIsDisplayed()
        composeRule.onNodeWithText("Post-test Identitas + Orientasi Masa Depan · evaluasi · intervensi lanjutan").assertIsDisplayed()
        composeRule.onNodeWithTag("guide-program-card-asa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("guide-program-card-aksi").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("guide-program-card-rasa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("guide-program-card-karsa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("rubrik skornya tidak tersedia.", substring = true).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-tab-profile").performClick()
        composeRule.onNodeWithTag("workspace-tab-profile").assertIsSelected()
        composeRule.onNodeWithText("Profil").assertIsDisplayed()
        composeRule.onNodeWithTag("backup-card").assertIsDisplayed()
        composeRule.onNodeWithTag("backup-export").assertIsDisplayed()
        composeRule.onNodeWithTag("backup-restore").assertIsDisplayed()
        composeRule.onNodeWithTag("mentor-avatar-pick").assertIsDisplayed()
        composeRule.onNodeWithTag("mentor-biometric-switch").assertIsDisplayed()
        composeRule.onNodeWithText("PIN tidak ikut disalin.", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Kunci aplikasi").performClick()
        composeRule.runOnIdle { assertTrue(lockRequested) }

        composeRule.onNodeWithTag("workspace-tab-home").performClick()
        composeRule.onNodeWithText("Lihat anak").performClick()
        composeRule.onNodeWithText("Mulai dengan satu anak").assertIsDisplayed()
    }

    @Test
    fun homeShowsActiveIdentityCycleFromAssessmentRecords() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val assessments = InMemoryIdentityForeclosureRepository()
        assertTrue(assessments.complete(child.id, List(15) { 2 }, AssessmentPhase.PRE_TEST) is AssessmentSaveResult.Completed)

        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = assessments) { }
            }
        }

        composeRule.onNodeWithTag("home-active-cycle-count").assertTextEquals("1")
        assertTrue(assessments.complete(child.id, List(15) { 3 }, AssessmentPhase.POST_TEST) is AssessmentSaveResult.Completed)
        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("workspace-tab-home").performClick()
        composeRule.onNodeWithTag("home-active-cycle-count").assertTextEquals("0")
    }

    @Test
    fun programStageTracksBaselineCompletionAndPostTestStart() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val identity = InMemoryIdentityForeclosureRepository()
        val future = InMemoryFutureOrientationRepository()
        assertTrue(identity.complete(child.id, List(15) { 2 }) is AssessmentSaveResult.Completed)
        assertTrue(future.complete(child.id, List(15) { 3 }) is FutureOrientationSaveResult.Completed)

        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = identity,
                    futureOrientationRepository = future,
                    interventionRepository = InMemoryInterventionRepository(),
                ) { }
            }
        }

        composeRule.onNodeWithContentDescription("Tahap Intervensi, sedang berjalan").assertIsDisplayed()
        composeRule.onNodeWithTag("home-program-stage-detail").assertTextEquals("Catatan kegiatan: 0/1 anak")

        assertTrue(future.saveDraft(child.id, List(15) { 3 }, AssessmentPhase.POST_TEST) is FutureOrientationSaveResult.SavedDraft)
        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("workspace-tab-home").performClick()

        composeRule.onNodeWithContentDescription("Tahap Pasca-program, sedang berjalan").assertIsDisplayed()
        composeRule.onNodeWithTag("home-program-stage-detail").assertTextEquals("Post-test Identitas + Orientasi: 0/1 anak lengkap")
    }

    @Test
    fun savedMentorPhotoIsShownInHomeHeader() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val avatarSource = File(context.cacheDir, "home-avatar-test.jpg")
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.MAGENTA)
        }
        avatarSource.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        val authRepository = MentorAuthRepository(context)
        try {
            assertTrue(authRepository.saveAvatar(Uri.fromFile(avatarSource)))
            composeRule.setContent {
                MaterialTheme {
                    WorkspaceShell(
                        mentorName = "Pendamping",
                        childrenRepository = InMemoryChildrenRepository(),
                        birthDatePicker = testBirthDatePicker,
                        assessmentRepository = InMemoryIdentityForeclosureRepository(),
                        mentorAuthRepository = authRepository,
                    ) { }
                }
            }

            composeRule.onNodeWithTag("home-mentor-avatar").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Foto profil pendamping Pendamping").assertIsDisplayed()
        } finally {
            authRepository.removeAvatar()
            avatarSource.delete()
        }
    }

    @Test
    fun childCanBeAddedListedAndFoundFromSearch() {
        val children = InMemoryChildrenRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = InMemoryIdentityForeclosureRepository()) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-add").performClick()
        composeRule.onNodeWithTag("child-name-field").performTextInput("Ayu Lestari")
        composeRule.onNodeWithTag("child-save").performClick()
        composeRule.onNodeWithText("Pilih tanggal lahir terlebih dahulu.").assertIsDisplayed()

        composeRule.onNodeWithText("Pilih tanggal lahir").performClick()
        composeRule.onNodeWithText("12 April 2015").assertIsDisplayed()
        composeRule.onNodeWithTag("child-save").performClick()
        composeRule.onNodeWithText("Pilih jenis kelamin anak.").assertIsDisplayed()
        composeRule.onNodeWithTag("child-gender-boy").performClick()
        composeRule.onNodeWithTag("child-save").performClick()
        composeRule.onNodeWithText("Pilih keterlibatan memulung anak.").assertIsDisplayed()
        composeRule.onNodeWithTag("child-scavenging-no").performClick()
        composeRule.onNodeWithTag("child-save").performClick()
        composeRule.onNodeWithText("Ayu Lestari").assertIsDisplayed()
        composeRule.onNodeWithText("Laki-laki · Memulung: Tidak").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-query").performTextInput("Ayu")
        composeRule.onNodeWithText("Ayu Lestari").assertIsDisplayed()
    }

    @Test
    fun identityForeclosurePretestSupportsDraftAndScoresCompletedAnswers() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        children.add("Bima Pratama", LocalDate.of(2014, 8, 20), ChildGender.Boy, true)
        val assessments = InMemoryIdentityForeclosureRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = assessments) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-start-pretest").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Pre-test identitas").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-save-draft").performScrollTo().performClick()
        composeRule.onNodeWithText("Draf pre-test tersimpan di perangkat ini.").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithText("Lengkapi semua butir sebelum menyelesaikan pre-test.").assertIsDisplayed()

        repeat(15) { index ->
            composeRule.onNodeWithTag("assessment-answer-$index-1").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithText("15 dari 60").assertIsDisplayed()
        composeRule.onNodeWithText("Kecenderungan identity foreclosure tinggi").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-tab-home").performClick()
    }

    @Test
    fun switchingTabsFromPretestLeavesPageAndSavesPartialAnswers() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val assessments = InMemoryIdentityForeclosureRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = assessments) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-start-pretest").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithTag("assessment-answer-0-3").performScrollTo().performClick()
        composeRule.onNodeWithTag("workspace-tab-home").performClick()

        composeRule.onNodeWithTag("workspace-tab-home").assertIsSelected()
        composeRule.onNodeWithText("Dampingi setiap langkah.").assertIsDisplayed()
        assertTrue(assessments.get(child.id)?.answers?.first() == 3)

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-start-pretest").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Terjawab 1 dari 15").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-answer-0-3").assertIsDisplayed()
        assertTrue(assessments.get(child.id)?.answers?.first() == 3)
    }

    @Test
    fun childrenAndSearchListsPaginateAfterFiveResults() {
        val children = InMemoryChildrenRepository()
        repeat(6) { index ->
            children.add("Anak ${index + 1}", LocalDate.of(2015, 4, 12).minusDays(index.toLong()), ChildGender.Girl, false)
        }
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = InMemoryIdentityForeclosureRepository(),
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithText("Anak 1").assertIsDisplayed()
        composeRule.onNodeWithTag("child-card-test-4").assertExists()
        composeRule.onNodeWithTag("children-page-2").assertExists()
        composeRule.onNodeWithText("Berikutnya").assertDoesNotExist()
        composeRule.onNodeWithTag("children-next-page").performScrollTo().performClick()
        composeRule.onNodeWithTag("children-page-indicator").assertTextEquals("2")
        composeRule.onNodeWithTag("child-card-test-5").assertExists()

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-query").performTextInput("Anak")
        composeRule.onNodeWithText("6 anak ditemukan").assertIsDisplayed()
        composeRule.onNodeWithText("Anak 1").assertIsDisplayed()
        composeRule.onNodeWithTag("search-next-page").performScrollTo().performClick()
        composeRule.onNodeWithTag("search-child-card-test-5").assertExists()
        composeRule.onNodeWithTag("search-page-indicator").assertIsDisplayed()
    }

    @Test
    fun searchResultsShowSavedPerChildAssessmentSummary() {
        val children = InMemoryChildrenRepository()
        val ayu = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        children.add("Bima Pratama", LocalDate.of(2014, 8, 20), ChildGender.Boy, true)
        val assessments = InMemoryIdentityForeclosureRepository()
        assertTrue(assessments.complete(ayu.id, List(15) { 1 }) is AssessmentSaveResult.Completed)

        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = assessments,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-query").performTextInput("Ayu")
        composeRule.onNodeWithText("Kecenderungan identity foreclosure tinggi · 15/60").assertIsDisplayed()
        composeRule.onNodeWithText("1 anak ditemukan").assertIsDisplayed()

        composeRule.onNodeWithTag("search-child-query").performTextClearance()
        composeRule.onNodeWithText("Belum dimulai").assertIsDisplayed()
    }

    @Test
    fun searchChildCardOpensStatisticsAndRecommendation() {
        val children = InMemoryChildrenRepository()
        val ayu = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val rani = (children.add("Rani Pratama", LocalDate.now().minusYears(8), ChildGender.Girl, false) as AddChildResult.Added).child
        val assessments = InMemoryIdentityForeclosureRepository()
        val futureAssessments = InMemoryFutureOrientationRepository()
        assertTrue(assessments.complete(ayu.id, List(15) { 1 }) is AssessmentSaveResult.Completed)
        assertTrue(futureAssessments.complete(ayu.id, List(15) { 3 }) is FutureOrientationSaveResult.Completed)
        assertTrue(assessments.complete(rani.id, List(15) { if (it == 14) 3 else 1 }) is AssessmentSaveResult.Completed)
        assertTrue(futureAssessments.complete(rani.id, List(15) { 1 }) is FutureOrientationSaveResult.Completed)

        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = assessments,
                    futureOrientationRepository = futureAssessments,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-card-${ayu.id}").performClick()
        composeRule.onNodeWithTag("child-detail-page-${ayu.id}").assertIsDisplayed()
        composeRule.onNodeWithText("Statistik anak").assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-baseline-summary").assertIsDisplayed()
        composeRule.onNodeWithText("15", substring = false).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-score-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-future-score-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("child-instrument-future").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("experimental-ml-forecast").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("experimental-ml-focus").assertTextEquals("HERO Asa · Potensi")
        composeRule.onNodeWithTag("experimental-ml-status-Y_Potensi").performScrollTo().assertTextEquals("PERLU")
        composeRule.onNodeWithTag("experimental-ml-status-Y_Eksplorasi").performScrollTo().assertTextEquals("TIDAK DITANDAI")
        composeRule.onNodeWithTag("experimental-ml-activity").assertTextEquals("Buat peta minat dan cita-cita. Pilih satu profesi untuk dikenali lewat cerita, video, atau narasumber.")
        composeRule.onNodeWithTag("experimental-ml-details-toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("experimental-ml-metrics").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("experimental-ml-explanation").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("child-detail-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${ayu.id}").assertDoesNotExist()
        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-card-${rani.id}").performClick()
        composeRule.onNodeWithTag("experimental-ml-focus").performScrollTo().assertTextEquals("HERO Aksi · Eksplorasi")
        composeRule.onNodeWithTag("experimental-ml-status-Y_Eksplorasi").performScrollTo().assertTextEquals("PERLU")
        composeRule.onNodeWithTag("experimental-ml-activity").performScrollTo().assertTextEquals("Ajak anak mencoba peran atau profesi lewat role-play, wawancara narasumber, atau kunjungan edukatif seperti KidZania bila tersedia.")
    }

    @Test
    fun baselineModelMatchesExportedLogisticRegressionGoldenValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val forecast = ExperimentalBaselineModel.load(context).predict(
            BaselineModelInput(
                ageAtPretest = 11,
                identityForeclosurePretest = 15,
                futureOrientationPretest = 45,
                genderCode = "P",
                scavengingInvolved = false,
            ),
        )

        assertEquals(4, forecast?.estimates?.size)
        assertEquals(0.7196597323, forecast!!.estimates[0].probabilityBelowTarget, 1e-8)
        assertEquals(0.1978072491, forecast.estimates[1].probabilityBelowTarget, 1e-8)
        assertEquals(0.2084421356, forecast.estimates[2].probabilityBelowTarget, 1e-8)
        assertEquals(0.1588756747, forecast.estimates[3].probabilityBelowTarget, 1e-8)
    }

    @Test
    fun childStatisticsComparePreAndPostScoresPerInstrument() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val assessments = InMemoryIdentityForeclosureRepository().apply {
            complete(child.id, List(15) { 1 }, AssessmentPhase.PRE_TEST)
            complete(child.id, List(15) { 2 }, AssessmentPhase.POST_TEST)
        }
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = assessments) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("comparison-identity-scores").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pre-test 15/60   ·   Post-test 30/60").assertIsDisplayed()
        composeRule.onNodeWithTag("comparison-identity-change").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perubahan skor +15").assertIsDisplayed()
        composeRule.onNodeWithTag("comparison-future-change").performScrollTo().assertTextEquals("Lengkapi pre-test dan post-test untuk membandingkan skor")
    }

    @Test
    fun childWithoutPretestCanStartItFromSearchDetail() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = InMemoryIdentityForeclosureRepository()) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-start-pretest").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Pre-test identitas").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-back").performClick()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-method-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()
    }

    @Test
    fun childDetailCanRecordAndDisplayAnActualFollowUpActivity() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val followUps = InMemoryInterventionRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = InMemoryIdentityForeclosureRepository(),
                    interventionRepository = followUps,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("intervention-add").performScrollTo().performClick()
        composeRule.onNodeWithTag("intervention-program-name").performScrollTo().performTextInput("Sesi refleksi bersama")
        composeRule.onNodeWithTag("intervention-notes").performScrollTo().performTextInput("Anak memilih cerita yang ingin dibahas.")
        composeRule.onNodeWithTag("intervention-save").performScrollTo().performClick()

        composeRule.onNodeWithText("Sesi refleksi bersama").assertIsDisplayed()
        composeRule.onNodeWithText("Anak memilih cerita yang ingin dibahas.").assertIsDisplayed()
        composeRule.onNodeWithTag("intervention-catalog-note").assertIsDisplayed()

        val recordId = followUps.getForChild(child.id).single().id
        composeRule.onNodeWithTag("intervention-edit-$recordId").performScrollTo().performClick()
        composeRule.onNodeWithTag("intervention-program-name").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("intervention-program-name").performTextInput("Sesi refleksi diperbarui")
        composeRule.onNodeWithTag("intervention-save").performScrollTo().performClick()
        composeRule.onNodeWithText("Sesi refleksi diperbarui").assertIsDisplayed()

        composeRule.onNodeWithTag("intervention-delete-$recordId").performScrollTo().performClick()
        composeRule.onNodeWithTag("intervention-delete-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Batal").performClick()
        composeRule.onNodeWithTag("intervention-delete-$recordId").performScrollTo().performClick()
        composeRule.onNodeWithTag("intervention-delete-confirm").performClick()
        assertTrue(followUps.getForChild(child.id).isEmpty())
    }

    @Test
    fun deletingChildRequiresConfirmationAndRemovesAllLinkedRecords() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val identity = InMemoryIdentityForeclosureRepository().apply { complete(child.id, List(15) { 2 }) }
        val future = InMemoryFutureOrientationRepository().apply { complete(child.id, List(15) { 3 }) }
        val asa = InMemoryHeroAsaRepository().apply { complete(child.id, List(10) { 3 }) }
        val aksi = InMemoryHeroAksiRepository().apply { complete(child.id, List(10) { 2 }) }
        val rasa = InMemoryHeroRasaRepository().apply { complete(child.id, List(10) { 4 }) }
        val interventions = InMemoryInterventionRepository().apply {
            add(child.id, LocalDate.now(), "HERO Asa", "Catatan pilot")
        }
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = identity,
                    futureOrientationRepository = future,
                    heroAsaRepository = asa,
                    heroAksiRepository = aksi,
                    heroRasaRepository = rasa,
                    interventionRepository = interventions,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-delete").performScrollTo().performClick()
        composeRule.onNodeWithTag("child-delete-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("child-delete-cancel").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()

        composeRule.onNodeWithTag("child-detail-delete").performScrollTo().performClick()
        composeRule.onNodeWithTag("child-delete-confirm").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertDoesNotExist()
        composeRule.runOnIdle {
            assertTrue(children.getAll().none { it.id == child.id })
            assertEquals(null, identity.get(child.id))
            assertEquals(null, future.get(child.id))
            assertEquals(null, asa.get(child.id))
            assertEquals(null, aksi.get(child.id))
            assertEquals(null, rasa.get(child.id))
            assertTrue(interventions.getForChild(child.id).isEmpty())
        }
    }

    @Test
    fun childProfileCanBeCorrectedWithoutDeletingItsAssessments() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val identity = InMemoryIdentityForeclosureRepository().apply { complete(child.id, List(15) { 2 }) }
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = identity,
                    futureOrientationRepository = InMemoryFutureOrientationRepository(),
                    heroAsaRepository = InMemoryHeroAsaRepository(),
                    heroAksiRepository = InMemoryHeroAksiRepository(),
                    heroRasaRepository = InMemoryHeroRasaRepository(),
                    interventionRepository = InMemoryInterventionRepository(),
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-edit").performScrollTo().performClick()
        composeRule.onNodeWithTag("child-edit-name").performTextClearance()
        composeRule.onNodeWithTag("child-edit-name").performTextInput("Ayu Wibowo")
        composeRule.onNodeWithTag("child-edit-gender-boy").performClick()
        composeRule.onNodeWithTag("child-edit-scavenging-yes").performClick()
        composeRule.onNodeWithTag("child-edit-save").performClick()
        composeRule.onNodeWithText("Ayu Wibowo").assertIsDisplayed()
        composeRule.onNodeWithText("Laki-laki · Memulung: Ya").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("Ayu Wibowo", children.getAll().single().name)
            assertEquals(30, identity.get(child.id)?.totalScore)
        }
    }

    @Test
    fun identityOcrEntryPointIsAvailableForPretestAndPosttest() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val completedChild = (children.add("Bima Pratama", LocalDate.of(2014, 8, 20), ChildGender.Boy, true) as AddChildResult.Added).child
        val assessments = InMemoryIdentityForeclosureRepository().apply {
            complete(completedChild.id, List(15) { 1 })
        }
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(mentorName = "Pendamping", childrenRepository = children, birthDatePicker = testBirthDatePicker, assessmentRepository = assessments) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-start-pretest").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-camera").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-file").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-method-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-tab-search").performClick()
        composeRule.onNodeWithTag("search-child-card-${completedChild.id}").performClick()
        composeRule.onNodeWithTag("child-detail-post-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-camera").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-file").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").assertIsDisplayed()
    }

    @Test
    fun futureOrientationSupportsSeparatePreAndPostManualAssessments() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val futureAssessments = InMemoryFutureOrientationRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = InMemoryIdentityForeclosureRepository(),
                    futureOrientationRepository = futureAssessments,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-detail-future-pre-test").performScrollTo().performClick()
        composeRule.onNodeWithText("${child.name} · Orientasi Masa Depan").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-camera").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-file").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Pre-test orientasi masa depan").assertIsDisplayed()
        composeRule.onNodeWithText("15–29 rendah, 30–44 sedang, 45–60 tinggi", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("future-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithText("Lengkapi semua butir sebelum menyelesaikan pre-test.").assertIsDisplayed()
        repeat(15) { index ->
            composeRule.onNodeWithTag("future-assessment-answer-$index-3").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("future-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithTag("future-assessment-result-score").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            val pre = futureAssessments.get(child.id, AssessmentPhase.PRE_TEST)
            assertTrue(pre?.isComplete == true)
            assertTrue(pre?.totalScore == 45)
            assertTrue(pre?.category == FutureOrientationCategory.HIGH)
        }

        composeRule.onNodeWithTag("future-assessment-back").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-method-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-future-post-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        repeat(15) { index ->
            composeRule.onNodeWithTag("future-assessment-answer-$index-2").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("future-assessment-complete").performScrollTo().performClick()
        composeRule.runOnIdle {
            val pre = futureAssessments.get(child.id, AssessmentPhase.PRE_TEST)
            val post = futureAssessments.get(child.id, AssessmentPhase.POST_TEST)
            assertEquals(45, pre?.totalScore)
            assertEquals(30, post?.totalScore)
            assertEquals(FutureOrientationCategory.MEDIUM, post?.category)
        }
    }

    @Test
    fun heroAsaForethoughtSupportsManualPreAndPostWithSeparateResults() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val asaAssessments = InMemoryHeroAsaRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = InMemoryIdentityForeclosureRepository(),
                    futureOrientationRepository = InMemoryFutureOrientationRepository(),
                    heroAsaRepository = asaAssessments,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-instrument-asa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-asa-pre-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-camera").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-file").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Pre-test HERO Asa").assertIsDisplayed()
        composeRule.onNodeWithTag("hero-asa-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithText("Lengkapi semua butir sebelum menyelesaikan pre-test.").assertIsDisplayed()
        repeat(10) { index -> composeRule.onNodeWithTag("hero-asa-assessment-answer-$index-3").performScrollTo().performClick() }
        composeRule.onNodeWithTag("hero-asa-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithTag("hero-asa-assessment-result-score").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(30, asaAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
            assertEquals(HeroAsaCategory.HIGH, asaAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.category)
        }

        composeRule.onNodeWithTag("hero-asa-assessment-back").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-method-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-asa-post-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        repeat(10) { index -> composeRule.onNodeWithTag("hero-asa-assessment-answer-$index-2").performScrollTo().performClick() }
        composeRule.onNodeWithTag("hero-asa-assessment-complete").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(30, asaAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
            assertEquals(20, asaAssessments.get(child.id, AssessmentPhase.POST_TEST)?.totalScore)
            assertEquals(HeroAsaCategory.MEDIUM, asaAssessments.get(child.id, AssessmentPhase.POST_TEST)?.category)
        }
    }

    @Test
    fun heroAksiPerformanceSupportsManualPreAndPostWithSeparateResults() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val aksiAssessments = InMemoryHeroAksiRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = InMemoryIdentityForeclosureRepository(),
                    futureOrientationRepository = InMemoryFutureOrientationRepository(),
                    heroAksiRepository = aksiAssessments,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-instrument-aksi").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-aksi-pre-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-camera").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-file").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Pre-test HERO Aksi").assertIsDisplayed()
        composeRule.onNodeWithTag("hero-aksi-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithText("Lengkapi semua butir sebelum menyelesaikan pre-test.").assertIsDisplayed()
        repeat(10) { index -> composeRule.onNodeWithTag("hero-aksi-assessment-answer-$index-3").performScrollTo().performClick() }
        composeRule.onNodeWithTag("hero-aksi-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithTag("hero-aksi-assessment-result-score").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(30, aksiAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
            assertEquals(HeroAksiCategory.HIGH, aksiAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.category)
        }

        composeRule.onNodeWithTag("hero-aksi-assessment-back").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-method-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-aksi-post-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        repeat(10) { index -> composeRule.onNodeWithTag("hero-aksi-assessment-answer-$index-2").performScrollTo().performClick() }
        composeRule.onNodeWithTag("hero-aksi-assessment-complete").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(30, aksiAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
            assertEquals(20, aksiAssessments.get(child.id, AssessmentPhase.POST_TEST)?.totalScore)
            assertEquals(HeroAksiCategory.MEDIUM, aksiAssessments.get(child.id, AssessmentPhase.POST_TEST)?.category)
        }
    }

    @Test
    fun heroRasaReflectionSupportsManualPreAndPostWithSeparateResults() {
        val children = InMemoryChildrenRepository()
        val child = (children.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val rasaAssessments = InMemoryHeroRasaRepository()
        composeRule.setContent {
            MaterialTheme {
                WorkspaceShell(
                    mentorName = "Pendamping",
                    childrenRepository = children,
                    birthDatePicker = testBirthDatePicker,
                    assessmentRepository = InMemoryIdentityForeclosureRepository(),
                    futureOrientationRepository = InMemoryFutureOrientationRepository(),
                    heroRasaRepository = rasaAssessments,
                ) { }
            }
        }

        composeRule.onNodeWithTag("workspace-tab-children").performClick()
        composeRule.onNodeWithTag("child-card-${child.id}").performClick()
        composeRule.onNodeWithTag("child-instrument-rasa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-rasa-pre-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-camera").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-file").assertIsEnabled()
        composeRule.onNodeWithTag("assessment-input-method-page").assertIsDisplayed()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        composeRule.onNodeWithText("Pre-test HERO Rasa").assertIsDisplayed()
        composeRule.onNodeWithTag("hero-rasa-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithText("Lengkapi semua butir sebelum menyelesaikan pre-test.").assertIsDisplayed()
        repeat(10) { index -> composeRule.onNodeWithTag("hero-rasa-assessment-answer-$index-3").performScrollTo().performClick() }
        composeRule.onNodeWithTag("hero-rasa-assessment-complete").performScrollTo().performClick()
        composeRule.onNodeWithTag("hero-rasa-assessment-result-score").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(30, rasaAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
            assertEquals(HeroRasaCategory.HIGH, rasaAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.category)
        }

        composeRule.onNodeWithTag("hero-rasa-assessment-back").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-method-back").performClick()
        composeRule.onNodeWithTag("child-detail-page-${child.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("child-detail-rasa-post-test").performScrollTo().performClick()
        composeRule.onNodeWithTag("assessment-input-manual").performClick()
        repeat(10) { index -> composeRule.onNodeWithTag("hero-rasa-assessment-answer-$index-2").performScrollTo().performClick() }
        composeRule.onNodeWithTag("hero-rasa-assessment-complete").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(30, rasaAssessments.get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
            assertEquals(20, rasaAssessments.get(child.id, AssessmentPhase.POST_TEST)?.totalScore)
            assertEquals(HeroRasaCategory.MEDIUM, rasaAssessments.get(child.id, AssessmentPhase.POST_TEST)?.category)
        }
    }
}

private val testBirthDatePicker = BirthDatePicker { _, onDateSelected -> onDateSelected(LocalDate.of(2015, 4, 12)) }

private class InMemoryChildrenRepository : ChildrenRepository {
    private val records = mutableListOf<ChildRecord>()
    override fun getAll(): List<ChildRecord> = records.toList()
    override fun add(name: String, birthDate: LocalDate, gender: ChildGender, involvedInScavenging: Boolean): AddChildResult {
        val record = ChildRecord("test-${records.size}", name.trim(), birthDate, gender, involvedInScavenging)
        records += record
        return AddChildResult.Added(record)
    }
    override fun delete(childId: String): Boolean { records.removeAll { it.id == childId }; return true }
    override fun update(child: ChildRecord): Boolean {
        val index = records.indexOfFirst { it.id == child.id }
        if (index < 0) return false
        records[index] = child
        return true
    }
}

private class InMemoryIdentityForeclosureRepository : IdentityForeclosureRepository {
    private val records = mutableMapOf<Pair<String, AssessmentPhase>, IdentityForeclosureAssessment>()

    override fun get(childId: String, phase: AssessmentPhase): IdentityForeclosureAssessment? = records[childId to phase]
    override fun deleteForChild(childId: String): Boolean { records.keys.removeAll { it.first == childId }; return true }
    override fun mergeImported(records: List<IdentityForeclosureAssessment>): Boolean { records.forEach { this.records[it.childId to it.phase] = it }; return true }

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): AssessmentSaveResult {
        if (!IdentityForeclosureRules.validateAnswers(answers)) return AssessmentSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return AssessmentSaveResult.AlreadyCompleted
        val record = IdentityForeclosureAssessment(records[key]?.id ?: childId, childId, answers, null, null, null, phase)
        records[key] = record
        return AssessmentSaveResult.SavedDraft(record)
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): AssessmentSaveResult {
        val score = IdentityForeclosureRules.score(answers) ?: return AssessmentSaveResult.InvalidAnswers
        val category = IdentityForeclosureRules.category(score) ?: return AssessmentSaveResult.InvalidAnswers
        val key = childId to phase
        val record = IdentityForeclosureAssessment(records[key]?.id ?: childId, childId, answers, score, category, System.currentTimeMillis(), phase)
        records[key] = record
        return AssessmentSaveResult.Completed(record)
    }
}

private class InMemoryFutureOrientationRepository : FutureOrientationRepository {
    private val records = mutableMapOf<Pair<String, AssessmentPhase>, FutureOrientationAssessment>()

    override fun get(childId: String, phase: AssessmentPhase): FutureOrientationAssessment? = records[childId to phase]
    override fun deleteForChild(childId: String): Boolean { records.keys.removeAll { it.first == childId }; return true }
    override fun mergeImported(records: List<FutureOrientationAssessment>): Boolean { records.forEach { this.records[it.childId to it.phase] = it }; return true }

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): FutureOrientationSaveResult {
        if (childId.isBlank() || !FutureOrientationRules.validateAnswers(answers)) return FutureOrientationSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return FutureOrientationSaveResult.AlreadyCompleted
        val record = FutureOrientationAssessment(records[key]?.id ?: childId, childId, answers.toList(), null, null, null, phase)
        records[key] = record
        return FutureOrientationSaveResult.SavedDraft(record)
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): FutureOrientationSaveResult {
        val score = FutureOrientationRules.score(answers) ?: return FutureOrientationSaveResult.InvalidAnswers
        val category = FutureOrientationRules.category(score) ?: return FutureOrientationSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return FutureOrientationSaveResult.AlreadyCompleted
        val record = FutureOrientationAssessment(records[key]?.id ?: childId, childId, answers.toList(), score, category, System.currentTimeMillis(), phase)
        records[key] = record
        return FutureOrientationSaveResult.Completed(record)
    }
}

private class InMemoryHeroAsaRepository : HeroAsaRepository {
    private val records = mutableMapOf<Pair<String, AssessmentPhase>, HeroAsaAssessment>()

    override fun get(childId: String, phase: AssessmentPhase): HeroAsaAssessment? = records[childId to phase]
    override fun deleteForChild(childId: String): Boolean { records.keys.removeAll { it.first == childId }; return true }
    override fun mergeImported(records: List<HeroAsaAssessment>): Boolean { records.forEach { this.records[it.childId to it.phase] = it }; return true }

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAsaSaveResult {
        if (childId.isBlank() || !HeroAsaRules.validateAnswers(answers)) return HeroAsaSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return HeroAsaSaveResult.AlreadyCompleted
        val record = HeroAsaAssessment(records[key]?.id ?: childId, childId, answers.toList(), null, null, null, phase)
        records[key] = record
        return HeroAsaSaveResult.SavedDraft(record)
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAsaSaveResult {
        val score = HeroAsaRules.score(answers) ?: return HeroAsaSaveResult.InvalidAnswers
        val category = HeroAsaRules.category(score) ?: return HeroAsaSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return HeroAsaSaveResult.AlreadyCompleted
        val record = HeroAsaAssessment(records[key]?.id ?: childId, childId, answers, score, category, 1L, phase)
        records[key] = record
        return HeroAsaSaveResult.Completed(record)
    }
}

private class InMemoryHeroAksiRepository : HeroAksiRepository {
    private val records = mutableMapOf<Pair<String, AssessmentPhase>, HeroAksiAssessment>()

    override fun get(childId: String, phase: AssessmentPhase): HeroAksiAssessment? = records[childId to phase]
    override fun deleteForChild(childId: String): Boolean { records.keys.removeAll { it.first == childId }; return true }
    override fun mergeImported(records: List<HeroAksiAssessment>): Boolean { records.forEach { this.records[it.childId to it.phase] = it }; return true }

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAksiSaveResult {
        if (childId.isBlank() || !HeroAksiRules.validateAnswers(answers)) return HeroAksiSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return HeroAksiSaveResult.AlreadyCompleted
        val record = HeroAksiAssessment(records[key]?.id ?: childId, childId, answers.toList(), null, null, null, phase)
        records[key] = record
        return HeroAksiSaveResult.SavedDraft(record)
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAksiSaveResult {
        val score = HeroAksiRules.score(answers) ?: return HeroAksiSaveResult.InvalidAnswers
        val category = HeroAksiRules.category(score) ?: return HeroAksiSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return HeroAksiSaveResult.AlreadyCompleted
        val record = HeroAksiAssessment(records[key]?.id ?: childId, childId, answers, score, category, 1L, phase)
        records[key] = record
        return HeroAksiSaveResult.Completed(record)
    }
}

private class InMemoryHeroRasaRepository : HeroRasaRepository {
    private val records = mutableMapOf<Pair<String, AssessmentPhase>, HeroRasaAssessment>()

    override fun get(childId: String, phase: AssessmentPhase): HeroRasaAssessment? = records[childId to phase]
    override fun deleteForChild(childId: String): Boolean { records.keys.removeAll { it.first == childId }; return true }
    override fun mergeImported(records: List<HeroRasaAssessment>): Boolean { records.forEach { this.records[it.childId to it.phase] = it }; return true }

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroRasaSaveResult {
        if (childId.isBlank() || !HeroRasaRules.validateAnswers(answers)) return HeroRasaSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return HeroRasaSaveResult.AlreadyCompleted
        val record = HeroRasaAssessment(records[key]?.id ?: childId, childId, answers.toList(), null, null, null, phase)
        records[key] = record
        return HeroRasaSaveResult.SavedDraft(record)
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroRasaSaveResult {
        val score = HeroRasaRules.score(answers) ?: return HeroRasaSaveResult.InvalidAnswers
        val category = HeroRasaRules.category(score) ?: return HeroRasaSaveResult.InvalidAnswers
        val key = childId to phase
        if (records[key]?.isComplete == true) return HeroRasaSaveResult.AlreadyCompleted
        val record = HeroRasaAssessment(records[key]?.id ?: childId, childId, answers, score, category, 1L, phase)
        records[key] = record
        return HeroRasaSaveResult.Completed(record)
    }
}

private class InMemoryInterventionRepository : InterventionRepository {
    private val records = mutableListOf<InterventionRecord>()

    override fun getForChild(childId: String): List<InterventionRecord> =
        records.filter { it.childId == childId }.sortedByDescending { it.date }
    override fun deleteForChild(childId: String): Boolean { records.removeAll { it.childId == childId }; return true }
    override fun mergeImported(records: List<InterventionRecord>): Boolean { this.records.addAll(records); return true }

    override fun update(record: InterventionRecord): SaveInterventionResult {
        val index = records.indexOfFirst { it.id == record.id && it.childId == record.childId }
        if (index < 0) return SaveInterventionResult.StorageError
        records[index] = record
        return SaveInterventionResult.Saved(record)
    }

    override fun delete(recordId: String): Boolean { records.removeAll { it.id == recordId }; return true }

    override fun add(childId: String, date: LocalDate, programName: String, notes: String): SaveInterventionResult {
        if (!org.heroism.herocompass.intervention.InterventionRules.validProgramName(programName)) return SaveInterventionResult.InvalidProgramName
        if (!org.heroism.herocompass.intervention.InterventionRules.validNotes(notes)) return SaveInterventionResult.InvalidNotes
        if (!org.heroism.herocompass.intervention.InterventionRules.validDate(date)) return SaveInterventionResult.InvalidDate
        return InterventionRecord("id-${records.size}", childId, date, programName.trim(), notes.trim()).let {
            records += it
            SaveInterventionResult.Saved(it)
        }
    }
}
