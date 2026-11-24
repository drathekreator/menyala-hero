package org.heroism.herocompass.children

import org.heroism.herocompass.assessment.AssessmentPhase
import org.heroism.herocompass.assessment.FutureOrientationRepository
import org.heroism.herocompass.assessment.HeroAksiRepository
import org.heroism.herocompass.assessment.HeroAsaRepository
import org.heroism.herocompass.assessment.HeroRasaRepository
import org.heroism.herocompass.assessment.IdentityForeclosureRepository
import org.heroism.herocompass.intervention.InterventionRepository

/** Removes a child and every dependent local record; restores earlier repositories if a later write fails. */
class ChildDataDeletionCoordinator(
    private val children: ChildrenRepository,
    private val identity: IdentityForeclosureRepository,
    private val future: FutureOrientationRepository,
    private val asa: HeroAsaRepository,
    private val aksi: HeroAksiRepository,
    private val rasa: HeroRasaRepository,
    private val interventions: InterventionRepository,
) {
    fun delete(childId: String): Boolean = runCatching {
        require(childId.isNotBlank())
        val child = children.getAll().firstOrNull { it.id == childId } ?: return true
        val phases = AssessmentPhase.entries
        val identityRows = phases.mapNotNull { identity.get(childId, it) }
        val futureRows = phases.mapNotNull { future.get(childId, it) }
        val asaRows = phases.mapNotNull { asa.get(childId, it) }
        val aksiRows = phases.mapNotNull { aksi.get(childId, it) }
        val rasaRows = phases.mapNotNull { rasa.get(childId, it) }
        val followUps = interventions.getForChild(childId)
        val restores = mutableListOf<() -> Boolean>()

        fun rollback(): Boolean = restores.asReversed().map { restore ->
            runCatching { restore() }.getOrDefault(false)
        }.all { it }

        fun deleteStore(remove: () -> Boolean, restore: () -> Boolean): Boolean {
            if (!remove()) {
                rollback()
                return false
            }
            restores += restore
            return true
        }

        if (!deleteStore({ identity.deleteForChild(childId) }, { identityRows.isEmpty() || identity.mergeImported(identityRows) })) return false
        if (!deleteStore({ future.deleteForChild(childId) }, { futureRows.isEmpty() || future.mergeImported(futureRows) })) return false
        if (!deleteStore({ asa.deleteForChild(childId) }, { asaRows.isEmpty() || asa.mergeImported(asaRows) })) return false
        if (!deleteStore({ aksi.deleteForChild(childId) }, { aksiRows.isEmpty() || aksi.mergeImported(aksiRows) })) return false
        if (!deleteStore({ rasa.deleteForChild(childId) }, { rasaRows.isEmpty() || rasa.mergeImported(rasaRows) })) return false
        if (!deleteStore({ interventions.deleteForChild(childId) }, { followUps.isEmpty() || interventions.mergeImported(followUps) })) return false
        if (!children.delete(childId)) {
            rollback()
            return false
        }
        true
    }.getOrDefault(false)
}
