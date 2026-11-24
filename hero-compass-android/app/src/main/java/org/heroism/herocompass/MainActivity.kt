package org.heroism.herocompass

import android.os.Bundle
import android.os.CancellationSignal
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import javax.crypto.Cipher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.heroism.herocompass.auth.AuthFailure
import org.heroism.herocompass.auth.AuthResult
import org.heroism.herocompass.auth.MentorAuthRepository
import org.heroism.herocompass.children.EncryptedChildrenRepository
import org.heroism.herocompass.children.AndroidBirthDatePicker
import org.heroism.herocompass.assessment.EncryptedIdentityForeclosureRepository
import org.heroism.herocompass.assessment.IdentityForeclosureRepository
import org.heroism.herocompass.assessment.EncryptedFutureOrientationRepository
import org.heroism.herocompass.assessment.FutureOrientationRepository
import org.heroism.herocompass.assessment.EncryptedHeroAsaRepository
import org.heroism.herocompass.assessment.HeroAsaRepository
import org.heroism.herocompass.assessment.EncryptedHeroAksiRepository
import org.heroism.herocompass.assessment.HeroAksiRepository
import org.heroism.herocompass.assessment.EncryptedHeroRasaRepository
import org.heroism.herocompass.assessment.HeroRasaRepository
import org.heroism.herocompass.intervention.EncryptedInterventionRepository
import org.heroism.herocompass.backup.BackupCoordinator

private val HeroRed = Color(0xFFB72929)
private val HeroBlue = Color(0xFF004772)
private val HeroSky = Color(0xFF5E9FCA)
private val Ink = Color(0xFF343434)
private val Muted = Color(0xFF777985)
private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = HeroBlue.toArgb()
        window.navigationBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        val authRepository = MentorAuthRepository(applicationContext)
        val childrenRepository = EncryptedChildrenRepository(applicationContext)
        val assessmentRepository = EncryptedIdentityForeclosureRepository(applicationContext)
        val futureOrientationRepository = EncryptedFutureOrientationRepository(applicationContext)
        val heroAsaRepository = EncryptedHeroAsaRepository(applicationContext)
        val heroAksiRepository = EncryptedHeroAksiRepository(applicationContext)
        val heroRasaRepository = EncryptedHeroRasaRepository(applicationContext)
        val interventionRepository = EncryptedInterventionRepository(applicationContext)
        val backupCoordinator = BackupCoordinator(
            childrenRepository,
            assessmentRepository,
            futureOrientationRepository,
            heroAsaRepository,
            heroAksiRepository,
            heroRasaRepository,
            interventionRepository,
        )
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = HeroRed, background = Color(0xFFF7F8FC)), typography = MaterialTheme.typography) {
                MentorAuthFlow(authRepository, childrenRepository, AndroidBirthDatePicker(this@MainActivity), assessmentRepository, futureOrientationRepository, heroAsaRepository, heroAksiRepository, heroRasaRepository, interventionRepository, backupCoordinator, this@MainActivity)
            }
        }
    }

    internal fun promptBiometric(cipher: Cipher, purpose: String, onSuccess: (Cipher?) -> Unit, onError: (String) -> Unit) {
        val manager = getSystemService(BIOMETRIC_SERVICE) as BiometricManager
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometrik belum siap. Periksa pengaturan perangkat.")
            return
        }
        runCatching {
            val prompt = BiometricPrompt.Builder(this)
                .setTitle("$purpose Menyala-HERO")
                .setSubtitle("Gunakan biometrik perangkat")
                .setNegativeButton("Batal", mainExecutor) { dialog, _ -> dialog.dismiss() }
                .build()
            prompt.authenticate(
                BiometricPrompt.CryptoObject(cipher),
                CancellationSignal(),
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess(result.cryptoObject?.cipher)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onError("Autentikasi dibatalkan. Gunakan PIN untuk masuk.")
                    }
                },
            )
        }.onFailure { onError("Biometrik tidak tersedia. Gunakan PIN untuk masuk.") }
    }
}

private enum class AuthScreen { Splash, Welcome, SignIn, SignUp, SignedIn }

