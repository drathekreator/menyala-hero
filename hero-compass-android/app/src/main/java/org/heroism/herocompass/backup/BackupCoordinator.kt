package org.heroism.herocompass.backup

import org.heroism.herocompass.assessment.AssessmentPhase
import org.heroism.herocompass.assessment.FutureOrientationAssessment
import org.heroism.herocompass.assessment.FutureOrientationCategory
import org.heroism.herocompass.assessment.FutureOrientationRepository
import org.heroism.herocompass.assessment.FutureOrientationRules
import org.heroism.herocompass.assessment.HeroAksiAssessment
import org.heroism.herocompass.assessment.HeroAksiCategory
import org.heroism.herocompass.assessment.HeroAksiRepository
import org.heroism.herocompass.assessment.HeroAksiRules
import org.heroism.herocompass.assessment.HeroAsaAssessment
import org.heroism.herocompass.assessment.HeroAsaCategory
import org.heroism.herocompass.assessment.HeroAsaRepository
import org.heroism.herocompass.assessment.HeroAsaRules
import org.heroism.herocompass.assessment.HeroRasaAssessment
import org.heroism.herocompass.assessment.HeroRasaCategory
import org.heroism.herocompass.assessment.HeroRasaRepository
import org.heroism.herocompass.assessment.HeroRasaRules
import org.heroism.herocompass.assessment.IdentityForeclosureAssessment
import org.heroism.herocompass.assessment.IdentityForeclosureCategory
import org.heroism.herocompass.assessment.IdentityForeclosureRepository
import org.heroism.herocompass.assessment.IdentityForeclosureRules
import org.heroism.herocompass.children.ChildGender
import org.heroism.herocompass.children.ChildRecord
import org.heroism.herocompass.children.ChildrenRepository
import org.heroism.herocompass.children.ChildRules
import org.heroism.herocompass.intervention.InterventionRecord
import org.heroism.herocompass.intervention.InterventionRepository
import org.heroism.herocompass.intervention.InterventionRules
import java.time.LocalDate

sealed interface BackupOperationResult {
    data class Exported(val bytes: ByteArray, val childCount: Int, val assessmentCount: Int, val interventionCount: Int) : BackupOperationResult
    data class Imported(val childCount: Int, val assessmentCount: Int, val interventionCount: Int) : BackupOperationResult
    data object InvalidPassphrase : BackupOperationResult
    data object InvalidBackup : BackupOperationResult
    data object IncorrectPassphraseOrDamagedFile : BackupOperationResult
    data object DestinationNotEmpty : BackupOperationResult
    data object StorageError : BackupOperationResult
}

