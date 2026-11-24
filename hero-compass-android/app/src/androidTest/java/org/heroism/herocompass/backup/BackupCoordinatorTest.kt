package org.heroism.herocompass.backup

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.heroism.herocompass.assessment.AssessmentPhase
import org.heroism.herocompass.assessment.EncryptedFutureOrientationRepository
import org.heroism.herocompass.assessment.EncryptedHeroAksiRepository
import org.heroism.herocompass.assessment.EncryptedHeroAsaRepository
import org.heroism.herocompass.assessment.EncryptedHeroRasaRepository
import org.heroism.herocompass.assessment.EncryptedIdentityForeclosureRepository
import org.heroism.herocompass.assessment.IdentityForeclosureCategory
import org.heroism.herocompass.assessment.AssessmentSaveResult
import org.heroism.herocompass.assessment.FutureOrientationSaveResult
import org.heroism.herocompass.assessment.HeroAsaSaveResult
import org.heroism.herocompass.assessment.HeroAksiSaveResult
import org.heroism.herocompass.assessment.HeroRasaSaveResult
import org.heroism.herocompass.children.AddChildResult
import org.heroism.herocompass.children.ChildGender
import org.heroism.herocompass.children.EncryptedChildrenRepository
import org.heroism.herocompass.intervention.EncryptedInterventionRepository
import org.heroism.herocompass.intervention.SaveInterventionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupCoordinatorTest {
    @Test
    fun backupRestoresAllInstrumentsDraftsAndInterventionsIntoAnEmptyProfile() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val sourceContext = isolatedContext(base, "backup-source")
        val targetContext = isolatedContext(base, "backup-target")
        val source = createCoordinator(sourceContext)
        val target = createCoordinator(targetContext)
        val sourceChildren = EncryptedChildrenRepository(sourceContext)
        val child = (sourceChildren.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val identity = EncryptedIdentityForeclosureRepository(sourceContext)
        val future = EncryptedFutureOrientationRepository(sourceContext)
        val asa = EncryptedHeroAsaRepository(sourceContext)
        val aksi = EncryptedHeroAksiRepository(sourceContext)
        val rasa = EncryptedHeroRasaRepository(sourceContext)
        val intervention = EncryptedInterventionRepository(sourceContext)

        assertTrue(identity.complete(child.id, List(15) { 2 }, AssessmentPhase.PRE_TEST) is AssessmentSaveResult.Completed)
        assertTrue(identity.saveDraft(child.id, List(15) { if (it < 4) 1 else null }, AssessmentPhase.POST_TEST) is AssessmentSaveResult.SavedDraft)
        assertTrue(future.complete(child.id, List(15) { 3 }) is FutureOrientationSaveResult.Completed)
        assertTrue(asa.complete(child.id, List(10) { 3 }) is HeroAsaSaveResult.Completed)
        assertTrue(aksi.complete(child.id, List(10) { 2 }) is HeroAksiSaveResult.Completed)
        assertTrue(rasa.complete(child.id, List(10) { 4 }) is HeroRasaSaveResult.Completed)
        assertTrue(intervention.add(child.id, LocalDate.of(2026, 11, 22), "Sesi refleksi", "Catatan singkat") is SaveInterventionResult.Saved)

        val password = "contoh-passphrase-aman".toCharArray()
        val export = source.export(password)
        assertTrue(export is BackupOperationResult.Exported)
        val exported = export as BackupOperationResult.Exported
        assertTrue(exported.bytes.isNotEmpty())

        val restored = target.restore(exported.bytes, password)
        assertTrue(restored is BackupOperationResult.Imported)
        restored as BackupOperationResult.Imported
        assertEquals(1, restored.childCount)
        assertEquals(6, restored.assessmentCount)
        assertEquals(1, restored.interventionCount)
        assertEquals("Ayu Lestari", EncryptedChildrenRepository(targetContext).getAll().single().name)
        assertEquals(30, EncryptedIdentityForeclosureRepository(targetContext).get(child.id, AssessmentPhase.PRE_TEST)?.totalScore)
        assertEquals(IdentityForeclosureCategory.MEDIUM, EncryptedIdentityForeclosureRepository(targetContext).get(child.id, AssessmentPhase.PRE_TEST)?.category)
        assertEquals(4, EncryptedIdentityForeclosureRepository(targetContext).get(child.id, AssessmentPhase.POST_TEST)?.answers?.count { it != null })
        assertEquals(45, EncryptedFutureOrientationRepository(targetContext).get(child.id)?.totalScore)
        assertEquals(30, EncryptedHeroAsaRepository(targetContext).get(child.id)?.totalScore)
        assertEquals(20, EncryptedHeroAksiRepository(targetContext).get(child.id)?.totalScore)
        assertEquals(40, EncryptedHeroRasaRepository(targetContext).get(child.id)?.totalScore)
        assertEquals("Sesi refleksi", EncryptedInterventionRepository(targetContext).getForChild(child.id).single().programName)
        assertEquals(BackupOperationResult.DestinationNotEmpty, target.restore(exported.bytes, password))
        assertEquals(BackupOperationResult.IncorrectPassphraseOrDamagedFile, target.restore(exported.bytes, "password-yang-keliru".toCharArray()))
    }

    private fun createCoordinator(context: Context) = BackupCoordinator(
        EncryptedChildrenRepository(context),
        EncryptedIdentityForeclosureRepository(context),
        EncryptedFutureOrientationRepository(context),
        EncryptedHeroAsaRepository(context),
        EncryptedHeroAksiRepository(context),
        EncryptedHeroRasaRepository(context),
        EncryptedInterventionRepository(context),
    )

    private fun isolatedContext(base: Context, name: String): Context {
        val files = File(base.cacheDir, name).apply {
            deleteRecursively()
            mkdirs()
        }
        return object : ContextWrapper(base) {
            override fun getFilesDir(): File = files
            override fun getApplicationContext(): Context = this
        }
    }
}