@Composable
private fun MentorAuthFlow(repository: MentorAuthRepository, childrenRepository: EncryptedChildrenRepository, birthDatePicker: AndroidBirthDatePicker, assessmentRepository: IdentityForeclosureRepository, futureOrientationRepository: FutureOrientationRepository, heroAsaRepository: HeroAsaRepository, heroAksiRepository: HeroAksiRepository, heroRasaRepository: HeroRasaRepository, interventionRepository: EncryptedInterventionRepository, backupCoordinator: BackupCoordinator, activity: MainActivity) {
    var screen by remember { mutableStateOf(AuthScreen.Splash) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mentor by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(repository.isBiometricEnabled()) }
    var biometricFeedback by remember { mutableStateOf("") }

    fun startBiometricLogin() {
        val cipher = runCatching { repository.biometricCipherForLogin() }.getOrElse {
            repository.disableBiometric()
            biometricEnabled = false
            error = "Biometrik perlu diatur ulang. Masuk dengan PIN."
            return
        }
        activity.promptBiometric(cipher, "Masuk ke", onSuccess = { authenticatedCipher ->
            when (val result = authenticatedCipher?.let(repository::authenticateBiometric)) {
                is AuthResult.Success -> { mentor = result.profile.name; error = ""; screen = AuthScreen.SignedIn }
                else -> error = "Biometrik gagal. Masuk dengan PIN."
            }
        }, onError = { error = it })
    }

    fun startBiometricEnrollment() {
        val cipher = runCatching { repository.biometricCipherForEnrollment() }.getOrElse {
            biometricFeedback = "Aktifkan biometrik perangkat terlebih dahulu."
            return
        }
        activity.promptBiometric(cipher, "Aktifkan biometrik untuk", onSuccess = { authenticatedCipher ->
            biometricEnabled = authenticatedCipher?.let(repository::enableBiometric) == true
            biometricFeedback = if (biometricEnabled) "Biometrik aktif. PIN tetap bisa digunakan." else "Biometrik gagal diaktifkan."
        }, onError = { biometricFeedback = it })
    }

    when (screen) {
        AuthScreen.Splash -> SplashScreen(onComplete = { screen = AuthScreen.Welcome })
        AuthScreen.Welcome -> WelcomeScreen(
            onSignIn = { screen = AuthScreen.SignIn },
            onSignUp = { screen = AuthScreen.SignUp },
        )
        AuthScreen.SignIn -> AuthFormScreen(
            title = "Selamat datang,",
            subtitle = "Masuk kembali untuk melanjutkan",
            showName = false,
            name = name,
            phone = phone,
            pin = pin,
            error = error,
            onNameChange = { name = it },
            onPhoneChange = { phone = it.filter(Char::isDigit).take(15) },
            onPinChange = { pin = it.filter(Char::isDigit).take(6) },
            onBack = { error = ""; screen = AuthScreen.Welcome },
            onSubmit = {
                when (val result = repository.authenticate(phone, pin)) {
                    is AuthResult.Success -> { mentor = result.profile.name; error = ""; screen = AuthScreen.SignedIn }
                    is AuthResult.Failure -> error = authMessage(result.reason)
                }
            },
            buttonText = "Masuk",
            isSignUp = false,
            biometricEnabled = biometricEnabled,
            onBiometricLogin = ::startBiometricLogin,
            footerText = "Belum punya profil? Buat profil lokal",
            onFooter = { error = ""; screen = AuthScreen.SignUp },
        )
        AuthScreen.SignUp -> AuthFormScreen(
            title = "Selamat datang,",
            subtitle = "Ayo buat akun pendamping",
            showName = true,
            name = name,
            phone = phone,
            pin = pin,
            error = error,
            onNameChange = { name = it },
            onPhoneChange = { phone = it.filter(Char::isDigit).take(15) },
            onPinChange = { pin = it.filter(Char::isDigit).take(6) },
            onBack = { error = ""; screen = AuthScreen.Welcome },
            onSubmit = {
                when (val result = repository.createProfile(name, phone, pin)) {
                    is AuthResult.Success -> { mentor = result.profile.name; error = ""; screen = AuthScreen.SignedIn }
                    is AuthResult.Failure -> error = authMessage(result.reason)
                }
            },
            buttonText = "Buat profil lokal",
            isSignUp = true,
            footerText = "Sudah punya profil? Masuk",
            onFooter = { error = ""; screen = AuthScreen.SignIn },
        )
        AuthScreen.SignedIn -> WorkspaceShell(
            mentor, childrenRepository, birthDatePicker, assessmentRepository, futureOrientationRepository, heroAsaRepository, heroAksiRepository, heroRasaRepository, interventionRepository, backupCoordinator,
            mentorAuthRepository = repository,
            biometricEnabled = biometricEnabled,
            biometricFeedback = biometricFeedback,
            onEnableBiometric = ::startBiometricEnrollment,
            onDisableBiometric = { repository.disableBiometric(); biometricEnabled = false; biometricFeedback = "Biometrik dinonaktifkan." },
        ) {
            pin = ""
            screen = AuthScreen.SignIn
        }
    }
}