/** Coordinates a portable snapshot across the app's independently encrypted repositories. */
class BackupCoordinator(
    private val children: ChildrenRepository,
    private val identity: IdentityForeclosureRepository,
    private val future: FutureOrientationRepository,
    private val asa: HeroAsaRepository,
    private val aksi: HeroAksiRepository,
    private val rasa: HeroRasaRepository,
    private val interventions: InterventionRepository,
) {
    fun export(passphrase: CharArray): BackupOperationResult = runCatching {
        val childRecords = children.getAll()
        val assessments = buildList {
            childRecords.forEach { child ->
                AssessmentPhase.entries.forEach { phase ->
                    identity.get(child.id, phase)?.let { add(it.toBackup("identity")) }
                    future.get(child.id, phase)?.let { add(it.toBackup("future")) }
                    asa.get(child.id, phase)?.let { add(it.toBackup("asa")) }
                    aksi.get(child.id, phase)?.let { add(it.toBackup("aksi")) }
                    rasa.get(child.id, phase)?.let { add(it.toBackup("rasa")) }
                }
            }
        }
        val followUps = childRecords.flatMap { interventions.getForChild(it.id) }
        val snapshot = PortableBackupSnapshot(
            createdAtEpochMillis = System.currentTimeMillis(),
            children = childRecords.map { it.toBackup() },
            assessments = assessments,
            interventions = followUps.map { it.toBackup() },
        )
        when (val encrypted = PortableBackupCrypto.encrypt(snapshot, passphrase)) {
            is BackupCryptoResult.Success -> BackupOperationResult.Exported(encrypted.bytes, childRecords.size, assessments.size, followUps.size)
            BackupCryptoResult.InvalidPassphrase -> BackupOperationResult.InvalidPassphrase
            else -> BackupOperationResult.StorageError
        }
    }.getOrDefault(BackupOperationResult.StorageError)

    fun restore(bytes: ByteArray, passphrase: CharArray): BackupOperationResult {
        if (!PortableBackupCrypto.validPassphrase(passphrase)) return BackupOperationResult.InvalidPassphrase
        val decrypted = when (val result = PortableBackupCrypto.decrypt(bytes, passphrase)) {
            is BackupCryptoResult.Success -> result.bytes
            BackupCryptoResult.AuthenticationFailed -> return BackupOperationResult.IncorrectPassphraseOrDamagedFile
            BackupCryptoResult.InvalidPassphrase -> return BackupOperationResult.InvalidPassphrase
            BackupCryptoResult.InvalidFile -> return BackupOperationResult.InvalidBackup
        }
        if (runCatching { children.getAll().isNotEmpty() }.getOrDefault(true)) return BackupOperationResult.DestinationNotEmpty
        return runCatching {
            val snapshot = PortableBackupCrypto.decodeSnapshot(decrypted)
            val childrenToImport = snapshot.children.map { child ->
                val gender = child.genderCode?.let(ChildGender::fromDatasetCode)
                require(child.genderCode == null || gender != null)
                val record = ChildRecord(
                    id = child.id,
                    name = child.name,
                    birthDate = LocalDate.parse(child.birthDate),
                    gender = gender,
                    involvedInScavenging = child.involvedInScavenging,
                )
                require(ChildRules.validName(record.name) && ChildRules.validBirthDate(record.birthDate))
                record
            }
            val childIds = childrenToImport.map { it.id }.toSet()
            require(snapshot.assessments.all { it.childId in childIds })
            require(snapshot.interventions.all { it.childId in childIds })
            val identityRows = snapshot.assessments.filter { it.instrument == "identity" }.map { it.toIdentity() }
            val futureRows = snapshot.assessments.filter { it.instrument == "future" }.map { it.toFuture() }
            val asaRows = snapshot.assessments.filter { it.instrument == "asa" }.map { it.toAsa() }
            val aksiRows = snapshot.assessments.filter { it.instrument == "aksi" }.map { it.toAksi() }
            val rasaRows = snapshot.assessments.filter { it.instrument == "rasa" }.map { it.toRasa() }
            require(snapshot.assessments.all { it.instrument in KNOWN_INSTRUMENTS })
            val followUps = snapshot.interventions.map { record ->
                val date = LocalDate.parse(record.date)
                require(InterventionRules.validDate(date) && InterventionRules.validProgramName(record.programName) && InterventionRules.validNotes(record.notes))
                InterventionRecord(record.id, record.childId, date, record.programName.trim(), record.notes.trim())
            }
            if (!children.mergeImported(childrenToImport)) return BackupOperationResult.StorageError
            if (!identity.mergeImported(identityRows)) return BackupOperationResult.StorageError
            if (!future.mergeImported(futureRows)) return BackupOperationResult.StorageError
            if (!asa.mergeImported(asaRows)) return BackupOperationResult.StorageError
            if (!aksi.mergeImported(aksiRows)) return BackupOperationResult.StorageError
            if (!rasa.mergeImported(rasaRows)) return BackupOperationResult.StorageError
            if (!interventions.mergeImported(followUps)) return BackupOperationResult.StorageError
            BackupOperationResult.Imported(childrenToImport.size, snapshot.assessments.size, followUps.size)
        }.getOrElse { error ->
            if (error is IllegalArgumentException) BackupOperationResult.InvalidBackup else BackupOperationResult.StorageError
        }
    }

    private fun ChildRecord.toBackup() = BackupChild(id, name, birthDate.toString(), gender?.datasetCode, involvedInScavenging)

    private fun IdentityForeclosureAssessment.toBackup(instrument: String) =
        BackupAssessment(instrument, id, childId, phase.name, answers, totalScore, completedAtEpochMillis)

    private fun FutureOrientationAssessment.toBackup(instrument: String) =
        BackupAssessment(instrument, id, childId, phase.name, answers, totalScore, completedAtEpochMillis)

    private fun HeroAsaAssessment.toBackup(instrument: String) =
        BackupAssessment(instrument, id, childId, phase.name, answers, totalScore, completedAtEpochMillis)

    private fun HeroAksiAssessment.toBackup(instrument: String) =
        BackupAssessment(instrument, id, childId, phase.name, answers, totalScore, completedAtEpochMillis)

    private fun HeroRasaAssessment.toBackup(instrument: String) =
        BackupAssessment(instrument, id, childId, phase.name, answers, totalScore, completedAtEpochMillis)

    private fun InterventionRecord.toBackup() = BackupIntervention(id, childId, date.toString(), programName, notes)

    private fun BackupAssessment.validatedPhase(): AssessmentPhase =
        AssessmentPhase.valueOf(phase).also { require(id.isNotBlank() && childId.isNotBlank()) }

    private fun BackupAssessment.toIdentity(): IdentityForeclosureAssessment {
        val score = IdentityForeclosureRules.score(answers)
        require(IdentityForeclosureRules.validateAnswers(answers))
        validateScore(score)
        return IdentityForeclosureAssessment(id, childId, answers, totalScore, totalScore?.let(IdentityForeclosureCategory::fromScore), completedAtEpochMillis, validatedPhase())
    }

    private fun BackupAssessment.toFuture(): FutureOrientationAssessment {
        val score = FutureOrientationRules.score(answers)
        require(FutureOrientationRules.validateAnswers(answers))
        validateScore(score)
        return FutureOrientationAssessment(id, childId, answers, totalScore, totalScore?.let(FutureOrientationCategory::fromScore), completedAtEpochMillis, validatedPhase())
    }

    private fun BackupAssessment.toAsa(): HeroAsaAssessment {
        val score = HeroAsaRules.score(answers)
        require(HeroAsaRules.validateAnswers(answers))
        validateScore(score)
        return HeroAsaAssessment(id, childId, answers, totalScore, totalScore?.let(HeroAsaCategory::fromScore), completedAtEpochMillis, validatedPhase())
    }

    private fun BackupAssessment.toAksi(): HeroAksiAssessment {
        val score = HeroAksiRules.score(answers)
        require(HeroAksiRules.validateAnswers(answers))
        validateScore(score)
        return HeroAksiAssessment(id, childId, answers, totalScore, totalScore?.let(HeroAksiCategory::fromScore), completedAtEpochMillis, validatedPhase())
    }

    private fun BackupAssessment.toRasa(): HeroRasaAssessment {
        val score = HeroRasaRules.score(answers)
        require(HeroRasaRules.validateAnswers(answers))
        validateScore(score)
        return HeroRasaAssessment(id, childId, answers, totalScore, totalScore?.let(HeroRasaCategory::fromScore), completedAtEpochMillis, validatedPhase())
    }

    private fun BackupAssessment.validateScore(calculated: Int?) {
        if (totalScore == null) require(calculated == null && completedAtEpochMillis == null)
        else require(calculated == totalScore && completedAtEpochMillis != null)
    }

    private companion object {
        val KNOWN_INSTRUMENTS = setOf("identity", "future", "asa", "aksi", "rasa")
    }
}
