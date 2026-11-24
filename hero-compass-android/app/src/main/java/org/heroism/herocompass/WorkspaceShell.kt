package org.heroism.herocompass

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.heroism.herocompass.children.AddChildResult
import org.heroism.herocompass.children.ChildRecord
import org.heroism.herocompass.children.ChildGender
import org.heroism.herocompass.children.ChildrenRepository
import org.heroism.herocompass.children.BirthDatePicker
import org.heroism.herocompass.children.ChildDataDeletionCoordinator
import org.heroism.herocompass.intervention.InterventionRepository
import org.heroism.herocompass.intervention.InterventionRecord
import org.heroism.herocompass.intervention.SaveInterventionResult
import org.heroism.herocompass.backup.BackupCoordinator
import org.heroism.herocompass.backup.BackupOperationResult
import org.heroism.herocompass.backup.PortableBackupCrypto
import org.heroism.herocompass.auth.MentorAuthRepository
import org.heroism.herocompass.assessment.AssessmentSaveResult
import org.heroism.herocompass.assessment.IdentityForeclosureAssessment
import org.heroism.herocompass.assessment.IdentityForeclosureInstrument
import org.heroism.herocompass.assessment.IdentityForeclosureRepository
import org.heroism.herocompass.assessment.IdentityForeclosureCategory
import org.heroism.herocompass.assessment.AssessmentPhase
import org.heroism.herocompass.assessment.IdentityForeclosureOcr
import org.heroism.herocompass.assessment.IdentityOcrFailure
import org.heroism.herocompass.assessment.IdentityOcrOutcome
import org.heroism.herocompass.assessment.FutureOrientationAssessment
import org.heroism.herocompass.assessment.FutureOrientationCategory
import org.heroism.herocompass.assessment.FutureOrientationInstrument
import org.heroism.herocompass.assessment.FutureOrientationRepository
import org.heroism.herocompass.assessment.FutureOrientationRules
import org.heroism.herocompass.assessment.FutureOrientationSaveResult
import org.heroism.herocompass.ml.BaselineModelInput
import org.heroism.herocompass.ml.DomainRiskEstimate
import org.heroism.herocompass.ml.ExperimentalBaselineModel
import org.heroism.herocompass.ml.ExperimentalBaselineForecast
import java.time.Instant
import org.heroism.herocompass.assessment.HeroAsaAssessment
import org.heroism.herocompass.assessment.HeroAsaCategory
import org.heroism.herocompass.assessment.HeroAsaInstrument
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
import org.heroism.herocompass.assessment.HeroRasaSaveResult
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider

private val WorkspaceBlue = Color(0xFF004772)
private val WorkspaceSky = Color(0xFF5E9FCA)
private val WorkspaceRed = Color(0xFFB72929)
private val WorkspaceInk = Color(0xFF343434)
private val WorkspaceMuted = Color(0xFF5F626C)
private val WorkspaceCanvas = Color(0xFFF5F7FA)
private val WorkspacePattern = Color(0xFF0A5A87)
private val IndonesianLocale = Locale.forLanguageTag("id-ID")
private const val ChildPageSize = 5
private val WorkspacePoppins = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.poppins_regular, FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.poppins_medium, FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.poppins_semibold, FontWeight.SemiBold),
)

@Composable
internal fun SetStatusBarIconStyle(lightBackground: Boolean) {
    val context = LocalContext.current
    SideEffect {
        var current = context
        while (current !is Activity && current is ContextWrapper) current = current.baseContext
        (current as? Activity)?.window?.let { window ->
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = lightBackground
        }
    }
}

private enum class WorkspaceTab(val title: String) {
    Home("Home"), Search("Cari"), Children("Anak"), Guide("Panduan"), Profile("Profil")
}

private enum class AssessmentInstrument { IdentityForeclosure, FutureOrientation, HeroAsa, HeroAksi, HeroRasa }