@Composable
private fun WelcomeScreen(onSignIn: () -> Unit, onSignUp: () -> Unit) {
    SetStatusBarIconStyle(lightBackground = false)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(HeroBlue, HeroSky)))) {
        Image(painterResource(R.drawable.hero_pattern), null, Modifier.fillMaxSize().alpha(.18f), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Image(painterResource(R.drawable.menyala_hero_logo), "Menyala HERO", Modifier.width(218.dp).height(130.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(24.dp))
            Text("“Setiap langkah kecil adalah bagian dari pertumbuhan.”", color = Color.White, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 29.sp, textAlign = TextAlign.Center)
//            Spacer(Modifier.height(10.dp))
//            Text("Catat pre-test dan post-test, pantau perkembangan anak, lalu rencanakan tindak lanjut bersama program HEROISM.", color = Color.White.copy(alpha = .9f), fontFamily = Poppins, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Button(onClick = onSignUp, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroBlue)) {
                Text("Buat profil pendamping", fontFamily = Poppins, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSignIn) { Text("Sudah punya profil? Masuk", color = Color.White, fontFamily = Poppins) }
//            Spacer(Modifier.height(10.dp))
//            Text("Bisa digunakan offline • Data lokal terlindungi PIN", color = Color.White.copy(alpha = .8f), fontFamily = Poppins, fontSize = 10.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SplashScreen(onComplete: () -> Unit) {
    SetStatusBarIconStyle(lightBackground = false)
    LaunchedEffect(Unit) {
        delay(1_000)
        onComplete()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(HeroBlue, HeroSky)))) {
        Image(painterResource(R.drawable.hero_pattern), null, Modifier.fillMaxSize().alpha(.18f), contentScale = ContentScale.Crop)
        Image(
            painterResource(R.drawable.menyala_hero_logo),
            contentDescription = "Menyala-HERO",
            modifier = Modifier.align(Alignment.Center).width(238.dp).height(132.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun AuthFormScreen(
    title: String, subtitle: String, showName: Boolean,
    name: String, phone: String, pin: String, error: String,
    onNameChange: (String) -> Unit, onPhoneChange: (String) -> Unit, onPinChange: (String) -> Unit,
    onBack: () -> Unit, onSubmit: () -> Unit,
    buttonText: String, isSignUp: Boolean, footerText: String, onFooter: () -> Unit,
    biometricEnabled: Boolean = false,
    onBiometricLogin: () -> Unit = {},
) {
    SetStatusBarIconStyle(lightBackground = false)
    BackHandler(onBack = onBack)
    var focusedField by remember { mutableStateOf("") }
    val signInExpression = when {
        phone.length >= 10 && pin.length in 4..6 -> R.drawable.aksa_excited
        focusedField == "pin" -> R.drawable.aksa_avoid_looking
        focusedField == "phone" -> R.drawable.aksa_curious
        else -> R.drawable.aksa_happi
    }

    Box(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth()
                    .height(if (isSignUp) 220.dp else 92.dp)
                    .background(Brush.verticalGradient(listOf(HeroBlue, HeroSky))),
            ) {
                Image(painterResource(R.drawable.hero_pattern), null, Modifier.fillMaxSize().alpha(.18f), contentScale = ContentScale.Crop)
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp).testTag("auth-back").semantics { contentDescription = "Kembali" },
                        shape = RoundedCornerShape(13.dp),
                        color = Color.White.copy(alpha = .16f),
                        contentColor = Color.White,
                    ) { Box(contentAlignment = Alignment.Center) { Text("‹", fontSize = 31.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium) } }
                    Spacer(Modifier.width(10.dp))
                    Text(if (isSignUp) "Daftar" else "Masuk", color = Color.White, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }
            Surface(
                Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    if (isSignUp) Spacer(Modifier.height(52.dp))
                    Column(Modifier.fillMaxWidth(if (isSignUp) .68f else 1f)) {
                        Text(title, color = HeroRed, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(subtitle, color = Muted, fontFamily = Poppins, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    if (!isSignUp) {
                        Spacer(Modifier.height(23.dp))
                        Crossfade(targetState = signInExpression, label = "aksa-sign-in-expression") { expression ->
                            Image(
                                painterResource(expression),
                                contentDescription = when (expression) {
                                    R.drawable.aksa_curious -> "Aksa melihat ke arah nomor telepon"
                                    R.drawable.aksa_avoid_looking -> "Aksa mengalihkan pandangan saat PIN diisi"
                                    R.drawable.aksa_excited -> "Aksa tersenyum dengan mata berbinar"
                                    else -> "Aksa tersenyum"
                                },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }
                    if (showName) {
                        AuthField("Nama pendamping", name, onNameChange, "Nama", showLabel = false, tag = "auth-name")
                        Spacer(Modifier.height(10.dp))
                    }
                    AuthField(
                        "Nomor telepon", phone, onPhoneChange, "Nomor Telepon", KeyboardType.Phone,
                        showLabel = false, tag = "auth-phone", onFocusChange = { if (it) focusedField = "phone" },
                    )
                    Spacer(Modifier.height(10.dp))
                    AuthField(
                        "PIN aplikasi", pin, onPinChange, "PIN", KeyboardType.NumberPassword,
                        secret = true, showLabel = false, tag = "auth-pin", onFocusChange = { if (it) focusedField = "pin" },
                    )
                    if (showName) {
                        Spacer(Modifier.height(6.dp))
                        Text("Profil disimpan aman di perangkat ini.", color = Muted, fontFamily = Poppins, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(error, color = HeroRed, fontFamily = Poppins, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    val canSubmit = phone.length >= 10 && pin.length in 4..6 && (!showName || name.trim().length >= 2)
                    if (!isSignUp && biometricEnabled) {
                        Row(
                            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2F5F8)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = onSubmit,
                                enabled = canSubmit,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HeroRed, disabledContainerColor = Color(0xFFD7D9DF), disabledContentColor = Color.White),
                            ) { Text(buttonText, fontFamily = Poppins, fontWeight = FontWeight.Medium) }
                            Box(
                                modifier = Modifier.size(48.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(HeroBlue)
                                    .clickable(role = Role.Button, onClick = onBiometricLogin)
                                    .testTag("auth-biometric-login")
                                    .semantics { contentDescription = "Masuk dengan sidik jari" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_fingerprint),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    colorFilter = ColorFilter.tint(Color.White),
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onSubmit,
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HeroRed, disabledContainerColor = Color(0xFFF0EFF6), disabledContentColor = Muted.copy(alpha = .58f)),
                        ) { Text(buttonText, fontFamily = Poppins, fontWeight = FontWeight.Medium) }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onFooter, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(footerText, color = HeroBlue, fontFamily = Poppins, fontSize = 11.sp)
                    }
                    if (showName) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nomor telepon menjadi identitas lokal; tidak ada verifikasi SMS.",
                            color = Muted, fontFamily = Poppins, fontSize = 10.sp, lineHeight = 15.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (isSignUp) {
            Image(
                painterResource(R.drawable.signup_mascot),
                contentDescription = "Aksa memegang dan menunjuk formulir daftar",
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp).offset(y = 34.dp).size(width = 145.dp, height = 258.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    showLabel: Boolean = true,
    tag: String = "",
    onFocusChange: (Boolean) -> Unit = {},
) {
    Column {
        if (showLabel) {
            Text(label, color = Ink, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .then(if (tag.isEmpty()) Modifier else Modifier.testTag(tag))
                .onFocusChanged { onFocusChange(it.isFocused) },
            placeholder = { Text(placeholder, fontFamily = Poppins, fontSize = 11.sp, color = Muted.copy(alpha = .7f)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        )
    }
}

private fun authMessage(failure: AuthFailure): String = when (failure) {
    AuthFailure.PROFILE_EXISTS -> "Profil pendamping sudah ada di perangkat ini. Silakan masuk."
    AuthFailure.PROFILE_MISSING -> "Profil lokal belum dibuat. Buat profil terlebih dahulu."
    AuthFailure.INVALID_NAME -> "Nama harus berisi 2–60 karakter."
    AuthFailure.INVALID_PHONE -> "Masukkan nomor Indonesia yang valid, minimal 10 digit."
    AuthFailure.INVALID_PIN -> "PIN harus terdiri dari 4–6 angka."
    AuthFailure.INVALID_CREDENTIALS -> "Nomor telepon atau PIN tidak cocok."
    AuthFailure.STORAGE_ERROR -> "Data profil tidak dapat dibaca atau disimpan. Coba lagi."
}
