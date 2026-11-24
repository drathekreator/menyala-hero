package org.heroism.herocompass.children

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.heroism.herocompass.assessment.AssessmentPhase
import org.heroism.herocompass.assessment.EncryptedFutureOrientationRepository
import org.heroism.herocompass.assessment.EncryptedHeroAksiRepository
import org.heroism.herocompass.assessment.EncryptedHeroAsaRepository
import org.heroism.herocompass.assessment.EncryptedHeroRasaRepository
import org.heroism.herocompass.assessment.EncryptedIdentityForeclosureRepository
import org.heroism.herocompass.intervention.EncryptedInterventionRepository
import org.heroism.herocompass.intervention.SaveInterventionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ChildDataDeletionCoordinatorTest {
    @Test
    fun deletesChildAndAllEncryptedRecordsButKeepsOtherChildren() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val children = EncryptedChildrenRepository(context)
        val identity = EncryptedIdentityForeclosureRepository(context)
        val future = EncryptedFutureOrientationRepository(context)
        val asa = EncryptedHeroAsaRepository(context)
        val aksi = EncryptedHeroAksiRepository(context)
        val rasa = EncryptedHeroRasaRepository(context)
        val interventions = EncryptedInterventionRepository(context)
        val target = (children.add("Anak Hapus Pilot", LocalDate.of(2015, 4, 12), ChildGender.Girl, false) as AddChildResult.Added).child
        val keeper = (children.add("Anak Tetap Pilot", LocalDate.of(2014, 7, 20), ChildGender.Boy, true) as AddChildResult.Added).child
        assertTrue(identity.complete(target.id, List(15) { 2 }) is org.heroism.herocompass.assessment.AssessmentSaveResult.Completed)
        assertTrue(future.complete(target.id, List(15) { 3 }) is org.heroism.herocompass.assessment.FutureOrientationSaveResult.Completed)
        assertTrue(asa.complete(target.id, List(10) { 3 }) is org.heroism.herocompass.assessment.HeroAsaSaveResult.Completed)
        assertTrue(aksi.complete(target.id, List(10) { 2 }) is org.heroism.herocompass.assessment.HeroAksiSaveResult.Completed)
        assertTrue(rasa.complete(target.id, List(10) { 4 }) is org.heroism.herocompass.assessment.HeroRasaSaveResult.Completed)
        assertTrue(interventions.add(target.id, LocalDate.now(), "HERO Asa", "Catatan uji") is SaveInterventionResult.Saved)
        assertTrue(identity.complete(keeper.id, List(15) { 2 }, AssessmentPhase.PRE_TEST) is org.heroism.herocompass.assessment.AssessmentSaveResult.Completed)

        val coordinator = ChildDataDeletionCoordinator(children, identity, future, asa, aksi, rasa, interventions)
        assertTrue(coordinator.delete(target.id))

        assertTrue(children.getAll().none { it.id == target.id })
        assertTrue(children.getAll().any { it.id == keeper.id })
        assertEquals(null, identity.get(target.id))
        assertEquals(null, future.get(target.id))
        assertEquals(null, asa.get(target.id))
        assertEquals(null, aksi.get(target.id))
        assertEquals(null, rasa.get(target.id))
        assertTrue(interventions.getForChild(target.id).isEmpty())
        assertEquals(30, identity.get(keeper.id)?.totalScore)
        assertTrue(coordinator.delete(keeper.id))
    }
}