@Composable
internal fun WorkspaceShell(
    mentorName: String,
    childrenRepository: ChildrenRepository,
    birthDatePicker: BirthDatePicker,
    assessmentRepository: IdentityForeclosureRepository,
    futureOrientationRepository: FutureOrientationRepository? = null,
    heroAsaRepository: HeroAsaRepository? = null,
    heroAksiRepository: HeroAksiRepository? = null,
    heroRasaRepository: HeroRasaRepository? = null,
    interventionRepository: InterventionRepository? = null,
    backupCoordinator: BackupCoordinator? = null,
    mentorAuthRepository: MentorAuthRepository? = null,
    biometricEnabled: Boolean = false,
    biometricFeedback: String = "",
    onEnableBiometric: () -> Unit = {},
    onDisableBiometric: () -> Unit = {},
    onLock: () -> Unit,
) {
    SetStatusBarIconStyle(lightBackground = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = WorkspaceTab.entries
    var storageIssue by remember { mutableStateOf(false) }
    var activeAssessmentChildId by remember { mutableStateOf<String?>(null) }
    var assessmentFormVisible by remember { mutableStateOf(false) }
    var activeAssessmentPhase by remember { mutableStateOf(AssessmentPhase.PRE_TEST) }
    var activeAssessmentInstrument by remember { mutableStateOf(AssessmentInstrument.IdentityForeclosure) }
    var activeChildDetailId by remember { mutableStateOf<String?>(null) }
    var assessmentReturnToChildDetail by remember { mutableStateOf(false) }
    var activeAssessmentAnswers by remember { mutableStateOf<List<Int?>?>(null) }
    var assessmentNavigationError by remember { mutableStateOf<String?>(null) }
    var ocrImportVersion by remember { mutableIntStateOf(0) }
    var avatarVersion by remember { mutableIntStateOf(0) }
    var pendingOcr by remember { mutableStateOf<org.heroism.herocompass.assessment.IdentityOcrResult?>(null) }
    var answersBeforeOcr by remember { mutableStateOf<List<Int?>?>(null) }
    var ocrReviewConfirmed by remember { mutableStateOf(false) }
    var ocrProcessing by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val appContext = LocalContext.current
    LaunchedEffect(appContext) {
        File(appContext.cacheDir, "assessment-scans").listFiles()?.forEach { it.delete() }
    }
    val coroutineScope = rememberCoroutineScope()
    fun saveActiveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): Boolean = when (activeAssessmentInstrument) {
        AssessmentInstrument.IdentityForeclosure -> when (runCatching { assessmentRepository.saveDraft(childId, answers, phase) }
            .getOrElse { AssessmentSaveResult.StorageError }) {
            is AssessmentSaveResult.SavedDraft, is AssessmentSaveResult.Completed, AssessmentSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.FutureOrientation -> when (runCatching {
            futureOrientationRepository?.saveDraft(childId, answers, phase) ?: FutureOrientationSaveResult.StorageError
        }.getOrElse { FutureOrientationSaveResult.StorageError }) {
            is FutureOrientationSaveResult.SavedDraft, is FutureOrientationSaveResult.Completed, FutureOrientationSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.HeroAsa -> when (runCatching {
            heroAsaRepository?.saveDraft(childId, answers, phase) ?: HeroAsaSaveResult.StorageError
        }.getOrElse { HeroAsaSaveResult.StorageError }) {
            is HeroAsaSaveResult.SavedDraft, is HeroAsaSaveResult.Completed, HeroAsaSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.HeroAksi -> when (runCatching {
            heroAksiRepository?.saveDraft(childId, answers, phase) ?: HeroAksiSaveResult.StorageError
        }.getOrElse { HeroAksiSaveResult.StorageError }) {
            is HeroAksiSaveResult.SavedDraft, is HeroAksiSaveResult.Completed, HeroAksiSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.HeroRasa -> when (runCatching {
            heroRasaRepository?.saveDraft(childId, answers, phase) ?: HeroRasaSaveResult.StorageError
        }.getOrElse { HeroRasaSaveResult.StorageError }) {
            is HeroRasaSaveResult.SavedDraft, is HeroRasaSaveResult.Completed, HeroRasaSaveResult.AlreadyCompleted -> true
            else -> false
        }
    }
    fun activeItemCount(): Int = when (activeAssessmentInstrument) {
        AssessmentInstrument.IdentityForeclosure -> IdentityForeclosureInstrument.items.size
        AssessmentInstrument.FutureOrientation -> FutureOrientationInstrument.items.size
        AssessmentInstrument.HeroAsa -> HeroAsaInstrument.items.size
        AssessmentInstrument.HeroAksi -> HeroAksiInstrument.items.size
        AssessmentInstrument.HeroRasa -> HeroRasaInstrument.items.size
    }
    fun activeOcrDefinition(): Pair<String, List<String>> = when (activeAssessmentInstrument) {
        AssessmentInstrument.IdentityForeclosure -> "Identity Foreclosure (U-MICS)" to IdentityForeclosureInstrument.items
        AssessmentInstrument.FutureOrientation -> "Orientasi Masa Depan" to FutureOrientationInstrument.items
        AssessmentInstrument.HeroAsa -> "HERO Asa · Forethought" to HeroAsaInstrument.items
        AssessmentInstrument.HeroAksi -> "HERO Aksi · Performance" to HeroAksiInstrument.items
        AssessmentInstrument.HeroRasa -> "HERO Rasa · Reflection" to HeroRasaInstrument.items
    }
    fun completeActiveAssessment(childId: String, answers: List<Int?>, phase: AssessmentPhase): Boolean = when (activeAssessmentInstrument) {
        AssessmentInstrument.IdentityForeclosure -> when (runCatching { assessmentRepository.complete(childId, answers, phase) }.getOrNull()) {
            is AssessmentSaveResult.Completed, AssessmentSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.FutureOrientation -> when (runCatching { futureOrientationRepository?.complete(childId, answers, phase) }.getOrNull()) {
            is FutureOrientationSaveResult.Completed, FutureOrientationSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.HeroAsa -> when (runCatching { heroAsaRepository?.complete(childId, answers, phase) }.getOrNull()) {
            is HeroAsaSaveResult.Completed, HeroAsaSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.HeroAksi -> when (runCatching { heroAksiRepository?.complete(childId, answers, phase) }.getOrNull()) {
            is HeroAksiSaveResult.Completed, HeroAksiSaveResult.AlreadyCompleted -> true
            else -> false
        }
        AssessmentInstrument.HeroRasa -> when (runCatching { heroRasaRepository?.complete(childId, answers, phase) }.getOrNull()) {
            is HeroRasaSaveResult.Completed, HeroRasaSaveResult.AlreadyCompleted -> true
            else -> false
        }
    }
    fun removeTemporaryCameraScan() {
        pendingCameraFile?.delete()
        pendingCameraFile = null
        pendingCameraUri = null
    }
    fun processOcr(uri: Uri, cameraFile: File? = null) {
        pendingCameraFile = cameraFile
        if (activeAssessmentChildId != null) {
            coroutineScope.launch {
                ocrProcessing = true
                ocrError = null
                val (instrumentName, itemStatements) = activeOcrDefinition()
                val outcome = withContext(Dispatchers.IO) {
                    IdentityForeclosureOcr.read(appContext, uri, instrumentName, itemStatements)
                }
                when (outcome) {
                    is IdentityOcrOutcome.Read -> {
                        pendingOcr?.previewBitmap?.recycle()
                        answersBeforeOcr = activeAssessmentAnswers.orEmpty()
                        pendingOcr = outcome.result
                        activeAssessmentAnswers = outcome.result.answers
                        ocrReviewConfirmed = false
                        ocrImportVersion++
                        assessmentFormVisible = true
                    }
                    is IdentityOcrOutcome.Failed -> {
                        cameraFile?.delete()
                        if (cameraFile != null) {
                            pendingCameraFile = null
                            pendingCameraUri = null
                        }
                        ocrError = when (outcome.reason) {
                            IdentityOcrFailure.NotIdentityForeclosureForm -> "Judul formulir belum dikenali. Pilih foto atau PDF ${activeOcrDefinition().first}."
                            is IdentityOcrFailure.UnsupportedInstrument -> "Formulir terdeteksi sebagai ${outcome.reason.name}, tetapi instrumen yang dipilih adalah ${activeOcrDefinition().first}."
                            IdentityOcrFailure.ResponseGridNotFound -> "Kisi jawaban tidak terbaca. Ambil foto satu halaman penuh, rata, dan dengan cahaya cukup. Jawaban tidak diubah."
                            is IdentityOcrFailure.ReadError -> "Foto atau PDF belum dapat dibaca. Pilih berkas lain atau coba lagi."
                        }
                    }
                }
                ocrProcessing = false
            }
        }
    }
    val scanPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) processOcr(uri)
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        if (captured && uri != null) processOcr(uri, file)
        else {
            removeTemporaryCameraScan()
            ocrError = "Foto tidak diambil. Kamu bisa mencoba lagi atau memilih PDF/foto."
        }
    }
    fun launchCameraScan() {
        runCatching {
            val directory = File(appContext.cacheDir, "assessment-scans").apply { mkdirs() }
            val file = File(directory, "scan-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
            pendingCameraFile = file
            pendingCameraUri = uri
            cameraCapture.launch(uri)
        }.onFailure {
            removeTemporaryCameraScan()
            ocrError = "Kamera tidak dapat dibuka di perangkat ini. Pilih foto atau PDF sebagai gantinya."
        }
    }
    fun navigateToTab(index: Int) {
        val childId = activeAssessmentChildId
        if (childId != null) {
            if (ocrProcessing) {
                assessmentNavigationError = "Tunggu proses pemindaian selesai sebelum berpindah halaman."
                return
            }
            if (pendingOcr != null && !ocrReviewConfirmed) {
                assessmentNavigationError = "Periksa dan konfirmasi semua jawaban hasil OCR, atau buang hasil scan sebelum berpindah halaman."
                return
            }
            val answers = activeAssessmentAnswers.orEmpty()
            if (answers.any { it != null } && !saveActiveDraft(childId, answers, activeAssessmentPhase)) {
                    assessmentNavigationError = "Jawaban belum tersimpan. Periksa penyimpanan lalu coba lagi."
                    return
            }
            activeAssessmentChildId = null
            assessmentFormVisible = false
            activeAssessmentAnswers = null
            assessmentNavigationError = null
            pendingOcr?.previewBitmap?.recycle()
            pendingOcr = null
            removeTemporaryCameraScan()
            answersBeforeOcr = null
            ocrReviewConfirmed = false
            ocrError = null
        }
        activeChildDetailId = null
        selectedTab = index
    }
    BackHandler(enabled = activeChildDetailId == null && activeAssessmentChildId == null && selectedTab != WorkspaceTab.Home.ordinal) {
        navigateToTab(WorkspaceTab.Home.ordinal)
    }
    fun startAssessment(
        childId: String,
        phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
        returnToDetail: Boolean,
        instrument: AssessmentInstrument = AssessmentInstrument.IdentityForeclosure,
    ) {
        activeAssessmentChildId = childId
        assessmentFormVisible = false
        activeAssessmentPhase = phase
        activeAssessmentInstrument = instrument
        assessmentReturnToChildDetail = returnToDetail
        activeAssessmentAnswers = when (instrument) {
            AssessmentInstrument.IdentityForeclosure -> runCatching { assessmentRepository.get(childId, phase)?.answers }.getOrNull()
            AssessmentInstrument.FutureOrientation -> runCatching { futureOrientationRepository?.get(childId, phase)?.answers }.getOrNull()
            AssessmentInstrument.HeroAsa -> runCatching { heroAsaRepository?.get(childId, phase)?.answers }.getOrNull()
            AssessmentInstrument.HeroAksi -> runCatching { heroAksiRepository?.get(childId, phase)?.answers }.getOrNull()
            AssessmentInstrument.HeroRasa -> runCatching { heroRasaRepository?.get(childId, phase)?.answers }.getOrNull()
        } ?: List(activeItemCount()) { null }
        assessmentNavigationError = null
        pendingOcr?.previewBitmap?.recycle()
        pendingOcr = null
        answersBeforeOcr = null
        ocrReviewConfirmed = false
        ocrProcessing = false
        ocrError = null
    }
    val children = remember(childrenRepository) {
        mutableStateListOf<ChildRecord>().apply {
            runCatching { addAll(childrenRepository.getAll()) }.onFailure { storageIssue = true }
        }
    }

    Column(
        Modifier.fillMaxSize().background(WorkspaceCanvas).statusBarsPadding(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (activeAssessmentChildId != null && !assessmentFormVisible) {
                val child = children.firstOrNull { it.id == activeAssessmentChildId }
                if (child == null) {
                    activeAssessmentChildId = null
                } else {
                    AssessmentInputMethodPage(
                        child = child,
                        phase = activeAssessmentPhase,
                        instrumentTitle = if (activeAssessmentInstrument == AssessmentInstrument.IdentityForeclosure) {
                            IdentityForeclosureInstrument.TITLE
                        } else when (activeAssessmentInstrument) {
                            AssessmentInstrument.IdentityForeclosure -> IdentityForeclosureInstrument.TITLE
                            AssessmentInstrument.FutureOrientation -> FutureOrientationInstrument.TITLE
                            AssessmentInstrument.HeroAsa -> HeroAsaInstrument.TITLE
                            AssessmentInstrument.HeroAksi -> HeroAksiInstrument.TITLE
                            AssessmentInstrument.HeroRasa -> HeroRasaInstrument.TITLE
                        },
                        ocrAvailable = true,
                        error = ocrError,
                        processing = ocrProcessing,
                        navigationError = assessmentNavigationError,
                        onBack = {
                            if (ocrProcessing) {
                                assessmentNavigationError = "Tunggu proses pemindaian selesai sebelum kembali."
                                return@AssessmentInputMethodPage
                            }
                            val answers = activeAssessmentAnswers.orEmpty()
                            val saved = pendingOcr == null || ocrReviewConfirmed
                            if (!saved) {
                                assessmentNavigationError = "Periksa dan konfirmasi hasil OCR atau buang scan sebelum kembali."
                            } else if (answers.any { it != null }) {
                                if (saveActiveDraft(child.id, answers, activeAssessmentPhase)) {
                                    activeAssessmentChildId = null
                                    activeAssessmentAnswers = null
                                    assessmentNavigationError = null
                                    pendingOcr?.previewBitmap?.recycle()
                                    pendingOcr = null
                                    removeTemporaryCameraScan()
                                    ocrReviewConfirmed = false
                                    ocrError = null
                                } else assessmentNavigationError = "Draf belum tersimpan. Coba lagi sebelum kembali."
                            } else {
                                activeAssessmentChildId = null
                                activeAssessmentAnswers = null
                                assessmentNavigationError = null
                                removeTemporaryCameraScan()
                                ocrError = null
                            }
                        },
                        onManual = { assessmentFormVisible = true; ocrError = null; assessmentNavigationError = null },
                        onChooseFile = {
                            ocrError = null
                            assessmentNavigationError = null
                            scanPhotoPicker.launch(arrayOf("image/*", "application/pdf"))
                        },
                        onScanNow = {
                            ocrError = null
                            assessmentNavigationError = null
                            launchCameraScan()
                        },
                    )
                }
            } else if (activeAssessmentChildId != null) {
                val child = children.firstOrNull { it.id == activeAssessmentChildId }
                if (child == null) {
                    activeAssessmentChildId = null
                } else {
                    if (pendingOcr != null && activeAssessmentInstrument != AssessmentInstrument.IdentityForeclosure) AssessmentOcrReviewPage(
                        child = child,
                        instrumentTitle = activeOcrDefinition().first,
                        phase = activeAssessmentPhase,
                        itemStatements = activeOcrDefinition().second,
                        importedOcr = pendingOcr!!,
                        initialAnswers = activeAssessmentAnswers.orEmpty(),
                        confirmed = ocrReviewConfirmed,
                        onAnswersChanged = { activeAssessmentAnswers = it },
                        onConfirmed = { ocrReviewConfirmed = it },
                        onSaveDraft = { answers ->
                            if (saveActiveDraft(child.id, answers, activeAssessmentPhase)) {
                                pendingOcr?.previewBitmap?.recycle()
                                pendingOcr = null
                                removeTemporaryCameraScan()
                                answersBeforeOcr = null
                                ocrReviewConfirmed = false
                                true
                            } else false
                        },
                        onComplete = { answers ->
                            if (completeActiveAssessment(child.id, answers, activeAssessmentPhase)) {
                                pendingOcr?.previewBitmap?.recycle()
                                pendingOcr = null
                                removeTemporaryCameraScan()
                                answersBeforeOcr = null
                                ocrReviewConfirmed = false
                                true
                            } else false
                        },
                        onDiscard = {
                            activeAssessmentAnswers = answersBeforeOcr
                            pendingOcr?.previewBitmap?.recycle()
                            pendingOcr = null
                            removeTemporaryCameraScan()
                            answersBeforeOcr = null
                            ocrReviewConfirmed = false
                            ocrImportVersion++
                        },
                    ) else if (activeAssessmentInstrument == AssessmentInstrument.IdentityForeclosure) IdentityForeclosureAssessmentPage(
                        child = child,
                        repository = assessmentRepository,
                        phase = activeAssessmentPhase,
                        initialAnswers = activeAssessmentAnswers.orEmpty(),
                        importedOcr = pendingOcr,
                        ocrImportVersion = ocrImportVersion,
                        ocrReviewConfirmed = ocrReviewConfirmed,
                        navigationError = assessmentNavigationError,
                        onAnswersChanged = {
                            activeAssessmentAnswers = it
                            assessmentNavigationError = null
                        },
                        onOcrReviewConfirmed = { ocrReviewConfirmed = it },
                        onOcrSaved = {
                            pendingOcr?.previewBitmap?.recycle()
                            pendingOcr = null
                            removeTemporaryCameraScan()
                            answersBeforeOcr = null
                            ocrReviewConfirmed = false
                            ocrError = null
                        },
                        onDiscardOcr = {
                            activeAssessmentAnswers = answersBeforeOcr
                            pendingOcr?.previewBitmap?.recycle()
                            pendingOcr = null
                            removeTemporaryCameraScan()
                            answersBeforeOcr = null
                            ocrReviewConfirmed = false
                            ocrError = null
                            ocrImportVersion++
                        },
                        onBack = {
                            if (assessmentReturnToChildDetail) {
                                if (pendingOcr != null && !ocrReviewConfirmed) {
                                    assessmentNavigationError = "Periksa dan konfirmasi jawaban hasil OCR, atau buang hasil scan sebelum kembali."
                                } else {
                                    val saved = pendingOcr == null || runCatching {
                                        assessmentRepository.saveDraft(child.id, activeAssessmentAnswers.orEmpty(), activeAssessmentPhase)
                                    }.getOrNull().let { result ->
                                        result is AssessmentSaveResult.SavedDraft ||
                                            result is AssessmentSaveResult.Completed ||
                                            result == AssessmentSaveResult.AlreadyCompleted
                                    }
                                    if (!saved) {
                                        assessmentNavigationError = "Draf OCR belum tersimpan. Simpan sebagai draf atau coba lagi sebelum kembali."
                                    } else {
                                        assessmentFormVisible = false
                                        assessmentNavigationError = null
                                        pendingOcr?.previewBitmap?.recycle()
                                        pendingOcr = null
                                        removeTemporaryCameraScan()
                                        answersBeforeOcr = null
                                        ocrReviewConfirmed = false
                                    }
                                }
                            } else navigateToTab(WorkspaceTab.Children.ordinal)
                        },
                    ) else if (activeAssessmentInstrument == AssessmentInstrument.FutureOrientation) FutureOrientationAssessmentPage(
                        child = child,
                        repository = futureOrientationRepository,
                        phase = activeAssessmentPhase,
                        initialAnswers = activeAssessmentAnswers.orEmpty(),
                        navigationError = assessmentNavigationError,
                        onAnswersChanged = {
                            activeAssessmentAnswers = it
                            assessmentNavigationError = null
                        },
                        onBack = {
                            val answers = activeAssessmentAnswers.orEmpty()
                            if (answers.any { it != null } && !saveActiveDraft(child.id, answers, activeAssessmentPhase)) {
                                assessmentNavigationError = "Draf belum tersimpan. Coba lagi sebelum kembali."
                            } else {
                                assessmentFormVisible = false
                                assessmentNavigationError = null
                            }
                        },
                    ) else if (activeAssessmentInstrument == AssessmentInstrument.HeroAsa) HeroAsaAssessmentPage(
                        child = child,
                        repository = heroAsaRepository,
                        phase = activeAssessmentPhase,
                        initialAnswers = activeAssessmentAnswers.orEmpty(),
                        navigationError = assessmentNavigationError,
                        onAnswersChanged = {
                            activeAssessmentAnswers = it
                            assessmentNavigationError = null
                        },
                        onBack = {
                            val answers = activeAssessmentAnswers.orEmpty()
                            if (answers.any { it != null } && !saveActiveDraft(child.id, answers, activeAssessmentPhase)) {
                                assessmentNavigationError = "Draf belum tersimpan. Coba lagi sebelum kembali."
                            } else {
                                assessmentFormVisible = false
                                assessmentNavigationError = null
                            }
                        },
                    ) else if (activeAssessmentInstrument == AssessmentInstrument.HeroAksi) HeroAksiAssessmentPage(
                        child = child,
                        repository = heroAksiRepository,
                        phase = activeAssessmentPhase,
                        initialAnswers = activeAssessmentAnswers.orEmpty(),
                        navigationError = assessmentNavigationError,
                        onAnswersChanged = {
                            activeAssessmentAnswers = it
                            assessmentNavigationError = null
                        },
                        onBack = {
                            val answers = activeAssessmentAnswers.orEmpty()
                            if (answers.any { it != null } && !saveActiveDraft(child.id, answers, activeAssessmentPhase)) {
                                assessmentNavigationError = "Draf belum tersimpan. Coba lagi sebelum kembali."
                            } else {
                                assessmentFormVisible = false
                                assessmentNavigationError = null
                            }
                        },
                    ) else HeroRasaAssessmentPage(
                        child = child,
                        repository = heroRasaRepository,
                        phase = activeAssessmentPhase,
                        initialAnswers = activeAssessmentAnswers.orEmpty(),
                        navigationError = assessmentNavigationError,
                        onAnswersChanged = {
                            activeAssessmentAnswers = it
                            assessmentNavigationError = null
                        },
                        onBack = {
                            val answers = activeAssessmentAnswers.orEmpty()
                            if (answers.any { it != null } && !saveActiveDraft(child.id, answers, activeAssessmentPhase)) {
                                assessmentNavigationError = "Draf belum tersimpan. Coba lagi sebelum kembali."
                            } else {
                                assessmentFormVisible = false
                                assessmentNavigationError = null
                            }
                        },
                    )
                }
            } else if (activeChildDetailId != null) {
                val child = children.firstOrNull { it.id == activeChildDetailId }
                if (child == null) {
                    activeChildDetailId = null
                } else {
                    ChildDetailPage(
                        child = child,
                        assessmentRepository = assessmentRepository,
                        futureOrientationRepository = futureOrientationRepository,
                        heroAsaRepository = heroAsaRepository,
                        heroAksiRepository = heroAksiRepository,
                        heroRasaRepository = heroRasaRepository,
                        birthDatePicker = birthDatePicker,
                        interventionRepository = interventionRepository,
                        onBack = { activeChildDetailId = null },
                        onStartAssessment = { phase -> startAssessment(child.id, phase, returnToDetail = true) },
                        onStartFutureAssessment = { phase ->
                            startAssessment(
                                child.id,
                                phase,
                                returnToDetail = true,
                                instrument = AssessmentInstrument.FutureOrientation,
                            )
                        },
                        onStartHeroAsaAssessment = { phase ->
                            startAssessment(
                                child.id,
                                phase,
                                returnToDetail = true,
                                instrument = AssessmentInstrument.HeroAsa,
                            )
                        },
                        onStartHeroAksiAssessment = { phase ->
                            startAssessment(
                                child.id,
                                phase,
                                returnToDetail = true,
                                instrument = AssessmentInstrument.HeroAksi,
                            )
                        },
                        onStartHeroRasaAssessment = { phase ->
                            startAssessment(
                                child.id,
                                phase,
                                returnToDetail = true,
                                instrument = AssessmentInstrument.HeroRasa,
                            )
                        },
                        onUpdateChild = { updated ->
                            if (childrenRepository.update(updated)) {
                                val index = children.indexOfFirst { it.id == updated.id }
                                if (index >= 0) children[index] = updated
                                true
                            } else false
                        },
                        onDeleteChild = { childId ->
                            val futureRepository = futureOrientationRepository
                            val asaRepository = heroAsaRepository
                            val aksiRepository = heroAksiRepository
                            val rasaRepository = heroRasaRepository
                            val followUpRepository = interventionRepository
                            if (futureRepository == null || asaRepository == null || aksiRepository == null || rasaRepository == null || followUpRepository == null) {
                                false
                            } else {
                                val deleted = ChildDataDeletionCoordinator(
                                    childrenRepository,
                                    assessmentRepository,
                                    futureRepository,
                                    asaRepository,
                                    aksiRepository,
                                    rasaRepository,
                                    followUpRepository,
                                ).delete(childId)
                                if (deleted) {
                                    children.removeAll { it.id == childId }
                                    activeChildDetailId = null
                                }
                                deleted
                            }
                        },
                    )
                }
            } else {
                when (tabs[selectedTab]) {
                    WorkspaceTab.Home -> HomePage(
                        mentorName = mentorName,
                        children = children,
                        assessmentRepository = assessmentRepository,
                        futureOrientationRepository = futureOrientationRepository,
                        interventionRepository = interventionRepository,
                        avatarFile = mentorAuthRepository?.avatarFile(),
                        avatarVersion = avatarVersion,
                        onOpenChildren = { selectedTab = WorkspaceTab.Children.ordinal },
                    )
                    WorkspaceTab.Search -> SearchPage(
                        children = children,
                        assessmentRepository = assessmentRepository,
                        onOpenChild = { activeChildDetailId = it },
                    )
                    WorkspaceTab.Children -> ChildrenPage(
                        children = children,
                        storageIssue = storageIssue,
                        birthDatePicker = birthDatePicker,
                        assessmentRepository = assessmentRepository,
                        onOpenChild = { activeChildDetailId = it },
                        onAdd = { name, date, gender, involved ->
                            when (val result = childrenRepository.add(name, date, gender, involved)) {
                                is AddChildResult.Added -> { children.add(result.child); true }
                                AddChildResult.InvalidName, AddChildResult.InvalidBirthDate -> false
                                AddChildResult.StorageError -> { storageIssue = true; false }
                            }
                        },
                    )
                    WorkspaceTab.Guide -> AssessmentGuidePage()
                    WorkspaceTab.Profile -> ProfilePage(
                        name = mentorName,
                        backupCoordinator = backupCoordinator,
                        avatarFile = mentorAuthRepository?.avatarFile(),
                        avatarVersion = avatarVersion,
                        biometricEnabled = biometricEnabled,
                        biometricFeedback = biometricFeedback,
                        onAvatarSelected = { uri ->
                            val saved = mentorAuthRepository?.saveAvatar(uri) == true
                            if (saved) avatarVersion++
                            saved
                        },
                        onEnableBiometric = onEnableBiometric,
                        onDisableBiometric = onDisableBiometric,
                        onLock = onLock,
                    ) {
                        runCatching { childrenRepository.getAll() }.onSuccess { restored ->
                            children.clear()
                            children.addAll(restored)
                            activeChildDetailId = null
                            selectedTab = WorkspaceTab.Children.ordinal
                        }
                    }
                }
            }
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (selected) WorkspaceRed else Color.Transparent)
                            .selectable(
                                selected = selected,
                                onClick = { navigateToTab(index) },
                                role = Role.Tab,
                            )
                            .testTag("workspace-tab-${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        WorkspaceIcon(tab, selected, Modifier.size(23.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tab.title,
                            color = if (selected) Color.White else WorkspaceMuted,
                            fontFamily = WorkspacePoppins,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    mentorName: String,
    children: List<ChildRecord>,
    assessmentRepository: IdentityForeclosureRepository,
    futureOrientationRepository: FutureOrientationRepository?,
    interventionRepository: InterventionRepository?,
    avatarFile: File?,
    avatarVersion: Int,
    onOpenChildren: () -> Unit,
) {
    val assessmentReads = children.map { child -> runCatching { assessmentRepository.get(child.id) } }
    val completedPretests = assessmentReads.count { it.getOrNull()?.isComplete == true }
    val futurePretestReads = children.map { child ->
        runCatching { futureOrientationRepository?.get(child.id, AssessmentPhase.PRE_TEST) }
    }
    val completedBaselineChildren = children.indices.count { index ->
        assessmentReads[index].getOrNull()?.isComplete == true && futurePretestReads[index].getOrNull()?.isComplete == true
    }
    val postAssessmentReads = children.map { child -> runCatching { assessmentRepository.get(child.id, AssessmentPhase.POST_TEST) } }
    val futurePosttestReads = children.map { child ->
        runCatching { futureOrientationRepository?.get(child.id, AssessmentPhase.POST_TEST) }
    }
    val startedPosttest = children.indices.any { index ->
        postAssessmentReads[index].getOrNull() != null || futurePosttestReads[index].getOrNull() != null
    }
    val interventionReads = children.map { child ->
        runCatching { interventionRepository?.getForChild(child.id).orEmpty() }
    }
    val childrenWithInterventions = interventionReads.count { it.getOrNull()?.isNotEmpty() == true }
    val completedPosttests = children.indices.count { index ->
        postAssessmentReads[index].getOrNull()?.isComplete == true && futurePosttestReads[index].getOrNull()?.isComplete == true
    }
    val currentProgramStage = when {
        futureOrientationRepository == null -> null
        assessmentReads.any { it.isFailure } || futurePretestReads.any { it.isFailure } ||
            postAssessmentReads.any { it.isFailure } || futurePosttestReads.any { it.isFailure } -> null
        else -> currentProgramStage(children.size, completedBaselineChildren, startedPosttest)
    }
    val stageDetail = when {
        children.isEmpty() -> "Tambahkan anak untuk memulai pra-program."
        currentProgramStage == null -> "Data tahap belum dapat dibaca."
        currentProgramStage == ProgramStage.PRE_PROGRAM ->
            "Pre-test Identitas + Orientasi: $completedBaselineChildren/${children.size} anak lengkap"
        currentProgramStage == ProgramStage.INTERVENTION ->
            if (interventionReads.any { it.isFailure }) "Catatan kegiatan belum dapat dibaca."
            else "Catatan kegiatan: $childrenWithInterventions/${children.size} anak"
        else -> "Post-test Identitas + Orientasi: $completedPosttests/${children.size} anak lengkap"
    }
    val completedCycles = children.indices.count { index ->
        assessmentReads[index].getOrNull()?.isComplete == true && postAssessmentReads[index].getOrNull()?.isComplete == true
    }
    val activeCycles = if (assessmentReads.any { it.isFailure } || postAssessmentReads.any { it.isFailure }) null else
        (completedPretests - completedCycles).coerceAtLeast(0)
    val progressPercent = when {
        assessmentReads.any { it.isFailure } -> null
        else -> pretestProgressPercent(children.size, completedPretests)
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(WorkspaceBlue).padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HomeMentorAvatar(mentorName, avatarFile, avatarVersion)
            Column(Modifier.weight(1f)) {
                Text("Hai, $mentorName", color = Color.White, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp)
            }
            Text("OFFLINE", color = Color.White, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, modifier = Modifier.clip(CircleShape).background(WorkspacePattern).padding(horizontal = 10.dp, vertical = 7.dp))
        }
        Column(
            Modifier.fillMaxSize().background(WorkspaceCanvas).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 20.dp),
        ) {
        Spacer(Modifier.height(4.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WorkspaceBlue), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Dampingi setiap langkah.", color = Color.White, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 27.sp)
                Spacer(Modifier.height(5.dp))
                Text("Catat hasil tes dan tindak lanjut anak.", color = Color.White.copy(alpha = .86f), fontFamily = WorkspacePoppins, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenChildren, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WorkspaceBlue)) {
                    Text("Lihat anak", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ProgramStagesCard(currentProgramStage, stageDetail)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard(children.size.toString(), "Anak didampingi", Modifier.weight(1f), Color(0xFFEAF3F9), WorkspaceBlue)
            SummaryCard(activeCycles?.toString() ?: "—", "Identitas berjalan", Modifier.weight(1f), Color(0xFFFFF0EC), WorkspaceRed, "home-active-cycle-count")
        }
        Spacer(Modifier.height(14.dp))
        PretestProgressCard(
            totalChildren = children.size,
            completedChildren = completedPretests,
            progressPercent = progressPercent,
        )
        }
    }
}

@Composable
internal fun HomeMentorAvatar(name: String, avatarFile: File?, avatarVersion: Int) {
    val avatar = remember(avatarFile?.absolutePath, avatarVersion) {
        avatarFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.White).testTag("home-mentor-avatar"),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar != null) {
            Image(
                bitmap = avatar.asImageBitmap(),
                contentDescription = "Foto profil pendamping $name",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(name.trim().firstOrNull()?.uppercase() ?: "H", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
    }
}

internal enum class ProgramStage {
    PRE_PROGRAM,
    INTERVENTION,
    POST_PROGRAM,
}

internal fun currentProgramStage(
    totalChildren: Int,
    baselineCompleteChildren: Int,
    hasStartedPostTest: Boolean,
): ProgramStage {
    if (totalChildren <= 0 || baselineCompleteChildren < totalChildren) return ProgramStage.PRE_PROGRAM
    return if (hasStartedPostTest) ProgramStage.POST_PROGRAM else ProgramStage.INTERVENTION
}

@Composable
private fun ProgramStagesCard(currentStage: ProgramStage?, detail: String) {
    val stages = listOf(
        ProgramStage.PRE_PROGRAM to "Pra-program",
        ProgramStage.INTERVENTION to "Intervensi",
        ProgramStage.POST_PROGRAM to "Pasca-program",
    )
    val activeIndex = currentStage?.let { stage -> stages.indexOfFirst { it.first == stage } } ?: -1
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.testTag("home-program-stages")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text("Tahapan program", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                stages.forEachIndexed { index, _ ->
                    val color = when {
                        index == activeIndex -> WorkspaceRed
                        activeIndex >= 0 && index < activeIndex -> WorkspaceBlue
                        else -> WorkspaceMuted.copy(alpha = .22f)
                    }
                    ProgramStageMarker(
                        (index + 1).toString(),
                        color,
                        active = index == activeIndex,
                        completed = activeIndex >= 0 && index < activeIndex,
                        modifier = Modifier
                            .testTag("home-program-stage-marker-${stageAutomationKey(stages[index].first)}")
                            .semantics {
                                contentDescription = when {
                                    index == activeIndex -> "Tahap ${stages[index].second}, sedang berjalan"
                                    activeIndex >= 0 && index < activeIndex -> "Tahap ${stages[index].second}, selesai"
                                    else -> "Tahap ${stages[index].second}, belum dimulai"
                                }
                            },
                    )
                    if (index < stages.lastIndex) {
                        val connectorColor = if (activeIndex > index) WorkspaceBlue.copy(alpha = .55f) else WorkspaceMuted.copy(alpha = .18f)
                        ProgramStageConnector(connectorColor)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                stages.forEachIndexed { index, stage ->
                    val color = when {
                        index == activeIndex -> WorkspaceRed
                        activeIndex >= 0 && index < activeIndex -> WorkspaceBlue
                        else -> WorkspaceMuted
                    }
                    ProgramStageLabel(stage.second, Alignment.CenterHorizontally, Modifier.weight(1f), color)
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                detail,
                color = WorkspaceMuted,
                fontFamily = WorkspacePoppins,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                modifier = Modifier.testTag("home-program-stage-detail"),
            )
        }
    }
}

@Composable
private fun stageAutomationKey(stage: ProgramStage): String = when (stage) {
    ProgramStage.PRE_PROGRAM -> "pre-program"
    ProgramStage.INTERVENTION -> "intervention"
    ProgramStage.POST_PROGRAM -> "post-program"
}

@Composable
private fun RowScope.ProgramStageMarker(
    number: String,
    color: Color,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(if (active) 30.dp else 28.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (completed) "✓" else number, color = Color.White, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    }
}

@Composable
private fun RowScope.ProgramStageConnector(color: Color) {
    Box(Modifier.weight(1f).padding(horizontal = 5.dp).height(3.dp).clip(CircleShape).background(color))
}

@Composable
private fun RowScope.ProgramStageLabel(title: String, alignment: Alignment.Horizontal, modifier: Modifier, color: Color) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(title, color = color, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun AssessmentGuidePage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(WorkspaceCanvas).padding(horizontal = 20.dp, vertical = 22.dp).testTag("assessment-guide-page")) {
        PageHeading("Panduan program", "Rubrik skor dan kegiatan intervensi")
        Spacer(Modifier.height(14.dp))
        ProgramCycleCard()
        Spacer(Modifier.height(14.dp))
        Text("RUBRIK PENILAIAN", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .7.sp)
        Spacer(Modifier.height(8.dp))
        GuideScoreCard("Identity Foreclosure", "15 pernyataan", "15–29 Tinggi", "30–44 Sedang", "45–60 Rendah", "Skor lebih rendah berada pada kategori kecenderungan foreclosure lebih tinggi.")
        Spacer(Modifier.height(9.dp))
        GuideScoreCard("Orientasi Masa Depan", "15 pernyataan", "15–29 Rendah", "30–44 Sedang", "45–60 Tinggi", "Sasaran post-test pada panduan adalah kategori tinggi.")
        Spacer(Modifier.height(9.dp))
        GuideScoreCard("HERO Asa · Forethought", "10 pernyataan", "10–19 Rendah", "20–29 Sedang", "30–40 Tinggi", "Bagian dari rangkaian kegiatan HERO.")
        Spacer(Modifier.height(9.dp))
        GuideScoreCard("HERO Aksi · Performance", "10 pernyataan", "10–19 Rendah", "20–29 Sedang", "30–40 Tinggi", "Bagian dari rangkaian kegiatan HERO.")
        Spacer(Modifier.height(9.dp))
        GuideScoreCard("HERO Rasa · Reflection", "10 pernyataan", "10–19 Rendah", "20–29 Sedang", "30–40 Tinggi", "Bagian dari rangkaian kegiatan HERO.")
        Spacer(Modifier.height(12.dp))
        Text("KEGIATAN INTERVENSI", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .7.sp)
        Spacer(Modifier.height(4.dp))
        Text("Urutan kegiatan program HERO", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))
        ProgramCatalogCard("HERO Asa", "Kenali potensi dan kemungkinan diri", "✦", Color(0xFFB47A00), listOf("Kartu Asa", "Gambar diri sekarang dan masa depan", "Kelompok bercerita", "Penguatan peran orang tua"), "guide-program-card-asa")
        Spacer(Modifier.height(8.dp))
        ProgramCatalogCard("HERO Aksi", "Jelajahi identitas dan profesi masa depan", "↗", WorkspaceBlue, listOf("Video profesi dan bermain peran", "Proyek Misi Masa Depan", "Kartu Misi dan jelajah peran di KidZania", "Gantungan kunci profesi dari bahan bekas"), "guide-program-card-aksi")
        Spacer(Modifier.height(8.dp))
        ProgramCatalogCard("HERO Rasa", "Refleksi diri dan makna cita-cita", "♡", Color(0xFFB14E68), listOf("Bandingkan gambar diri", "Berbagi cerita", "Kartu Refleksi", "Surat untuk masa depan"), "guide-program-card-rasa")
        Spacer(Modifier.height(8.dp))
        ProgramCatalogCard("HERO Karsa", "Rencanakan masa depan dan teguhkan komitmen", "✓", Color(0xFF28724C), listOf("Jurnal HERO: Peta Hidup", "Kartu Komitmen", "Deklarasi Karsa", "Post-test IF dan masa depan", "Penutupan dan ToT mitra"), "guide-program-card-karsa")
        Spacer(Modifier.height(12.dp))
        Text("Skor dijumlahkan setelah semua butir terisi. Kategori tes yang berbeda tidak dibandingkan langsung. Karsa tercantum sebagai kegiatan program; rubrik skornya tidak tersedia.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ProgramCycleCard() {
    Card(Modifier.fillMaxWidth().testTag("guide-program-cycle"), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("ALUR PROGRAM", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .7.sp)
            Spacer(Modifier.height(8.dp))
            ProgramCycleRow("01", "Pra-program", "ToT awal · pre-test Identitas dan Orientasi Masa Depan", WorkspaceBlue)
            ProgramCycleRow("02", "Intervensi", "HERO Asa → Aksi → Rasa → Karsa", WorkspaceRed)
            ProgramCycleRow("03", "Pasca-program", "Post-test Identitas + Orientasi Masa Depan · evaluasi · intervensi lanjutan", Color(0xFF28724C))
        }
    }
}

@Composable
private fun ProgramCycleRow(number: String, title: String, detail: String, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Text(number, color = accent, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 8.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            Text(detail, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 8.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun ProgramCatalogCard(
    name: String,
    focus: String,
    symbol: String,
    accent: Color,
    activities: List<String>,
    testTag: String,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag(testTag), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text(symbol, color = accent, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text(focus, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            activities.chunked(2).forEach { rowActivities ->
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowActivities.forEach { activity ->
                        Text(activity, modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(WorkspaceCanvas).padding(horizontal = 8.dp, vertical = 7.dp), color = WorkspaceInk, fontFamily = WorkspacePoppins, fontSize = 8.sp, lineHeight = 12.sp)
                    }
                    if (rowActivities.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GuideScoreCard(title: String, itemCount: String, low: String, middle: String, high: String, note: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(itemCount, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(low, middle, high).forEachIndexed { index, category ->
                    Text(category, color = if (index == 1) WorkspaceBlue else WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 8.sp, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(WorkspaceCanvas).padding(horizontal = 5.dp, vertical = 7.dp), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(note, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
        }
    }
}

internal fun pretestProgressPercent(totalChildren: Int, completedChildren: Int): Int? {
    if (totalChildren <= 0) return null
    return (completedChildren.coerceIn(0, totalChildren) * 100f / totalChildren).roundToInt().coerceIn(0, 100)
}

@Composable
private fun PretestProgressCard(totalChildren: Int, completedChildren: Int, progressPercent: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().height(132.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WorkspaceRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.hero_pattern),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(.15f),
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.child_mascot),
                    contentDescription = "Maskot Aksa",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxHeight().weight(.72f),
                )
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.Center) {
                    Text(
                        "PRE-TEST IDENTITAS",
                        color = Color.White.copy(alpha = .82f),
                        fontFamily = WorkspacePoppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        letterSpacing = .7.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when {
                            totalChildren == 0 -> "Belum ada anak"
                            progressPercent == null -> "Data belum tersedia"
                            else -> "$completedChildren dari $totalChildren anak"
                        },
                        color = Color.White,
                        fontFamily = WorkspacePoppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                    )
                    val detail = when {
                        totalChildren == 0 -> "Tambahkan anak untuk mulai mencatat."
                        progressPercent == null -> "Catatan asesmen tidak dapat dibaca."
                        else -> null
                    }
                    detail?.let {
                        Text(it, color = Color.White.copy(alpha = .86f), fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 13.sp, maxLines = 2)
                    }
                }
                PretestProgressRing(progressPercent, Modifier.size(64.dp))
            }
        }
    }
}

@Composable
private fun PretestProgressRing(progressPercent: Int?, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = 7.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = .8f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            progressPercent?.takeIf { it > 0 }?.let { percent ->
                drawArc(
                    color = Color(0xFF57D86B),
                    startAngle = -90f,
                    sweepAngle = 360f * percent.coerceIn(0, 100) / 100f,
                    useCenter = false,
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            progressPercent?.let { "$it%" } ?: "—",
            color = Color.White,
            fontFamily = WorkspacePoppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier, tint: Color, accent: Color, valueTag: String? = null) {
    Card(modifier, shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(39.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Text(value, color = accent, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = if (valueTag == null) Modifier else Modifier.testTag(valueTag))
            }
            Spacer(Modifier.size(9.dp))
            Text(label, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun WorkflowStep(number: String, title: String, subtitle: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(35.dp).clip(CircleShape).background(color.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
            Text(number, color = color, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
        }
        Spacer(Modifier.size(11.dp))
        Column {
            Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Text(subtitle, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
        }
    }
}

@Composable
private fun WorkflowConnector() {
    Box(Modifier.padding(start = 17.dp).height(14.dp).size(width = 2.dp, height = 14.dp).background(Color(0xFFE6E9EF)))
}

@Composable
private fun SearchPage(
    children: List<ChildRecord>,
    assessmentRepository: IdentityForeclosureRepository,
    onOpenChild: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val matches = children.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val pageCount = ((matches.size + ChildPageSize - 1) / ChildPageSize).coerceAtLeast(1)
    val visibleMatches = matches.drop(currentPage * ChildPageSize).take(ChildPageSize)
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeading("Cari anak", "")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                currentPage = 0
                coroutineScope.launch { scrollState.animateScrollTo(0) }
            },
            modifier = Modifier.fillMaxWidth().testTag("search-child-query"),
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            placeholder = { Text("Cari nama anak", fontFamily = WorkspacePoppins, fontSize = 12.sp, color = WorkspaceMuted) },
            leadingIcon = { WorkspaceIcon(WorkspaceTab.Search, false) },
        )
        Spacer(Modifier.height(20.dp))
        if (matches.isEmpty()) {
            EmptyStateCard(
                if (children.isEmpty()) "Belum ada anak terdaftar" else "Anak tidak ditemukan",
                if (children.isEmpty()) "Tambahkan anak untuk mulai mencatat perkembangannya." else "Coba ubah kata pencarian.",
                "",
            )
        } else {
            Text(
                "${matches.size} anak ditemukan",
                color = WorkspaceMuted,
                fontFamily = WorkspacePoppins,
                fontSize = 11.sp,
                modifier = Modifier.testTag("search-result-count"),
            )
            Spacer(Modifier.height(10.dp))
            visibleMatches.forEach { child ->
                ChildCard(child, assessmentRepository, onClick = { onOpenChild(child.id) })
            }
            PaginationControls("search", currentPage, pageCount) { page ->
                currentPage = page
                coroutineScope.launch { scrollState.animateScrollTo(0) }
            }
        }
    }
}

@Composable
private fun ChildDetailPage(
    child: ChildRecord,
    assessmentRepository: IdentityForeclosureRepository,
    futureOrientationRepository: FutureOrientationRepository?,
    heroAsaRepository: HeroAsaRepository?,
    heroAksiRepository: HeroAksiRepository?,
    heroRasaRepository: HeroRasaRepository?,
    birthDatePicker: BirthDatePicker,
    interventionRepository: InterventionRepository?,
    onBack: () -> Unit,
    onStartAssessment: (AssessmentPhase) -> Unit,
    onStartFutureAssessment: (AssessmentPhase) -> Unit,
    onStartHeroAsaAssessment: (AssessmentPhase) -> Unit,
    onStartHeroAksiAssessment: (AssessmentPhase) -> Unit,
    onStartHeroRasaAssessment: (AssessmentPhase) -> Unit,
    onUpdateChild: (ChildRecord) -> Boolean,
    onDeleteChild: (String) -> Boolean,
) {
    val interventionLoad = remember(child.id, interventionRepository) {
        runCatching { interventionRepository?.getForChild(child.id).orEmpty() }
    }
    val interventions = remember(child.id, interventionRepository) {
        mutableStateListOf<InterventionRecord>().apply { addAll(interventionLoad.getOrDefault(emptyList())) }
    }
    var addingIntervention by remember(child.id) { mutableStateOf(false) }
    var interventionDate by remember(child.id) { mutableStateOf(LocalDate.now()) }
    var interventionProgram by remember(child.id) { mutableStateOf("") }
    var interventionNotes by remember(child.id) { mutableStateOf("") }
    var interventionFeedback by remember(child.id) { mutableStateOf("") }
    var editingInterventionId by remember(child.id) { mutableStateOf<String?>(null) }
    var pendingInterventionDelete by remember(child.id) { mutableStateOf<InterventionRecord?>(null) }
    var showDeleteConfirmation by remember(child.id) { mutableStateOf(false) }
    var deleteFeedback by remember(child.id) { mutableStateOf("") }
    var showEditChild by remember(child.id) { mutableStateOf(false) }
    var editChildName by remember(child.id) { mutableStateOf(child.name) }
    var editChildBirthDate by remember(child.id) { mutableStateOf(child.birthDate) }
    var editChildGender by remember(child.id) { mutableStateOf(child.gender) }
    var editChildScavenging by remember(child.id) { mutableStateOf(child.involvedInScavenging) }
    var editChildError by remember(child.id) { mutableStateOf("") }
    val assessmentLoad = remember(child.id, assessmentRepository) { runCatching { assessmentRepository.get(child.id) } }
    val assessment = assessmentLoad.getOrNull()
    val postAssessmentLoad = remember(child.id, assessmentRepository) {
        runCatching { assessmentRepository.get(child.id, AssessmentPhase.POST_TEST) }
    }
    val postAssessment = postAssessmentLoad.getOrNull()
    val futurePreLoad = remember(child.id, futureOrientationRepository) {
        runCatching { futureOrientationRepository?.get(child.id, AssessmentPhase.PRE_TEST) }
    }
    val futurePre = futurePreLoad.getOrNull()
    val futurePostLoad = remember(child.id, futureOrientationRepository) {
        runCatching { futureOrientationRepository?.get(child.id, AssessmentPhase.POST_TEST) }
    }
    val futurePost = futurePostLoad.getOrNull()
    val asaPreLoad = remember(child.id, heroAsaRepository) {
        runCatching { heroAsaRepository?.get(child.id, AssessmentPhase.PRE_TEST) }
    }
    val asaPre = asaPreLoad.getOrNull()
    val asaPostLoad = remember(child.id, heroAsaRepository) {
        runCatching { heroAsaRepository?.get(child.id, AssessmentPhase.POST_TEST) }
    }
    val asaPost = asaPostLoad.getOrNull()
    val aksiPreLoad = remember(child.id, heroAksiRepository) {
        runCatching { heroAksiRepository?.get(child.id, AssessmentPhase.PRE_TEST) }
    }
    val aksiPre = aksiPreLoad.getOrNull()
    val aksiPostLoad = remember(child.id, heroAksiRepository) {
        runCatching { heroAksiRepository?.get(child.id, AssessmentPhase.POST_TEST) }
    }
    val aksiPost = aksiPostLoad.getOrNull()
    val rasaPreLoad = remember(child.id, heroRasaRepository) {
        runCatching { heroRasaRepository?.get(child.id, AssessmentPhase.PRE_TEST) }
    }
    val rasaPre = rasaPreLoad.getOrNull()
    val rasaPostLoad = remember(child.id, heroRasaRepository) {
        runCatching { heroRasaRepository?.get(child.id, AssessmentPhase.POST_TEST) }
    }
    val rasaPost = rasaPostLoad.getOrNull()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().testTag("child-detail-page-${child.id}")) {
        NavigationBackButton(onClick = onBack, testTag = "child-detail-back")
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        PageHeading("Statistik anak", child.name)
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("PROFIL ANAK", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, letterSpacing = .7.sp)
                Spacer(Modifier.height(8.dp))
                Text("Lahir ${child.birthDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", IndonesianLocale))}", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontSize = 11.sp)
                Text(
                    "${child.gender?.displayName ?: "Jenis kelamin belum diisi"} · Memulung: ${child.involvedInScavenging?.let { if (it) "Ya" else "Tidak" } ?: "belum diisi"}",
                    color = WorkspaceMuted,
                    fontFamily = WorkspacePoppins,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            editChildName = child.name
                            editChildBirthDate = child.birthDate
                            editChildGender = child.gender
                            editChildScavenging = child.involvedInScavenging
                            editChildError = ""
                            showEditChild = true
                        },
                        modifier = Modifier.testTag("child-detail-edit"),
                    ) { Text("Ubah data", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp) }
                    TextButton(
                        onClick = { showDeleteConfirmation = true; deleteFeedback = "" },
                        modifier = Modifier.testTag("child-detail-delete"),
                    ) { Text("Hapus data…", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp) }
                }
                if (deleteFeedback.isNotBlank()) {
                    Text(deleteFeedback, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 9.sp, modifier = Modifier.testTag("child-delete-error"))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BaselineSummaryCard(assessment, assessmentLoad.isFailure, futurePre, futurePreLoad.isFailure)
        Spacer(Modifier.height(9.dp))
        val context = LocalContext.current
        val baselineModel = remember(context) { runCatching { ExperimentalBaselineModel.load(context) } }
        when {
            assessment?.isComplete != true -> DetailNoticeCard(
                "Prediksi awal belum tersedia",
                "Lengkapi dua pre-test awal untuk melihat fokus pendampingan.",
                WorkspaceBlue,
                modifier = Modifier.testTag("experimental-ml-not-ready"),
            )
            futurePre?.isComplete != true -> DetailNoticeCard(
                "Prediksi awal belum tersedia",
                "Lengkapi pre-test Orientasi Masa Depan agar hasil dapat dihitung.",
                WorkspaceBlue,
                modifier = Modifier.testTag("experimental-ml-not-ready"),
            )
            child.gender == null || child.involvedInScavenging == null -> DetailNoticeCard(
                "Data awal belum lengkap",
                "Lengkapi jenis kelamin dan status keterlibatan memulung pada profil anak.",
                WorkspaceBlue,
                modifier = Modifier.testTag("experimental-ml-not-ready"),
            )
            baselineModel.isFailure -> DetailNoticeCard(
                "Model belum dapat dibuka",
                "Berkas model prototipe tidak tersedia di aplikasi ini.",
                WorkspaceRed,
                modifier = Modifier.testTag("experimental-ml-not-ready"),
            )
            else -> {
                val baselineEpoch = minOfNotNull(assessment.completedAtEpochMillis, futurePre.completedAtEpochMillis)
                val baselineDate = baselineEpoch?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                val ageAtBaseline = baselineDate?.let { ChronoUnit.YEARS.between(child.birthDate, it).toInt() } ?: 0
                val forecast = baselineModel.getOrNull()?.predict(
                    BaselineModelInput(
                        ageAtPretest = ageAtBaseline,
                        identityForeclosurePretest = assessment.totalScore!!,
                        futureOrientationPretest = futurePre.totalScore!!,
                        genderCode = child.gender.datasetCode,
                        scavengingInvolved = child.involvedInScavenging,
                    ),
                )
                if (forecast == null) DetailNoticeCard(
                    "Prediksi awal belum tersedia",
                    "Periksa kembali tanggal lahir dan skor pre-test anak.",
                    WorkspaceRed,
                    modifier = Modifier.testTag("experimental-ml-not-ready"),
                ) else ExperimentalForecastCard(forecast)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (assessmentLoad.isFailure) {
            DetailNoticeCard("Statistik belum dapat dibuka", "Catatan asesmen anak ini gagal dibaca. Periksa penyimpanan perangkat lalu coba lagi.", WorkspaceRed)
        } else if (assessment?.isComplete == true) {
            val score = assessment.totalScore!!
            when {
                postAssessmentLoad.isFailure -> DetailNoticeCard("Post-test belum dapat dibaca", "Periksa penyimpanan perangkat sebelum melanjutkan.", WorkspaceRed)
                postAssessment?.isComplete == true -> {
                    val postScore = postAssessment.totalScore!!
                    val difference = postScore - score
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("POST-TEST IDENTITAS", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, letterSpacing = .4.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("$postScore / 60 · Kecenderungan ${postAssessment.category!!.label.lowercase()}", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.testTag("child-detail-post-test-result"))
                            Text("Perubahan skor dari pre-test: ${if (difference > 0) "+" else ""}$difference", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                        }
                    }
                }
                else -> {
                    if (postAssessment != null) {
                        DetailNoticeCard("Post-test masih berupa draf", "Terjawab ${postAssessment.answers.count { it != null }} dari ${IdentityForeclosureInstrument.items.size} butir.", WorkspaceBlue)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { onStartAssessment(AssessmentPhase.POST_TEST) },
                        modifier = Modifier.fillMaxWidth().height(49.dp).testTag("child-detail-post-test"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
                    ) {
                        Text(if (postAssessment == null) "Mulai post-test identitas" else "Lanjutkan post-test", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
            }
        } else {
            val message = when {
                assessment == null -> "Belum ada hasil untuk anak ini. Mulai pre-test identitas agar ringkasan awal dapat ditinjau di sini."
                else -> "Pre-test masih berupa draf (${assessment.answers.count { it != null }} dari ${IdentityForeclosureInstrument.items.size} butir terjawab). Lanjutkan untuk memperoleh hasil."
            }
            DetailNoticeCard("Statistik pre-test belum tersedia", message, WorkspaceBlue)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onStartAssessment(AssessmentPhase.PRE_TEST) },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("child-detail-start-pretest"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) {
                Text(if (assessment == null) "Mulai pre-test identitas" else "Lanjutkan pre-test", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("PERBANDINGAN PRE-TEST & POST-TEST", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .65.sp)
        Spacer(Modifier.height(6.dp))
        Text("Perubahan tiap tes ditampilkan terpisah.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.testTag("child-detail-assessment-comparison")) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                AssessmentComparisonRow(
                    "Identity Foreclosure",
                    assessment?.takeIf { it.isComplete }?.totalScore,
                    postAssessment?.takeIf { it.isComplete }?.totalScore,
                    60,
                    "comparison-identity",
                )
                AssessmentComparisonRow(
                    "Orientasi Masa Depan",
                    futurePre?.takeIf { it.isComplete }?.totalScore,
                    futurePost?.takeIf { it.isComplete }?.totalScore,
                    60,
                    "comparison-future",
                )
                AssessmentComparisonRow(
                    "HERO Asa · Forethought",
                    asaPre?.takeIf { it.isComplete }?.totalScore,
                    asaPost?.takeIf { it.isComplete }?.totalScore,
                    40,
                    "comparison-asa",
                )
                AssessmentComparisonRow(
                    "HERO Aksi · Performance",
                    aksiPre?.takeIf { it.isComplete }?.totalScore,
                    aksiPost?.takeIf { it.isComplete }?.totalScore,
                    40,
                    "comparison-aksi",
                )
                AssessmentComparisonRow(
                    "HERO Rasa · Reflection",
                    rasaPre?.takeIf { it.isComplete }?.totalScore,
                    rasaPost?.takeIf { it.isComplete }?.totalScore,
                    40,
                    "comparison-rasa",
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("SUBTES PENILAIAN", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .7.sp)
        Spacer(Modifier.height(7.dp))
        InstrumentStatusCard("Identity Foreclosure", "U-MICS · 15 pernyataan", "Tersedia", active = true, testTag = "child-instrument-identity")
        InstrumentStatusCard(
            "Orientasi Masa Depan",
            "Teori Nurmi · 15 pernyataan",
            if (futureOrientationRepository != null) "Input manual" else "Dalam pengembangan",
            active = futureOrientationRepository != null,
            testTag = "child-instrument-future",
        )
        if (futureOrientationRepository != null) {
            Spacer(Modifier.height(4.dp))
            when {
                futurePreLoad.isFailure || futurePostLoad.isFailure -> DetailNoticeCard(
                    "Hasil Orientasi Masa Depan belum dapat dibaca",
                    "Periksa penyimpanan perangkat sebelum membuka atau mengubah asesmen.",
                    WorkspaceRed,
                )
            }
            if (futurePost?.isComplete == true) {
                val result = futurePost
                FutureOrientationResultSummary("POST-TEST ORIENTASI MASA DEPAN", result, "child-detail-future-post-result")
            } else if (futurePost != null) {
                DetailNoticeCard(
                    "Post-test Orientasi Masa Depan masih berupa draf",
                    "Terjawab ${futurePost.answers.count { it != null }} dari ${FutureOrientationInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onStartFutureAssessment(AssessmentPhase.PRE_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-future-pre-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) {
                Text(if (futurePre == null) "Mulai pre-test orientasi masa depan" else if (futurePre.isComplete) "Pre-test orientasi masa depan selesai" else "Lanjutkan pre-test orientasi masa depan", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = { onStartFutureAssessment(AssessmentPhase.POST_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-future-post-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) {
                Text(if (futurePost == null) "Mulai post-test orientasi masa depan" else if (futurePost.isComplete) "Post-test orientasi masa depan selesai" else "Lanjutkan post-test orientasi masa depan", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
        }
        Spacer(Modifier.height(12.dp))
        InstrumentStatusCard(
            "HERO Asa",
            "Forethought · 10 pernyataan",
            if (heroAsaRepository != null) "Input manual" else "Dalam pengembangan",
            active = heroAsaRepository != null,
            testTag = "child-instrument-asa",
        )
        if (heroAsaRepository != null) {
            Spacer(Modifier.height(4.dp))
            when {
                asaPreLoad.isFailure || asaPostLoad.isFailure -> DetailNoticeCard(
                    "Hasil HERO Asa belum dapat dibaca",
                    "Periksa penyimpanan perangkat sebelum membuka atau mengubah asesmen.",
                    WorkspaceRed,
                )
                asaPre?.isComplete == true -> HeroAsaResultSummary("PRE-TEST HERO ASA · FORETHOUGHT", asaPre, "child-detail-asa-pre-result")
                asaPre != null -> DetailNoticeCard(
                    "Pre-test HERO Asa masih berupa draf",
                    "Terjawab ${asaPre.answers.count { it != null }} dari ${HeroAsaInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            if (asaPost?.isComplete == true) {
                HeroAsaResultSummary("POST-TEST HERO ASA · FORETHOUGHT", asaPost, "child-detail-asa-post-result")
            } else if (asaPost != null) {
                DetailNoticeCard(
                    "Post-test HERO Asa masih berupa draf",
                    "Terjawab ${asaPost.answers.count { it != null }} dari ${HeroAsaInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onStartHeroAsaAssessment(AssessmentPhase.PRE_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-asa-pre-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) {
                Text(if (asaPre == null) "Mulai pre-test HERO Asa" else if (asaPre.isComplete) "Pre-test HERO Asa selesai" else "Lanjutkan pre-test HERO Asa", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = { onStartHeroAsaAssessment(AssessmentPhase.POST_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-asa-post-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) {
                Text(if (asaPost == null) "Mulai post-test HERO Asa" else if (asaPost.isComplete) "Post-test HERO Asa selesai" else "Lanjutkan post-test HERO Asa", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
        }
        InstrumentStatusCard(
            "HERO Aksi",
            "Performance · 10 pernyataan",
            if (heroAksiRepository != null) "Input manual" else "Dalam pengembangan",
            active = heroAksiRepository != null,
            testTag = "child-instrument-aksi",
        )
        if (heroAksiRepository != null) {
            Spacer(Modifier.height(4.dp))
            when {
                aksiPreLoad.isFailure || aksiPostLoad.isFailure -> DetailNoticeCard(
                    "Hasil HERO Aksi belum dapat dibaca",
                    "Periksa penyimpanan perangkat sebelum membuka atau mengubah asesmen.",
                    WorkspaceRed,
                )
                aksiPre?.isComplete == true -> HeroAksiResultSummary("PRE-TEST HERO AKSI · PERFORMANCE", aksiPre, "child-detail-aksi-pre-result")
                aksiPre != null -> DetailNoticeCard(
                    "Pre-test HERO Aksi masih berupa draf",
                    "Terjawab ${aksiPre.answers.count { it != null }} dari ${HeroAksiInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            if (aksiPost?.isComplete == true) {
                HeroAksiResultSummary("POST-TEST HERO AKSI · PERFORMANCE", aksiPost, "child-detail-aksi-post-result")
            } else if (aksiPost != null) {
                DetailNoticeCard(
                    "Post-test HERO Aksi masih berupa draf",
                    "Terjawab ${aksiPost.answers.count { it != null }} dari ${HeroAksiInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onStartHeroAksiAssessment(AssessmentPhase.PRE_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-aksi-pre-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) {
                Text(if (aksiPre == null) "Mulai pre-test HERO Aksi" else if (aksiPre.isComplete) "Pre-test HERO Aksi selesai" else "Lanjutkan pre-test HERO Aksi", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = { onStartHeroAksiAssessment(AssessmentPhase.POST_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-aksi-post-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) {
                Text(if (aksiPost == null) "Mulai post-test HERO Aksi" else if (aksiPost.isComplete) "Post-test HERO Aksi selesai" else "Lanjutkan post-test HERO Aksi", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
        }
        InstrumentStatusCard(
            "HERO Rasa",
            "Reflection · 10 pernyataan",
            if (heroRasaRepository != null) "Input manual" else "Dalam pengembangan",
            active = heroRasaRepository != null,
            testTag = "child-instrument-rasa",
        )
        if (heroRasaRepository != null) {
            Spacer(Modifier.height(4.dp))
            when {
                rasaPreLoad.isFailure || rasaPostLoad.isFailure -> DetailNoticeCard(
                    "Hasil HERO Rasa belum dapat dibaca",
                    "Periksa penyimpanan perangkat sebelum membuka atau mengubah asesmen.",
                    WorkspaceRed,
                )
                rasaPre?.isComplete == true -> HeroRasaResultSummary("PRE-TEST HERO RASA · REFLECTION", rasaPre, "child-detail-rasa-pre-result")
                rasaPre != null -> DetailNoticeCard(
                    "Pre-test HERO Rasa masih berupa draf",
                    "Terjawab ${rasaPre.answers.count { it != null }} dari ${HeroRasaInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            if (rasaPost?.isComplete == true) {
                HeroRasaResultSummary("POST-TEST HERO RASA · REFLECTION", rasaPost, "child-detail-rasa-post-result")
            } else if (rasaPost != null) {
                DetailNoticeCard(
                    "Post-test HERO Rasa masih berupa draf",
                    "Terjawab ${rasaPost.answers.count { it != null }} dari ${HeroRasaInstrument.items.size} butir.",
                    WorkspaceBlue,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onStartHeroRasaAssessment(AssessmentPhase.PRE_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-rasa-pre-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) {
                Text(if (rasaPre == null) "Mulai pre-test HERO Rasa" else if (rasaPre.isComplete) "Pre-test HERO Rasa selesai" else "Lanjutkan pre-test HERO Rasa", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = { onStartHeroRasaAssessment(AssessmentPhase.POST_TEST) },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("child-detail-rasa-post-test"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) {
                Text(if (rasaPost == null) "Mulai post-test HERO Rasa" else if (rasaPost.isComplete) "Post-test HERO Rasa selesai" else "Lanjutkan post-test HERO Rasa", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("TINDAK LANJUT", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .7.sp)
        Spacer(Modifier.height(5.dp))
        Text("Catat program atau kegiatan yang benar-benar dilakukan bersama anak.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
        if (interventionLoad.isFailure) {
            Spacer(Modifier.height(8.dp))
            DetailNoticeCard("Catatan tindak lanjut belum dapat dibuka", "Periksa penyimpanan perangkat sebelum mencatat kegiatan baru.", WorkspaceRed)
        }
        interventions.forEach { record ->
            Spacer(Modifier.height(7.dp))
            Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.testTag("intervention-record-${record.id}")) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text(record.programName, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text(record.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", IndonesianLocale)), color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                    if (record.notes.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text(record.notes, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                editingInterventionId = record.id
                                interventionProgram = record.programName
                                interventionNotes = record.notes
                                interventionDate = record.date
                                interventionFeedback = ""
                                addingIntervention = true
                            },
                            modifier = Modifier.testTag("intervention-edit-${record.id}"),
                        ) { Text("Ubah", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 9.sp) }
                        TextButton(
                            onClick = { pendingInterventionDelete = record },
                            modifier = Modifier.testTag("intervention-delete-${record.id}"),
                        ) { Text("Hapus", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 9.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (addingIntervention) {
            Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(if (editingInterventionId == null) "Catatan kegiatan" else "Ubah catatan kegiatan", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { birthDatePicker.show(interventionDate) { interventionDate = it } },
                        modifier = Modifier.testTag("intervention-date-picker"),
                    ) {
                        Text("Tanggal: ${interventionDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))}", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                    }
                    OutlinedTextField(
                        value = interventionProgram,
                        onValueChange = { interventionProgram = it.take(100); interventionFeedback = "" },
                        modifier = Modifier.fillMaxWidth().testTag("intervention-program-name"),
                        label = { Text("Nama program/kegiatan", fontFamily = WorkspacePoppins, fontSize = 10.sp) },
                        placeholder = { Text("Isi sesuai kegiatan yang dilakukan", fontFamily = WorkspacePoppins, fontSize = 10.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                    )
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(
                        value = interventionNotes,
                        onValueChange = { interventionNotes = it.take(500); interventionFeedback = "" },
                        modifier = Modifier.fillMaxWidth().testTag("intervention-notes"),
                        label = { Text("Catatan singkat (opsional)", fontFamily = WorkspacePoppins, fontSize = 10.sp) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(13.dp),
                    )
                    if (interventionFeedback.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text(interventionFeedback, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 9.sp, modifier = Modifier.testTag("intervention-feedback"))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        androidx.compose.material3.TextButton(onClick = { addingIntervention = false; editingInterventionId = null; interventionFeedback = "" }, modifier = Modifier.weight(1f).testTag("intervention-cancel")) {
                            Text("Batal", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                val currentId = editingInterventionId
                                val result = if (currentId == null) {
                                    interventionRepository?.add(child.id, interventionDate, interventionProgram, interventionNotes)
                                } else {
                                    interventionRepository?.update(InterventionRecord(currentId, child.id, interventionDate, interventionProgram, interventionNotes))
                                } ?: SaveInterventionResult.StorageError
                                when (result) {
                                    is SaveInterventionResult.Saved -> {
                                        val existingIndex = interventions.indexOfFirst { it.id == result.record.id }
                                        if (existingIndex >= 0) interventions[existingIndex] = result.record else interventions.add(0, result.record)
                                        addingIntervention = false
                                        editingInterventionId = null
                                        interventionProgram = ""
                                        interventionNotes = ""
                                        interventionDate = LocalDate.now()
                                        interventionFeedback = ""
                                    }
                                    SaveInterventionResult.InvalidProgramName -> interventionFeedback = "Isi nama program atau kegiatan (2–100 karakter)."
                                    SaveInterventionResult.InvalidNotes -> interventionFeedback = "Catatan maksimal 500 karakter."
                                    SaveInterventionResult.InvalidDate -> interventionFeedback = "Tanggal kegiatan tidak boleh di masa depan."
                                    SaveInterventionResult.StorageError -> interventionFeedback = "Catatan tidak tersimpan. Periksa penyimpanan perangkat lalu coba lagi."
                                }
                            },
                            enabled = interventionRepository != null && interventionLoad.isSuccess,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("intervention-save"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
                        ) { Text(if (editingInterventionId == null) "Simpan" else "Simpan perubahan", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
                    }
                }
            }
        } else {
            Button(
                onClick = { addingIntervention = true },
                enabled = interventionRepository != null && interventionLoad.isSuccess,
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("intervention-add"),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceRed),
            ) { Text("Tambah catatan tindak lanjut", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
        }
        if (interventions.isEmpty() && interventionLoad.isSuccess) {
            Spacer(Modifier.height(5.dp))
            Text("Belum ada kegiatan tercatat.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Nama kegiatan diisi oleh pendamping; aplikasi belum menyediakan katalog program resmi.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.testTag("intervention-catalog-note"))
        }
    }
    if (showEditChild) {
        AlertDialog(
            onDismissRequest = { showEditChild = false },
            title = { Text("Ubah data anak", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editChildName,
                        onValueChange = { editChildName = it.take(80); editChildError = "" },
                        label = { Text("Nama") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("child-edit-name"),
                    )
                    TextButton(onClick = { birthDatePicker.show(editChildBirthDate) { editChildBirthDate = it } }) {
                        Text("Tanggal lahir: ${editChildBirthDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))}", fontFamily = WorkspacePoppins, fontSize = 10.sp)
                    }
                    Text("Jenis kelamin", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                    Row {
                        ChildChoice("Laki-laki", editChildGender == ChildGender.Boy, "child-edit-gender-boy", Modifier.weight(1f)) { editChildGender = ChildGender.Boy }
                        ChildChoice("Perempuan", editChildGender == ChildGender.Girl, "child-edit-gender-girl", Modifier.weight(1f)) { editChildGender = ChildGender.Girl }
                    }
                    Text("Terlibat memulung", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                    Row {
                        ChildChoice("Ya", editChildScavenging == true, "child-edit-scavenging-yes", Modifier.weight(1f)) { editChildScavenging = true }
                        ChildChoice("Tidak", editChildScavenging == false, "child-edit-scavenging-no", Modifier.weight(1f)) { editChildScavenging = false }
                    }
                    if (editChildError.isNotBlank()) Text(editChildError, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 9.sp, modifier = Modifier.testTag("child-edit-error"))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            editChildName.trim().length < 2 -> editChildError = "Nama minimal 2 karakter."
                            editChildGender == null || editChildScavenging == null -> editChildError = "Lengkapi jenis kelamin dan status memulung."
                            onUpdateChild(child.copy(name = editChildName.trim(), birthDate = editChildBirthDate, gender = editChildGender!!, involvedInScavenging = editChildScavenging!!)) -> showEditChild = false
                            else -> editChildError = "Perubahan belum tersimpan. Coba lagi setelah memeriksa penyimpanan."
                        }
                    },
                    modifier = Modifier.testTag("child-edit-save"),
                ) { Text("Simpan", fontFamily = WorkspacePoppins) }
            },
            dismissButton = { TextButton(onClick = { showEditChild = false }) { Text("Batal", fontFamily = WorkspacePoppins) } },
            modifier = Modifier.testTag("child-edit-dialog"),
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus data ${child.name}?", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Profil anak, semua jawaban dan hasil tes pre/post, serta catatan tindak lanjut akan dihapus dari perangkat ini. Tindakan ini tidak dapat dibatalkan. Ekspor backup terlebih dahulu jika datanya mungkin masih diperlukan.",
                    fontFamily = WorkspacePoppins,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onDeleteChild(child.id)) {
                            showDeleteConfirmation = false
                        } else {
                            showDeleteConfirmation = false
                            deleteFeedback = "Data belum terhapus sepenuhnya. Coba lagi setelah memeriksa penyimpanan perangkat."
                        }
                    },
                    modifier = Modifier.testTag("child-delete-confirm"),
                ) { Text("Hapus semua data", color = WorkspaceRed, fontFamily = WorkspacePoppins) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }, modifier = Modifier.testTag("child-delete-cancel")) {
                    Text("Batal", fontFamily = WorkspacePoppins)
                }
            },
            modifier = Modifier.testTag("child-delete-dialog"),
        )
    }
    pendingInterventionDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingInterventionDelete = null },
            title = { Text("Hapus catatan kegiatan?", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) },
            text = { Text("Catatan ‘${record.programName}’ akan dihapus.", fontFamily = WorkspacePoppins, fontSize = 11.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (interventionRepository?.delete(record.id) == true) {
                            interventions.removeAll { it.id == record.id }
                            pendingInterventionDelete = null
                        } else {
                            pendingInterventionDelete = null
                            interventionFeedback = "Catatan belum terhapus. Periksa penyimpanan lalu coba lagi."
                        }
                    },
                    modifier = Modifier.testTag("intervention-delete-confirm"),
                ) { Text("Hapus", color = WorkspaceRed, fontFamily = WorkspacePoppins) }
            },
            dismissButton = { TextButton(onClick = { pendingInterventionDelete = null }) { Text("Batal", fontFamily = WorkspacePoppins) } },
            modifier = Modifier.testTag("intervention-delete-dialog"),
        )
    }
}

@Composable
private fun AssessmentComparisonRow(
    title: String,
    preScore: Int?,
    postScore: Int?,
    maximumScore: Int,
    testTag: String,
) {
    Column(Modifier.fillMaxWidth().testTag(testTag)) {
        Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            "Pre-test ${preScore?.let { "$it/$maximumScore" } ?: "—"}   ·   Post-test ${postScore?.let { "$it/$maximumScore" } ?: "—"}",
            color = WorkspaceMuted,
            fontFamily = WorkspacePoppins,
            fontSize = 9.sp,
            modifier = Modifier.testTag("$testTag-scores"),
        )
        Text(
            if (preScore != null && postScore != null) {
                val change = postScore - preScore
                "Perubahan skor ${if (change > 0) "+" else ""}$change"
            } else {
                "Lengkapi pre-test dan post-test untuk membandingkan skor"
            },
            color = if (preScore != null && postScore != null) WorkspaceBlue else WorkspaceMuted,
            fontFamily = WorkspacePoppins,
            fontSize = 9.sp,
            modifier = Modifier.testTag("$testTag-change"),
        )
    }
}

@Composable
private fun InstrumentStatusCard(title: String, subtitle: String, status: String, active: Boolean, testTag: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).testTag(testTag),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) Color.White else Color(0xFFEEF1F4)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text(subtitle, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
            Text(status, color = if (active) WorkspaceBlue else WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 8.sp)
        }
    }
}

@Composable
private fun AssessmentInputMethodPage(
    child: ChildRecord,
    phase: AssessmentPhase,
    instrumentTitle: String,
    ocrAvailable: Boolean,
    error: String?,
    processing: Boolean,
    navigationError: String?,
    onBack: () -> Unit,
    onManual: () -> Unit,
    onChooseFile: () -> Unit,
    onScanNow: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("assessment-input-method-page"),
    ) {
        NavigationBackButton(onClick = onBack, testTag = "assessment-input-method-back")
        PageHeading("Input ${phase.displayName.lowercase()}", "${child.name} · $instrumentTitle")
        Spacer(Modifier.height(7.dp))
        Text("Pilih cara mengisi hasil tes.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onScanNow,
            enabled = !processing && ocrAvailable,
            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("assessment-input-camera"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkspaceRed),
        ) {
            Text(if (processing) "Memindai formulir…" else "Scan langsung", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(9.dp))
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onChooseFile,
            enabled = !processing && ocrAvailable,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("assessment-input-file"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
        ) { Text("Input via PDF/Foto", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        Spacer(Modifier.height(5.dp))
        Text("Pilih PDF atau gambar seperti JPG, PNG, dan HEIC dari perangkat.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onManual,
            enabled = !processing,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("assessment-input-manual"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WorkspaceInk),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        ) { Text("Input manual · skala 1–4", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        if (processing) {
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth().testTag("assessment-ocr-progress"), color = WorkspaceBlue)
        }
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("assessment-ocr-error"))
        }
        if (!navigationError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(navigationError, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("assessment-navigation-error"))
        }
    }
}

private fun minOfNotNull(first: Long?, second: Long?): Long? = when {
    first == null -> second
    second == null -> first
    else -> minOf(first, second)
}

@Composable
private fun BaselineSummaryCard(
    identity: IdentityForeclosureAssessment?,
    identityLoadFailed: Boolean,
    future: FutureOrientationAssessment?,
    futureLoadFailed: Boolean,
) {
    Card(Modifier.fillMaxWidth().testTag("child-detail-baseline-summary"), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("HASIL PRE-TEST", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .6.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BaselineInstrumentSummary("Identitas", identity?.takeIf { it.isComplete }?.totalScore, identity?.takeIf { it.isComplete }?.category?.label, "child-detail-score-progress", if (identityLoadFailed) "Gagal dibaca" else if (identity == null) "Belum diisi" else "Draf", Modifier.weight(1f))
                BaselineInstrumentSummary("Masa depan", future?.takeIf { it.isComplete }?.totalScore, future?.takeIf { it.isComplete }?.category?.label, "child-detail-future-score-progress", if (futureLoadFailed) "Gagal dibaca" else if (future == null) "Belum diisi" else "Draf", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BaselineInstrumentSummary(title: String, score: Int?, category: String?, progressTag: String, issue: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(WorkspaceCanvas).padding(10.dp)) {
        Text(title, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp)
        if (score == null) {
            Text(issue, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp)
                Text(" / 60", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            LinearProgressIndicator(progress = { score / 60f }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(6.dp).testTag(progressTag), color = WorkspaceBlue, trackColor = Color(0xFFE2E9EF))
            category?.let { Text(it, color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 8.sp, modifier = Modifier.padding(top = 5.dp)) }
        }
    }
}

@Composable
private fun NavigationBackButton(onClick: () -> Unit, testTag: String) {
    Surface(onClick = onClick, modifier = Modifier.padding(start = 12.dp, top = 4.dp).size(44.dp).testTag(testTag).semantics { contentDescription = "Kembali" }, shape = RoundedCornerShape(13.dp), color = Color(0xFFEAF3F9), contentColor = WorkspaceBlue) {
        Box(contentAlignment = Alignment.Center) { Text("‹", fontSize = 31.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun ExperimentalForecastCard(forecast: ExperimentalBaselineForecast) {
    val priorityAreas = forecast.estimates
        .filter { it.isAbovePrototypeThreshold }
        .sortedByDescending { it.probabilityBelowTarget }
    var showModelDetails by remember(forecast.modelVersion) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("experimental-ml-forecast"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("HASIL PREDIKSI MODEL", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = .6.sp)
            Spacer(Modifier.height(8.dp))
            Text("Empat area", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp)
            Spacer(Modifier.height(5.dp))
            forecast.estimates.chunked(2).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { estimate ->
                        ModelAreaStatusCard(estimate, Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            if (priorityAreas.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEAF7EF)), contentAlignment = Alignment.Center) {
                        Text("✓", color = Color(0xFF28724C), fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                    Column {
                        Text("Tidak ada area berlabel PERLU", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.testTag("experimental-ml-focus"))
                        Text("Pilih kegiatan bersama anak.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                    }
                }
            } else {
                val first = priorityAreas.first()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFEFEB)), contentAlignment = Alignment.Center) {
                        Text("!", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                    Column {
                        Text("Fokus utama", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                        Text(
                            "${first.programName} · ${modelTargetName(first.targetId)}",
                            color = WorkspaceInk,
                            fontFamily = WorkspacePoppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("experimental-ml-focus"),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7FB))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                            Text("↗", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Column {
                            Text("CONTOH KEGIATAN", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp)
                            Text(modelActivitySuggestion(first.targetId), color = WorkspaceInk, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("experimental-ml-activity"))
                        }
                    }
                }
            }
            androidx.compose.material3.TextButton(
                onClick = { showModelDetails = !showModelDetails },
                modifier = Modifier.testTag("experimental-ml-details-toggle"),
            ) {
                Text(if (showModelDetails) "Sembunyikan detail model" else "Tentang model", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
            if (showModelDetails) {
                Text(
                    "PERLU berarti model menandai kemungkinan skor pascaprogram di bawah 30 pada area itu. Contoh kegiatan dipetakan dari area prediksi; model belum belajar memilih aktivitas terbaik.",
                    color = WorkspaceMuted,
                    fontFamily = WorkspacePoppins,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.testTag("experimental-ml-explanation"),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Model ${forecast.modelVersion} · ${forecast.trainingRows} data latih · Macro-F1 ${(forecast.macroF1Mean * 100).roundToInt()}% ± ${(forecast.macroF1Std * 100).roundToInt()}%",
                    color = WorkspaceMuted,
                    fontFamily = WorkspacePoppins,
                    fontSize = 8.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.testTag("experimental-ml-metrics"),
                )
            }
            if (forecast.inputAgeOutsideTrainingRange) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Usia anak berada di luar rentang data latih (8–13 tahun); hasil perlu ditafsirkan ekstra hati-hati.",
                    color = WorkspaceRed,
                    fontFamily = WorkspacePoppins,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.testTag("experimental-ml-age-warning"),
                )
            }
        }
    }
}

@Composable
private fun ModelAreaStatusCard(estimate: DomainRiskEstimate, modifier: Modifier = Modifier) {
    val flagged = estimate.isAbovePrototypeThreshold
    val accent = if (flagged) WorkspaceRed else Color(0xFF28724C)
    val tint = if (flagged) Color(0xFFFFEFEB) else Color(0xFFEAF7EF)
    Card(
        modifier = modifier.testTag("experimental-ml-area-card-${estimate.targetId}"),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = tint),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (flagged) "!" else "✓", color = accent, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(if (flagged) "PERLU" else "TIDAK DITANDAI", color = accent, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 7.sp, maxLines = 1, modifier = Modifier.testTag("experimental-ml-status-${estimate.targetId}"))
            }
            Spacer(Modifier.height(3.dp))
            Text(estimate.programName, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, maxLines = 1)
            Text(modelTargetName(estimate.targetId), color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 8.sp, maxLines = 1)
        }
    }
}

private fun modelTargetName(targetId: String): String = when (targetId) {
    "Y_Potensi" -> "Potensi"
    "Y_Eksplorasi" -> "Eksplorasi"
    "Y_Refleksi" -> "Refleksi"
    "Y_Perencanaan" -> "Perencanaan"
    else -> targetId
}

private fun modelActivitySuggestion(targetId: String): String = when (targetId) {
    "Y_Potensi" -> "Buat peta minat dan cita-cita. Pilih satu profesi untuk dikenali lewat cerita, video, atau narasumber."
    "Y_Eksplorasi" -> "Ajak anak mencoba peran atau profesi lewat role-play, wawancara narasumber, atau kunjungan edukatif seperti KidZania bila tersedia."
    "Y_Refleksi" -> "Setelah kegiatan, ajak anak bercerita: apa yang dirasakan, disukai, dan ingin dicoba lagi."
    "Y_Perencanaan" -> "Bantu anak memilih satu tujuan sederhana dan menyusun langkah yang ia pilih sendiri."
    else -> "Tanyakan minat anak dan pilih satu kegiatan yang sesuai bersama pendamping."
}

@Composable
private fun DetailNoticeCard(title: String, message: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = accent, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            Text(message, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun FutureOrientationResultSummary(
    title: String,
    assessment: FutureOrientationAssessment,
    testTag: String,
) {
    val score = assessment.totalScore ?: return
    val category = assessment.category ?: return
    val color = when (category) {
        FutureOrientationCategory.LOW -> WorkspaceRed
        FutureOrientationCategory.MEDIUM -> Color(0xFF946000)
        FutureOrientationCategory.HIGH -> Color(0xFF28724C)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, letterSpacing = .35.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, modifier = Modifier.testTag(testTag))
                Text(" / 60 · ${category.label}", color = color, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp, bottom = 3.dp))
            }
            LinearProgressIndicator(
                progress = { score / 60f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(7.dp),
                color = color,
                trackColor = Color(0xFFE9EDF2),
            )
        }
    }
}

@Composable
private fun HeroAsaResultSummary(title: String, assessment: HeroAsaAssessment, testTag: String) {
    val score = assessment.totalScore ?: return
    val category = assessment.category ?: return
    val color = when (category) {
        HeroAsaCategory.LOW -> WorkspaceRed
        HeroAsaCategory.MEDIUM -> Color(0xFF946000)
        HeroAsaCategory.HIGH -> Color(0xFF28724C)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, letterSpacing = .35.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, modifier = Modifier.testTag(testTag))
                Text(" / 40 · ${category.label}", color = color, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp, bottom = 3.dp))
            }
            LinearProgressIndicator(
                progress = { score / 40f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(7.dp),
                color = color,
                trackColor = Color(0xFFE9EDF2),
            )
        }
    }
}


@Composable
private fun HeroAksiResultSummary(title: String, assessment: HeroAksiAssessment, testTag: String) {
    val score = assessment.totalScore ?: return
    val category = assessment.category ?: return
    val color = when (category) {
        HeroAksiCategory.LOW -> WorkspaceRed
        HeroAksiCategory.MEDIUM -> Color(0xFF946000)
        HeroAksiCategory.HIGH -> Color(0xFF28724C)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, letterSpacing = .35.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, modifier = Modifier.testTag(testTag))
                Text(" / 40 · ${category.label}", color = color, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp, bottom = 3.dp))
            }
            LinearProgressIndicator(
                progress = { score / 40f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(7.dp),
                color = color,
                trackColor = Color(0xFFE9EDF2),
            )
        }
    }
}


@Composable
private fun HeroRasaResultSummary(title: String, assessment: HeroRasaAssessment, testTag: String) {
    val score = assessment.totalScore ?: return
    val category = assessment.category ?: return
    val color = when (category) {
        HeroRasaCategory.LOW -> WorkspaceRed
        HeroRasaCategory.MEDIUM -> Color(0xFF946000)
        HeroRasaCategory.HIGH -> Color(0xFF28724C)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, letterSpacing = .35.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, modifier = Modifier.testTag(testTag))
                Text(" / 40 · ${category.label}", color = color, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp, bottom = 3.dp))
            }
            LinearProgressIndicator(
                progress = { score / 40f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(7.dp),
                color = color,
                trackColor = Color(0xFFE9EDF2),
            )
        }
    }
}

@Composable
private fun FutureOrientationAssessmentPage(
    child: ChildRecord,
    repository: FutureOrientationRepository?,
    phase: AssessmentPhase,
    initialAnswers: List<Int?>,
    navigationError: String?,
    onAnswersChanged: (List<Int?>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initialLoad = remember(child.id, repository, phase) {
        runCatching { repository?.get(child.id, phase) }
    }
    var assessment by remember(child.id, repository, phase) { mutableStateOf(initialLoad.getOrNull()) }
    var storageIssue by remember(child.id, repository, phase) { mutableStateOf(initialLoad.isFailure || repository == null) }
    val answers = remember(child.id, assessment?.id, phase) {
        mutableStateListOf<Int?>().apply {
            addAll(assessment?.answers ?: initialAnswers.takeIf { it.size == FutureOrientationInstrument.items.size }
                ?: List(FutureOrientationInstrument.items.size) { null })
        }
    }
    var feedback by remember(child.id, phase) { mutableStateOf("") }
    val answeredCount = answers.count { it != null }
    val completed = assessment?.isComplete == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("future-assessment-scroll").padding(horizontal = 20.dp, vertical = 18.dp)) {
        NavigationBackButton(onClick = onBack, testTag = "future-assessment-back")
        PageHeading("${phase.displayName} orientasi masa depan", child.name)
        if (!navigationError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(navigationError, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("future-assessment-navigation-error"))
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(FutureOrientationInstrument.TITLE, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Skala 1–4", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Spacer(Modifier.height(8.dp))
                Text("1 Sangat tidak setuju  ·  2 Tidak setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Text("3 Setuju  ·  4 Sangat setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Terjawab $answeredCount dari ${FutureOrientationInstrument.items.size}", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, modifier = Modifier.testTag("future-assessment-answered"))
        Spacer(Modifier.height(8.dp))
        FutureOrientationInstrument.items.forEachIndexed { index, statement ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("future-assessment-question-$index"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("${index + 1}. $statement", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..4).forEach { value ->
                            AssessmentAnswerChoice(
                                value = value,
                                selected = answers[index] == value,
                                enabled = !completed,
                                modifier = Modifier.weight(1f).testTag("future-assessment-answer-$index-$value"),
                            ) {
                                answers[index] = value
                                onAnswersChanged(answers.toList())
                                feedback = ""
                            }
                        }
                    }
                }
            }
        }
        if (completed) {
            Spacer(Modifier.height(4.dp))
            FutureOrientationResultSummary("HASIL ${phase.displayName.uppercase()} ORIENTASI MASA DEPAN", assessment!!, "future-assessment-result-score")
        } else {
            if (storageIssue) {
                Text("Penyimpanan asesmen bermasalah. Periksa ruang penyimpanan perangkat.", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("future-assessment-storage-error"))
            }
            if (feedback.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(feedback, color = if (storageIssue) WorkspaceRed else WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("future-assessment-feedback"))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    when (val result = repository?.saveDraft(child.id, answers.toList(), phase) ?: FutureOrientationSaveResult.StorageError) {
                        is FutureOrientationSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Draf ${phase.displayName.lowercase()} tersimpan di perangkat ini."; storageIssue = false }
                        FutureOrientationSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        FutureOrientationSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        is FutureOrientationSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai." }
                        FutureOrientationSaveResult.StorageError -> { storageIssue = true; feedback = "Draf belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("future-assessment-save-draft"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) { Text("Simpan sebagai draf", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (answeredCount < FutureOrientationInstrument.items.size) {
                        feedback = "Lengkapi semua butir sebelum menyelesaikan ${phase.displayName.lowercase()}."
                    } else {
                        when (val result = repository?.complete(child.id, answers.toList(), phase) ?: FutureOrientationSaveResult.StorageError) {
                            is FutureOrientationSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai."; storageIssue = false }
                            FutureOrientationSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                            FutureOrientationSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                            is FutureOrientationSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Jawaban tersimpan sebagai draf." }
                            FutureOrientationSaveResult.StorageError -> { storageIssue = true; feedback = "Hasil belum dapat disimpan." }
                        }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("future-assessment-complete"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) { Text("Selesaikan ${phase.displayName.lowercase()}", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroAsaAssessmentPage(
    child: ChildRecord,
    repository: HeroAsaRepository?,
    phase: AssessmentPhase,
    initialAnswers: List<Int?>,
    navigationError: String?,
    onAnswersChanged: (List<Int?>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initialLoad = remember(child.id, repository, phase) { runCatching { repository?.get(child.id, phase) } }
    var assessment by remember(child.id, repository, phase) { mutableStateOf(initialLoad.getOrNull()) }
    var storageIssue by remember(child.id, repository, phase) { mutableStateOf(initialLoad.isFailure || repository == null) }
    val answers = remember(child.id, assessment?.id, phase) {
        mutableStateListOf<Int?>().apply {
            addAll(assessment?.answers ?: initialAnswers.takeIf { it.size == HeroAsaInstrument.items.size } ?: List(HeroAsaInstrument.items.size) { null })
        }
    }
    var feedback by remember(child.id, phase) { mutableStateOf("") }
    val answeredCount = answers.count { it != null }
    val completed = assessment?.isComplete == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("hero-asa-assessment-scroll").padding(horizontal = 20.dp, vertical = 18.dp)) {
        NavigationBackButton(onClick = onBack, testTag = "hero-asa-assessment-back")
        PageHeading("${phase.displayName} HERO Asa", child.name)
        if (!navigationError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(navigationError, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("hero-asa-assessment-navigation-error"))
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(HeroAsaInstrument.TITLE, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Skala 1–4", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Spacer(Modifier.height(8.dp))
                Text("1 Sangat tidak setuju  ·  2 Tidak setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Text("3 Setuju  ·  4 Sangat setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Terjawab $answeredCount dari 10", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, modifier = Modifier.testTag("hero-asa-assessment-answered"))
        Spacer(Modifier.height(8.dp))
        HeroAsaInstrument.items.forEachIndexed { index, statement ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("hero-asa-assessment-question-$index"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("${index + 1}. $statement", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..4).forEach { value ->
                            AssessmentAnswerChoice(value, answers[index] == value, !completed, Modifier.weight(1f).testTag("hero-asa-assessment-answer-$index-$value")) {
                                answers[index] = value
                                onAnswersChanged(answers.toList())
                                feedback = ""
                            }
                        }
                    }
                }
            }
        }
        if (completed) {
            HeroAsaResultSummary("HASIL ${phase.displayName.uppercase()} HERO ASA", assessment!!, "hero-asa-assessment-result-score")
        } else {
            if (storageIssue) Text("Penyimpanan asesmen bermasalah. Periksa ruang penyimpanan perangkat.", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("hero-asa-assessment-storage-error"))
            if (feedback.isNotBlank()) Text(feedback, color = if (storageIssue) WorkspaceRed else WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("hero-asa-assessment-feedback"))
            Button(
                onClick = {
                    when (val result = repository?.saveDraft(child.id, answers.toList(), phase) ?: HeroAsaSaveResult.StorageError) {
                        is HeroAsaSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Draf ${phase.displayName.lowercase()} tersimpan di perangkat ini."; storageIssue = false }
                        HeroAsaSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        HeroAsaSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        is HeroAsaSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai." }
                        HeroAsaSaveResult.StorageError -> { storageIssue = true; feedback = "Draf belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp).testTag("hero-asa-assessment-save-draft"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) { Text("Simpan sebagai draf", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Button(
                onClick = {
                    if (answeredCount < HeroAsaInstrument.items.size) feedback = "Lengkapi semua butir sebelum menyelesaikan ${phase.displayName.lowercase()}."
                    else when (val result = repository?.complete(child.id, answers.toList(), phase) ?: HeroAsaSaveResult.StorageError) {
                        is HeroAsaSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai."; storageIssue = false }
                        HeroAsaSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        HeroAsaSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        is HeroAsaSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Jawaban tersimpan sebagai draf." }
                        HeroAsaSaveResult.StorageError -> { storageIssue = true; feedback = "Hasil belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp).testTag("hero-asa-assessment-complete"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) { Text("Selesaikan ${phase.displayName.lowercase()}", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun HeroAksiAssessmentPage(
    child: ChildRecord,
    repository: HeroAksiRepository?,
    phase: AssessmentPhase,
    initialAnswers: List<Int?>,
    navigationError: String?,
    onAnswersChanged: (List<Int?>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initialLoad = remember(child.id, repository, phase) { runCatching { repository?.get(child.id, phase) } }
    var assessment by remember(child.id, repository, phase) { mutableStateOf(initialLoad.getOrNull()) }
    var storageIssue by remember(child.id, repository, phase) { mutableStateOf(initialLoad.isFailure || repository == null) }
    val answers = remember(child.id, assessment?.id, phase) {
        mutableStateListOf<Int?>().apply {
            addAll(assessment?.answers ?: initialAnswers.takeIf { it.size == HeroAksiInstrument.items.size } ?: List(HeroAksiInstrument.items.size) { null })
        }
    }
    var feedback by remember(child.id, phase) { mutableStateOf("") }
    val answeredCount = answers.count { it != null }
    val completed = assessment?.isComplete == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("hero-aksi-assessment-scroll").padding(horizontal = 20.dp, vertical = 18.dp)) {
        NavigationBackButton(onClick = onBack, testTag = "hero-aksi-assessment-back")
        PageHeading("${phase.displayName} HERO Aksi", child.name)
        if (!navigationError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(navigationError, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("hero-aksi-assessment-navigation-error"))
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(HeroAksiInstrument.TITLE, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Skala 1–4", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Spacer(Modifier.height(8.dp))
                Text("1 Sangat tidak setuju  ·  2 Tidak setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Text("3 Setuju  ·  4 Sangat setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Terjawab $answeredCount dari 10", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, modifier = Modifier.testTag("hero-aksi-assessment-answered"))
        Spacer(Modifier.height(8.dp))
        HeroAksiInstrument.items.forEachIndexed { index, statement ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("hero-aksi-assessment-question-$index"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("${index + 1}. $statement", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..4).forEach { value ->
                            AssessmentAnswerChoice(value, answers[index] == value, !completed, Modifier.weight(1f).testTag("hero-aksi-assessment-answer-$index-$value")) {
                                answers[index] = value
                                onAnswersChanged(answers.toList())
                                feedback = ""
                            }
                        }
                    }
                }
            }
        }
        if (completed) {
            HeroAksiResultSummary("HASIL ${phase.displayName.uppercase()} HERO AKSI", assessment!!, "hero-aksi-assessment-result-score")
        } else {
            if (storageIssue) Text("Penyimpanan asesmen bermasalah. Periksa ruang penyimpanan perangkat.", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("hero-aksi-assessment-storage-error"))
            if (feedback.isNotBlank()) Text(feedback, color = if (storageIssue) WorkspaceRed else WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("hero-aksi-assessment-feedback"))
            Button(
                onClick = {
                    when (val result = repository?.saveDraft(child.id, answers.toList(), phase) ?: HeroAksiSaveResult.StorageError) {
                        is HeroAksiSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Draf ${phase.displayName.lowercase()} tersimpan di perangkat ini."; storageIssue = false }
                        HeroAksiSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        HeroAksiSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        is HeroAksiSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai." }
                        HeroAksiSaveResult.StorageError -> { storageIssue = true; feedback = "Draf belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp).testTag("hero-aksi-assessment-save-draft"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) { Text("Simpan sebagai draf", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Button(
                onClick = {
                    if (answeredCount < HeroAksiInstrument.items.size) feedback = "Lengkapi semua butir sebelum menyelesaikan ${phase.displayName.lowercase()}."
                    else when (val result = repository?.complete(child.id, answers.toList(), phase) ?: HeroAksiSaveResult.StorageError) {
                        is HeroAksiSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai."; storageIssue = false }
                        HeroAksiSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        HeroAksiSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        is HeroAksiSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Jawaban tersimpan sebagai draf." }
                        HeroAksiSaveResult.StorageError -> { storageIssue = true; feedback = "Hasil belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp).testTag("hero-aksi-assessment-complete"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) { Text("Selesaikan ${phase.displayName.lowercase()}", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }
}


@Composable
private fun HeroRasaAssessmentPage(
    child: ChildRecord,
    repository: HeroRasaRepository?,
    phase: AssessmentPhase,
    initialAnswers: List<Int?>,
    navigationError: String?,
    onAnswersChanged: (List<Int?>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initialLoad = remember(child.id, repository, phase) { runCatching { repository?.get(child.id, phase) } }
    var assessment by remember(child.id, repository, phase) { mutableStateOf(initialLoad.getOrNull()) }
    var storageIssue by remember(child.id, repository, phase) { mutableStateOf(initialLoad.isFailure || repository == null) }
    val answers = remember(child.id, assessment?.id, phase) {
        mutableStateListOf<Int?>().apply {
            addAll(assessment?.answers ?: initialAnswers.takeIf { it.size == HeroRasaInstrument.items.size } ?: List(HeroRasaInstrument.items.size) { null })
        }
    }
    var feedback by remember(child.id, phase) { mutableStateOf("") }
    val answeredCount = answers.count { it != null }
    val completed = assessment?.isComplete == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("hero-rasa-assessment-scroll").padding(horizontal = 20.dp, vertical = 18.dp)) {
        NavigationBackButton(onClick = onBack, testTag = "hero-rasa-assessment-back")
        PageHeading("${phase.displayName} HERO Rasa", child.name)
        if (!navigationError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(navigationError, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.testTag("hero-rasa-assessment-navigation-error"))
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(HeroRasaInstrument.TITLE, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Skala 1–4", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Spacer(Modifier.height(8.dp))
                Text("1 Sangat tidak setuju  ·  2 Tidak setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Text("3 Setuju  ·  4 Sangat setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Terjawab $answeredCount dari 10", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, modifier = Modifier.testTag("hero-rasa-assessment-answered"))
        Spacer(Modifier.height(8.dp))
        HeroRasaInstrument.items.forEachIndexed { index, statement ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("hero-rasa-assessment-question-$index"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("${index + 1}. $statement", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..4).forEach { value ->
                            AssessmentAnswerChoice(value, answers[index] == value, !completed, Modifier.weight(1f).testTag("hero-rasa-assessment-answer-$index-$value")) {
                                answers[index] = value
                                onAnswersChanged(answers.toList())
                                feedback = ""
                            }
                        }
                    }
                }
            }
        }
        if (completed) {
            HeroRasaResultSummary("HASIL ${phase.displayName.uppercase()} HERO RASA", assessment!!, "hero-rasa-assessment-result-score")
        } else {
            if (storageIssue) Text("Penyimpanan asesmen bermasalah. Periksa ruang penyimpanan perangkat.", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("hero-rasa-assessment-storage-error"))
            if (feedback.isNotBlank()) Text(feedback, color = if (storageIssue) WorkspaceRed else WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("hero-rasa-assessment-feedback"))
            Button(
                onClick = {
                    when (val result = repository?.saveDraft(child.id, answers.toList(), phase) ?: HeroRasaSaveResult.StorageError) {
                        is HeroRasaSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Draf ${phase.displayName.lowercase()} tersimpan di perangkat ini."; storageIssue = false }
                        HeroRasaSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        HeroRasaSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        is HeroRasaSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai." }
                        HeroRasaSaveResult.StorageError -> { storageIssue = true; feedback = "Draf belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp).testTag("hero-rasa-assessment-save-draft"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) { Text("Simpan sebagai draf", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Button(
                onClick = {
                    if (answeredCount < HeroRasaInstrument.items.size) feedback = "Lengkapi semua butir sebelum menyelesaikan ${phase.displayName.lowercase()}."
                    else when (val result = repository?.complete(child.id, answers.toList(), phase) ?: HeroRasaSaveResult.StorageError) {
                        is HeroRasaSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai."; storageIssue = false }
                        HeroRasaSaveResult.AlreadyCompleted -> { assessment = runCatching { repository?.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        HeroRasaSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        is HeroRasaSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Jawaban tersimpan sebagai draf." }
                        HeroRasaSaveResult.StorageError -> { storageIssue = true; feedback = "Hasil belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue && !completed,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp).testTag("hero-rasa-assessment-complete"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) { Text("Selesaikan ${phase.displayName.lowercase()}", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PaginationControls(
    tagPrefix: String,
    currentPage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
) {
    if (pageCount <= 1) return
    val visiblePages = when {
        pageCount <= 4 -> (0 until pageCount).toList()
        currentPage <= 1 -> (0..3).toList()
        currentPage >= pageCount - 2 -> (pageCount - 4 until pageCount).toList()
        else -> (currentPage - 1..currentPage + 2).toList()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("$tagPrefix-pagination"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            onClick = { onPageChange(currentPage - 1) },
            enabled = currentPage > 0,
            modifier = Modifier.size(40.dp)
                .semantics { contentDescription = "Halaman sebelumnya" }
                .testTag("$tagPrefix-previous-page"),
            shape = RoundedCornerShape(12.dp),
            color = if (currentPage > 0) Color(0xFFEAF3F9) else Color(0xFFF3F5F7),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", color = if (currentPage > 0) WorkspaceBlue else WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.size(5.dp))
        if (visiblePages.first() > 0) {
            Text("…", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 12.sp)
            Spacer(Modifier.size(5.dp))
        }
        visiblePages.forEachIndexed { index, page ->
            val selected = currentPage == page
            Surface(
                onClick = { onPageChange(page) },
                modifier = Modifier.size(38.dp)
                    .semantics { contentDescription = "Halaman ${page + 1}${if (selected) ", dipilih" else ""}" }
                    .testTag(if (selected) "$tagPrefix-page-indicator" else "$tagPrefix-page-${page + 1}"),
                shape = CircleShape,
                color = if (selected) WorkspaceRed else Color(0xFFF2F6FA),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${page + 1}", color = if (selected) Color.White else WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 11.sp)
                }
            }
            if (index < visiblePages.lastIndex) Spacer(Modifier.size(5.dp))
        }
        if (visiblePages.last() < pageCount - 1) {
            Spacer(Modifier.size(5.dp))
            Text("…", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 12.sp)
        }
        Spacer(Modifier.size(5.dp))
        Surface(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage < pageCount - 1,
            modifier = Modifier.size(40.dp)
                .semantics { contentDescription = "Halaman berikutnya" }
                .testTag("$tagPrefix-next-page"),
            shape = RoundedCornerShape(12.dp),
            color = if (currentPage < pageCount - 1) Color(0xFFEAF3F9) else Color(0xFFF3F5F7),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("›", color = if (currentPage < pageCount - 1) WorkspaceBlue else WorkspaceMuted, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun ChildrenPage(
    children: List<ChildRecord>,
    storageIssue: Boolean,
    birthDatePicker: BirthDatePicker,
    assessmentRepository: IdentityForeclosureRepository,
    onOpenChild: (String) -> Unit,
    onAdd: (String, LocalDate, ChildGender, Boolean) -> Boolean,
) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var gender by remember { mutableStateOf<ChildGender?>(null) }
    var involvedInScavenging by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val completedCount = remember(children.size, assessmentRepository) {
        val reads = children.map { child -> runCatching { assessmentRepository.get(child.id) } }
        if (reads.any { it.isFailure }) null else reads.count { it.getOrNull()?.isComplete == true }
    }
    val pageCount = ((children.size + ChildPageSize - 1) / ChildPageSize).coerceAtLeast(1)
    val visibleChildren = children.drop(currentPage * ChildPageSize).take(ChildPageSize)
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeading(if (adding) "Tambah anak" else "Anak", "")
        if (storageIssue) {
            Spacer(Modifier.height(10.dp))
            Text("Penyimpanan lokal bermasalah. Data baru belum dapat disimpan.", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 11.sp)
        }
        if (adding) {
            Spacer(Modifier.height(17.dp))
            Text("Nama anak", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80); error = "" },
                modifier = Modifier.fillMaxWidth().testTag("child-name-field"),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("Nama lengkap", fontFamily = WorkspacePoppins, fontSize = 12.sp) },
            )
            Spacer(Modifier.height(13.dp))
            Text("Tanggal lahir", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            Button(
                onClick = {
                    birthDatePicker.show(birthDate) { selectedDate ->
                        birthDate = selectedDate
                        error = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("child-birth-date"),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WorkspaceInk),
            ) {
                Text(birthDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", IndonesianLocale)) ?: "Pilih tanggal lahir", fontFamily = WorkspacePoppins)
            }
            Spacer(Modifier.height(8.dp))
            Text("Nama dan tanggal lahir disimpan terenkripsi di perangkat ini.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            Text("Jenis kelamin", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChildChoice("Laki-laki", gender == ChildGender.Boy, "child-gender-boy", Modifier.weight(1f)) { gender = ChildGender.Boy }
                ChildChoice("Perempuan", gender == ChildGender.Girl, "child-gender-girl", Modifier.weight(1f)) { gender = ChildGender.Girl }
            }
            Spacer(Modifier.height(12.dp))
            Text("Apakah anak terlibat memulung?", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChildChoice("Ya", involvedInScavenging == true, "child-scavenging-yes", Modifier.weight(1f)) { involvedInScavenging = true }
                ChildChoice("Tidak", involvedInScavenging == false, "child-scavenging-no", Modifier.weight(1f)) { involvedInScavenging = false }
            }
            Text("Pilihan ini mengikuti kategori yang digunakan dataset HERO-ML.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 11.sp, modifier = Modifier.testTag("child-form-error"))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    error = when {
                        name.trim().length < 2 -> "Nama anak minimal 2 karakter."
                        birthDate == null -> "Pilih tanggal lahir terlebih dahulu."
                        gender == null -> "Pilih jenis kelamin anak."
                        involvedInScavenging == null -> "Pilih keterlibatan memulung anak."
                        onAdd(name, birthDate!!, gender!!, involvedInScavenging!!) -> {
                            currentPage = (children.lastIndex.coerceAtLeast(0) / ChildPageSize)
                            name = ""; birthDate = null; gender = null; involvedInScavenging = null; adding = false; ""
                        }
                        else -> "Data anak belum dapat disimpan. Periksa kembali isianmu."
                    }
                },
                enabled = !storageIssue,
                modifier = Modifier.fillMaxWidth().height(49.dp).testTag("child-save"),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) { Text("Simpan data anak", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(5.dp))
            androidx.compose.material3.TextButton(onClick = { adding = false; error = "" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Batal", color = WorkspaceMuted, fontFamily = WorkspacePoppins)
            }
            return@Column
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(children.size.toString(), "Total anak", Modifier.weight(1f), Color(0xFFEAF3F9), WorkspaceBlue)
            SummaryCard(completedCount?.toString() ?: "—", "Pre-test selesai", Modifier.weight(1f), Color(0xFFFFF0EC), WorkspaceRed)
        }
        Spacer(Modifier.height(18.dp))
        if (children.isEmpty()) {
            EmptyStateCard("Mulai dengan satu anak", "Tambahkan nama dan tanggal lahir untuk memulai catatan pendampingan.", "Data anak hanya disimpan di perangkat ini.")
        } else {
            visibleChildren.forEach { child ->
                ChildCard(child, assessmentRepository, onClick = { onOpenChild(child.id) }, testTagPrefix = "child-card")
            }
            PaginationControls("children", currentPage, pageCount) { page ->
                currentPage = page
                coroutineScope.launch { scrollState.animateScrollTo(0) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { adding = true; name = ""; birthDate = null; gender = null; involvedInScavenging = null; error = "" },
            enabled = !storageIssue,
            modifier = Modifier.fillMaxWidth().height(49.dp).testTag("child-add"),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
        ) { Text("Tambah anak", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ChildChoice(label: String, isSelected: Boolean, testTag: String, modifier: Modifier = Modifier, onSelected: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (isSelected) Color(0xFFEAF3F9) else Color.White)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelected)
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(label, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontSize = 10.sp)
    }
}

@Composable
private fun ChildCard(
    child: ChildRecord,
    assessmentRepository: IdentityForeclosureRepository,
    onClick: (() -> Unit)? = null,
    testTagPrefix: String = "search-child-card",
) {
    val assessmentLoad = remember(child.id, assessmentRepository) { runCatching { assessmentRepository.get(child.id) } }
    val assessment = assessmentLoad.getOrNull()
    val assessmentStatus = when {
        assessmentLoad.isFailure -> "Status asesmen tidak dapat dibaca."
        assessment == null -> "Belum dimulai"
        !assessment.isComplete -> "Draf · ${assessment.answers.count { it != null }} dari ${IdentityForeclosureInstrument.items.size} butir"
        else -> {
            val category = assessment.category?.label?.lowercase()
            val score = assessment.totalScore
            if (category == null || score == null) "Hasil pre-test tidak lengkap"
            else "Kecenderungan identity foreclosure $category · $score/60"
        }
    }
    val assessmentStatusColor = when {
        assessmentLoad.isFailure || assessment == null || !assessment.isComplete -> WorkspaceMuted
        assessment.category == IdentityForeclosureCategory.HIGH -> WorkspaceRed
        assessment.category == IdentityForeclosureCategory.MEDIUM -> Color(0xFF946000)
        else -> Color(0xFF28724C)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .then(onClick?.let { Modifier.clickable(role = Role.Button, onClick = it).testTag("$testTagPrefix-${child.id}") } ?: Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEAF3F9)), contentAlignment = Alignment.Center) {
                Text(child.name.trim().firstOrNull()?.uppercase() ?: "A", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(child.name, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Lahir ${child.birthDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", IndonesianLocale))}", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                Text(
                    "${child.gender?.displayName ?: "Jenis kelamin belum diisi"} · Memulung: ${child.involvedInScavenging?.let { if (it) "Ya" else "Tidak" } ?: "belum diisi"}",
                    color = WorkspaceMuted,
                    fontFamily = WorkspacePoppins,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text("Pre-test identitas", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp)
                Text(
                    assessmentStatus,
                    color = assessmentStatusColor,
                    fontFamily = WorkspacePoppins,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    modifier = Modifier.testTag("child-assessment-status-${child.id}"),
                )
                if (onClick != null) {
                    Spacer(Modifier.height(7.dp))
                    Text("Lihat statistik & rekomendasi ›", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun AssessmentOcrReviewPage(
    child: ChildRecord,
    instrumentTitle: String,
    phase: AssessmentPhase,
    itemStatements: List<String>,
    importedOcr: org.heroism.herocompass.assessment.IdentityOcrResult,
    initialAnswers: List<Int?>,
    confirmed: Boolean,
    onAnswersChanged: (List<Int?>) -> Unit,
    onConfirmed: (Boolean) -> Unit,
    onSaveDraft: (List<Int?>) -> Boolean,
    onComplete: (List<Int?>) -> Boolean,
    onDiscard: () -> Unit,
) {
    BackHandler(onBack = onDiscard)
    val answers = remember(importedOcr, child.id, phase) {
        mutableStateListOf<Int?>().apply {
            addAll(initialAnswers.takeIf { it.size == itemStatements.size } ?: List(itemStatements.size) { null })
        }
    }
    var feedback by remember(importedOcr) { mutableStateOf("") }
    val answeredCount = answers.count { it != null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("assessment-ocr-review-scroll").padding(horizontal = 20.dp, vertical = 18.dp)) {
        PageHeading("Periksa hasil OCR", "${child.name} · ${phase.displayName}")
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(instrumentTitle, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = importedOcr.previewBitmap.asImageBitmap(),
                    contentDescription = "Foto formulir yang dipindai",
                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp)).testTag("assessment-ocr-review-preview"),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(8.dp))
                Text("Halaman ${importedOcr.sourcePage} · ${importedOcr.detectedAnswerCount} dari ${itemStatements.size} pilihan terbaca. Pilihan kosong dibiarkan kosong agar tidak menebak.", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = confirmed, onCheckedChange = onConfirmed, modifier = Modifier.testTag("assessment-ocr-review-confirm"))
                    Text("Saya sudah mencocokkan semua jawaban dengan foto.", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
                }
                Text("Koreksi skala 1–4 di bawah sebelum menyimpan.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                androidx.compose.material3.TextButton(onClick = onDiscard, modifier = Modifier.testTag("assessment-ocr-review-discard")) {
                    Text("Buang scan", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Terjawab $answeredCount dari ${itemStatements.size}", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, modifier = Modifier.testTag("assessment-ocr-review-count"))
        Spacer(Modifier.height(8.dp))
        itemStatements.forEachIndexed { index, statement ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("${index + 1}. $statement", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..4).forEach { value ->
                            AssessmentAnswerChoice(
                                value = value,
                                selected = answers[index] == value,
                                enabled = true,
                                modifier = Modifier.weight(1f).testTag("assessment-ocr-answer-$index-$value"),
                            ) {
                                answers[index] = value
                                onAnswersChanged(answers.toList())
                                feedback = ""
                            }
                        }
                    }
                }
            }
        }
        if (feedback.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(feedback, color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("assessment-ocr-review-feedback"))
        }
        Button(
            onClick = { feedback = if (!confirmed) "Cocokkan jawaban dengan foto dan centang konfirmasi terlebih dahulu." else if (onSaveDraft(answers.toList())) "" else "Draf belum tersimpan. Periksa penyimpanan perangkat lalu coba lagi." },
            enabled = confirmed,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp).testTag("assessment-ocr-review-save-draft"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
        ) { Text("Simpan sebagai draf", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        Button(
            onClick = {
                feedback = when {
                    !confirmed -> "Cocokkan jawaban dengan foto dan centang konfirmasi terlebih dahulu."
                    answeredCount < itemStatements.size -> "Lengkapi jawaban kosong secara manual sebelum menyelesaikan ${phase.displayName.lowercase()}."
                    onComplete(answers.toList()) -> ""
                    else -> "Hasil belum tersimpan. Periksa data atau coba lagi."
                }
            },
            enabled = confirmed && answeredCount == itemStatements.size,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp).testTag("assessment-ocr-review-complete"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
        ) { Text("Selesaikan ${phase.displayName.lowercase()}", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IdentityForeclosureAssessmentPage(
    child: ChildRecord,
    repository: IdentityForeclosureRepository,
    phase: AssessmentPhase,
    initialAnswers: List<Int?>,
    importedOcr: org.heroism.herocompass.assessment.IdentityOcrResult?,
    ocrImportVersion: Int,
    ocrReviewConfirmed: Boolean,
    navigationError: String?,
    onAnswersChanged: (List<Int?>) -> Unit,
    onOcrReviewConfirmed: (Boolean) -> Unit,
    onOcrSaved: () -> Unit,
    onDiscardOcr: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initialLoad = remember(child.id, repository, phase) { runCatching { repository.get(child.id, phase) } }
    var assessment by remember(child.id, repository, phase) { mutableStateOf(initialLoad.getOrNull()) }
    var storageIssue by remember(child.id, repository, phase) { mutableStateOf(initialLoad.isFailure) }
    val answers = remember(child.id, assessment?.id, phase, ocrImportVersion) {
        mutableStateListOf<Int?>().apply {
            addAll(assessment?.answers ?: initialAnswers.takeIf { it.size == IdentityForeclosureInstrument.items.size }
                ?: List(IdentityForeclosureInstrument.items.size) { null })
        }
    }
    var feedback by remember(child.id, phase) { mutableStateOf("") }
    val answeredCount = answers.count { it != null }
    val completed = assessment?.isComplete == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("assessment-scroll-container").padding(horizontal = 20.dp, vertical = 20.dp)) {
        NavigationBackButton(onClick = onBack, testTag = "assessment-back")
        PageHeading("${phase.displayName} identitas", child.name)
        if (!navigationError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                navigationError,
                color = WorkspaceRed,
                fontFamily = WorkspacePoppins,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.testTag("assessment-navigation-error"),
            )
        }
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(IdentityForeclosureInstrument.TITLE, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Skala 1–4", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Spacer(Modifier.height(9.dp))
                Text("1 Sangat tidak setuju   ·   2 Tidak setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
                Text("3 Setuju   ·   4 Sangat setuju", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (importedOcr != null) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("Periksa hasil scan", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(Modifier.height(7.dp))
                    Image(
                        bitmap = importedOcr.previewBitmap.asImageBitmap(),
                        contentDescription = "Pratinjau formulir hasil scan",
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).testTag("assessment-ocr-preview"),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("Halaman ${importedOcr.sourcePage} · ${importedOcr.detectedAnswerCount} dari ${IdentityForeclosureInstrument.items.size} pilihan terbaca. Jawaban kosong atau meragukan tetap perlu diisi manual.", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = ocrReviewConfirmed, onCheckedChange = onOcrReviewConfirmed, modifier = Modifier.testTag("assessment-ocr-review-confirm"))
                        Text("Saya sudah membandingkan semua jawaban dengan foto.", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                    Text("Jawaban 1–4 di bawah masih bisa dikoreksi sebelum disimpan.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(7.dp))
                    androidx.compose.material3.TextButton(onClick = onDiscardOcr, modifier = Modifier.testTag("assessment-ocr-discard")) {
                        Text("Buang scan", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Terjawab $answeredCount dari ${IdentityForeclosureInstrument.items.size}", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        IdentityForeclosureInstrument.items.forEachIndexed { index, statement ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("assessment-question-$index"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("${index + 1}. $statement", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..4).forEach { value ->
                            AssessmentAnswerChoice(
                                value = value,
                                selected = answers[index] == value,
                                enabled = !completed,
                                modifier = Modifier.weight(1f).testTag("assessment-answer-$index-$value"),
                            ) {
                                answers[index] = value
                                onAnswersChanged(answers.toList())
                                feedback = ""
                            }
                        }
                    }
                }
            }
        }
        if (assessment?.isComplete == true) {
            AssessmentResultCard(assessment!!)
        } else {
            if (storageIssue) {
                Text("Penyimpanan asesmen bermasalah. Periksa ruang penyimpanan perangkat.", color = WorkspaceRed, fontFamily = WorkspacePoppins, fontSize = 10.sp)
            }
            if (feedback.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(feedback, color = if (storageIssue) WorkspaceRed else WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("assessment-feedback"))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (importedOcr != null && !ocrReviewConfirmed) {
                        feedback = "Bandingkan semua jawaban dengan foto dan centang konfirmasi sebelum menyimpan."
                        return@Button
                    }
                    when (val result = repository.saveDraft(child.id, answers.toList(), phase)) {
                        is AssessmentSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Draf ${phase.displayName.lowercase()} tersimpan di perangkat ini."; storageIssue = false; onOcrSaved() }
                        AssessmentSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                        AssessmentSaveResult.AlreadyCompleted -> { assessment = runCatching { repository.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                        is AssessmentSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} sudah diselesaikan." }
                        AssessmentSaveResult.StorageError -> { storageIssue = true; feedback = "Draf belum dapat disimpan." }
                    }
                },
                enabled = !storageIssue,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("assessment-save-draft"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF3F9), contentColor = WorkspaceBlue),
            ) { Text("Simpan sebagai draf", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (importedOcr != null && !ocrReviewConfirmed) {
                        feedback = "Bandingkan semua jawaban dengan foto dan centang konfirmasi sebelum menyelesaikan."
                    } else if (answeredCount < IdentityForeclosureInstrument.items.size) {
                        feedback = "Lengkapi semua butir sebelum menyelesaikan ${phase.displayName.lowercase()}."
                    } else {
                        when (val result = repository.complete(child.id, answers.toList(), phase)) {
                            is AssessmentSaveResult.Completed -> { assessment = result.assessment; feedback = "${phase.displayName} selesai."; storageIssue = false; onOcrSaved() }
                            AssessmentSaveResult.AlreadyCompleted -> { assessment = runCatching { repository.get(child.id, phase) }.getOrNull(); feedback = "${phase.displayName} ini sudah diselesaikan." }
                            AssessmentSaveResult.InvalidAnswers -> feedback = "Jawaban tidak valid. Periksa kembali pilihanmu."
                            is AssessmentSaveResult.SavedDraft -> { assessment = result.assessment; feedback = "Jawaban tersimpan sebagai draf." }
                            AssessmentSaveResult.StorageError -> { storageIssue = true; feedback = "Hasil belum dapat disimpan." }
                        }
                    }
                },
                enabled = !storageIssue,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("assessment-complete"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
            ) { Text("Selesaikan ${phase.displayName.lowercase()}", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AssessmentAnswerChoice(value: Int, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) WorkspaceBlue else Color(0xFFF3F5F8),
        contentColor = if (selected) Color.White else WorkspaceInk,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(value.toString(), fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AssessmentResultCard(assessment: IdentityForeclosureAssessment) {
    val category = assessment.category ?: return
    val score = assessment.totalScore ?: return
    val categoryColor = if (category == IdentityForeclosureCategory.LOW) Color(0xFF28724C) else WorkspaceBlue
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Hasil ${assessment.phase.displayName.lowercase()}", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text("$score dari 60", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("Kecenderungan identity foreclosure ${category.label.lowercase()}", color = categoryColor, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Text("Kategori mengikuti panduan instrumen. Gunakan hasil ini sebagai bahan tinjauan pendamping.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ProfilePage(
    name: String,
    backupCoordinator: BackupCoordinator?,
    avatarFile: File?,
    avatarVersion: Int,
    biometricEnabled: Boolean,
    biometricFeedback: String,
    onAvatarSelected: (Uri) -> Boolean,
    onEnableBiometric: () -> Unit,
    onDisableBiometric: () -> Unit,
    onLock: () -> Unit,
    onBackupImported: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupPassphrase by remember { mutableStateOf("") }
    var backupConfirmation by remember { mutableStateOf("") }
    var pendingExportPassphrase by remember { mutableStateOf("") }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupFeedback by remember { mutableStateOf("") }
    var photoFeedback by remember { mutableStateOf("") }
    val avatar = remember(avatarFile?.absolutePath, avatarVersion) {
        avatarFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoFeedback = if (onAvatarSelected(uri)) "Foto profil diperbarui." else "Foto tidak dapat dibuka. Pilih gambar lain."
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri == null) {
            pendingExportPassphrase = ""
        } else if (backupCoordinator == null) {
            backupFeedback = "Fitur cadangan belum tersedia."
        } else {
            val password = pendingExportPassphrase.toCharArray()
            pendingExportPassphrase = ""
            backupBusy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    when (val export = backupCoordinator.export(password)) {
                        is BackupOperationResult.Exported -> runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { it.write(export.bytes) }
                                ?: error("Berkas tidak dapat ditulis.")
                            "Cadangan tersimpan: ${export.childCount} anak, ${export.assessmentCount} tes, ${export.interventionCount} tindak lanjut."
                        }.getOrElse { "Cadangan gagal ditulis. Pilih lokasi lain." }
                        BackupOperationResult.InvalidPassphrase -> "Kata sandi minimal 12 karakter."
                        else -> "Cadangan gagal dibuat. Periksa penyimpanan lalu coba lagi."
                    }
                }
                password.fill('\u0000')
                backupFeedback = result
                backupBusy = false
            }
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            restoreUri = uri
            backupPassphrase = ""
            showRestoreDialog = true
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
        PageHeading("Profil pendamping", "")
        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFEAF3F9)), contentAlignment = Alignment.Center) {
                    if (avatar != null) Image(avatar.asImageBitmap(), "Foto profil $name", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    else Text(name.trim().firstOrNull()?.uppercase() ?: "H", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                }
                Spacer(Modifier.size(13.dp))
                Column {
                    Text(name, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Pendamping HEROISM", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                    TextButton(onClick = { avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.testTag("mentor-avatar-pick")) {
                        Text(if (avatar == null) "Tambah foto" else "Ubah foto", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp)
                    }
                }
            }
        }
        if (photoFeedback.isNotBlank()) Text(photoFeedback, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Masuk dengan biometrik", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Pakai sidik jari atau wajah. PIN tetap tersedia.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 15.sp)
                }
                Switch(checked = biometricEnabled, onCheckedChange = { enabled -> if (enabled) onEnableBiometric() else onDisableBiometric() }, modifier = Modifier.testTag("mentor-biometric-switch"))
            }
            if (biometricFeedback.isNotBlank()) Text(biometricFeedback, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.padding(start = 17.dp, end = 17.dp, bottom = 12.dp).testTag("mentor-biometric-feedback"))
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Text("Privasi dan penyimpanan", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Data tersimpan di perangkat. Cadangan dibuat hanya saat kamu memilihnya.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.testTag("backup-card")) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Text("Cadangan data", color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pindahkan data ke perangkat baru. Pemulihan hanya untuk profil kosong.",
                    color = WorkspaceMuted,
                    fontFamily = WorkspacePoppins,
                    fontSize = 10.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text("PIN tidak ikut disalin. Simpan kata sandi minimal 12 karakter; tanpa itu cadangan tidak bisa dibuka.", color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { backupPassphrase = ""; backupConfirmation = ""; showExportDialog = true },
                    enabled = backupCoordinator != null && !backupBusy,
                    modifier = Modifier.fillMaxWidth().height(45.dp).testTag("backup-export"),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WorkspaceBlue),
                ) { Text("Simpan cadangan", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
                Spacer(Modifier.height(7.dp))
                TextButton(
                    onClick = { openBackupLauncher.launch(arrayOf("application/octet-stream", "application/vnd.menyalahero.backup", "*/*")) },
                    enabled = backupCoordinator != null && !backupBusy,
                    modifier = Modifier.fillMaxWidth().testTag("backup-restore"),
                ) { Text("Pulihkan dari file", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
                if (backupBusy) {
                    Text("Memproses cadangan…", color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 10.sp, modifier = Modifier.testTag("backup-progress"))
                }
                if (backupFeedback.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(backupFeedback, color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.testTag("backup-feedback"))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLock, modifier = Modifier.fillMaxWidth().height(49.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = WorkspaceRed)) {
            Text("Kunci aplikasi", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!backupBusy) { showExportDialog = false; backupPassphrase = ""; backupConfirmation = "" } },
            title = { Text("Lindungi cadangan", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Buat kata sandi minimal 12 karakter. Gunakan kata sandi yang sama saat memulihkan data.", fontFamily = WorkspacePoppins, fontSize = 11.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it.take(128) },
                        label = { Text("Kata sandi cadangan") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.testTag("backup-passphrase"),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = backupConfirmation,
                        onValueChange = { backupConfirmation = it.take(128) },
                        label = { Text("Ulangi kata sandi") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.testTag("backup-passphrase-confirm"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExportPassphrase = backupPassphrase
                        showExportDialog = false
                        backupPassphrase = ""
                        backupConfirmation = ""
                        createBackupLauncher.launch("Menyala-HERO-backup-${LocalDate.now()}.mhbk")
                    },
                    enabled = backupPassphrase.length >= PortableBackupCrypto.MIN_PASSPHRASE_LENGTH && backupPassphrase == backupConfirmation && !backupBusy,
                ) { Text("Pilih lokasi", fontFamily = WorkspacePoppins) }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false; backupPassphrase = ""; backupConfirmation = "" }) { Text("Batal", fontFamily = WorkspacePoppins) } },
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!backupBusy) { showRestoreDialog = false; backupPassphrase = ""; restoreUri = null } },
            title = { Text("Pulihkan data", fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Masukkan kata sandi. Pemulihan hanya dapat dilakukan ke profil yang belum berisi data anak.", fontFamily = WorkspacePoppins, fontSize = 11.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it.take(128) },
                        label = { Text("Kata sandi cadangan") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.testTag("backup-restore-passphrase"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = restoreUri
                        val password = backupPassphrase.toCharArray()
                        backupPassphrase = ""
                        if (uri != null && backupCoordinator != null) {
                            backupBusy = true
                            showRestoreDialog = false
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                                            val output = java.io.ByteArrayOutputStream()
                                            val buffer = ByteArray(8192)
                                            var total = 0
                                            while (true) {
                                                val count = input.read(buffer)
                                                if (count < 0) break
                                                total += count
                                                require(total <= PortableBackupCrypto.MAX_BACKUP_FILE_BYTES) { "Backup terlalu besar." }
                                                output.write(buffer, 0, count)
                                            }
                                            output.toByteArray()
                                        } ?: error("File backup tidak dapat dibaca.")
                                        backupCoordinator.restore(bytes, password)
                                    }.getOrElse { BackupOperationResult.InvalidBackup }
                                }
                                password.fill('\u0000')
                                backupFeedback = when (result) {
                                    is BackupOperationResult.Imported -> "Data dipulihkan: ${result.childCount} anak, ${result.assessmentCount} tes, ${result.interventionCount} tindak lanjut."
                                    BackupOperationResult.InvalidPassphrase -> "Kata sandi minimal 12 karakter."
                                    BackupOperationResult.InvalidBackup -> "File cadangan tidak valid atau belum didukung."
                                    BackupOperationResult.IncorrectPassphraseOrDamagedFile -> "Kata sandi salah atau file cadangan rusak."
                                    BackupOperationResult.DestinationNotEmpty -> "Pulihkan ke profil lokal yang belum memiliki data anak."
                                    BackupOperationResult.StorageError -> "Data belum dapat dipulihkan. Periksa penyimpanan lalu coba lagi."
                                    is BackupOperationResult.Exported -> ""
                                }
                                backupBusy = false
                                if (result is BackupOperationResult.Imported) onBackupImported()
                            }
                        }
                    },
                    enabled = backupPassphrase.length >= PortableBackupCrypto.MIN_PASSPHRASE_LENGTH && !backupBusy,
                ) { Text("Pulihkan", fontFamily = WorkspacePoppins) }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false; backupPassphrase = ""; restoreUri = null }) { Text("Batal", fontFamily = WorkspacePoppins) } },
        )
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
    if (subtitle.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun EmptyStateCard(title: String, description: String, note: String) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(66.dp).clip(CircleShape).background(Color(0xFFEAF3F9)), contentAlignment = Alignment.Center) {
                WorkspaceIcon(WorkspaceTab.Children, true, Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = WorkspaceInk, fontFamily = WorkspacePoppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(description, color = WorkspaceMuted, fontFamily = WorkspacePoppins, fontSize = 10.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
            if (note.isNotBlank()) {
                Spacer(Modifier.height(15.dp))
                Text(note, color = WorkspaceBlue, fontFamily = WorkspacePoppins, fontWeight = FontWeight.Medium, fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun WorkspaceIcon(tab: WorkspaceTab, selected: Boolean, modifier: Modifier = Modifier) {
    val color = if (selected) Color.White else WorkspaceMuted
    val icon = when (tab) {
        WorkspaceTab.Home -> R.drawable.ic_tab_home
        WorkspaceTab.Search -> R.drawable.ic_tab_search
        WorkspaceTab.Children -> R.drawable.ic_tab_children
        WorkspaceTab.Guide -> R.drawable.ic_tab_guide
        WorkspaceTab.Profile -> R.drawable.ic_tab_profile
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(22.dp),
    )
}
