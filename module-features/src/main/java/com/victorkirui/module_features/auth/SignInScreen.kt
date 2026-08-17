package com.victorkirui.module_features.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.victorkirui.core.R
import com.victorkirui.core.ui.theme.RemindlyTheme
import org.koin.androidx.compose.koinViewModel

import com.google.android.gms.common.api.Scope

@Composable
fun SignInScreen(
    onNavigateToSignUp: () -> Unit = {},
    onSignInSuccess: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: SignInViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(id = R.string.default_web_client_id)

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                android.util.Log.e("GoogleSignIn", "ID Token was null")
            }
        } catch (e: ApiException) {
            android.util.Log.e("GoogleSignIn", "Sign in failed with code: ${e.statusCode}, message: ${e.message}, status: ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}")
        }
    }

    fun launchGoogleSignIn() {
        android.util.Log.d("GoogleSignIn", "Launching with Web Client ID: $webClientId")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    LaunchedEffect(uiState) {
        if (uiState is SignInUiState.Success) {
            onSignInSuccess()
            viewModel.resetState()
        }
    }

    SignInScreenContent(
        uiState = uiState,
        windowWidthSizeClass = windowWidthSizeClass,
        onSignIn = { email, password ->
            viewModel.signIn(email, password)
        },
        onGoogleSignIn = { launchGoogleSignIn() },
        onNavigateToSignUp = onNavigateToSignUp,
        onForgotPassword = onForgotPassword
    )

    if (uiState is SignInUiState.Loading) {
        AuthLoadingDialog()
    }
}

@Composable
fun SignInScreenContent(
    uiState: SignInUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSignIn: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val isCompact = windowWidthSizeClass == WindowWidthSizeClass.Compact
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    Row(modifier = Modifier.fillMaxSize()) {
        if (!isCompact) {
            SignInBrandingPanel(
                windowWidthSizeClass = windowWidthSizeClass,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .weight(if (isExpanded) 1.1f else 1.2f)
                .fillMaxHeight()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            SignInForm(
                uiState = uiState,
                windowWidthSizeClass = windowWidthSizeClass,
                onSignIn = onSignIn,
                onGoogleSignIn = onGoogleSignIn,
                onNavigateToSignUp = onNavigateToSignUp,
                onForgotPassword = onForgotPassword,
                modifier = Modifier
                    .widthIn(max = if (isExpanded) 600.dp else 500.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(
                        horizontal = if (isCompact) 24.dp else if (isExpanded) 48.dp else 32.dp,
                        vertical = 32.dp
                    )
            )
        }
    }
}

@Composable
private fun SignInBrandingPanel(
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF2D6A4F))
            .padding(if (isExpanded) 64.dp else 48.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Remindly",
                style = if (isExpanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Nothing important should be forgotten.",
                style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(if (isExpanded) 140.dp else 120.dp))

            Text(
                "Welcome back.\nYour memories await.",
                style = if (isExpanded) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = if (isExpanded) 56.sp else 44.sp
            )
            
            Spacer(modifier = Modifier.height(if (isExpanded) 32.dp else 24.dp))
            
            Text(
                "Everything you saved is right here — organised, understood, and ready.",
                style = if (isExpanded) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = if (isExpanded) 32.sp else 24.sp
            )

            Spacer(modifier = Modifier.height(if (isExpanded) 64.dp else 48.dp))

            FeatureItem(icon = Icons.Outlined.Share, text = "Share from any app", isExpanded = isExpanded)
            FeatureItem(icon = Icons.Outlined.AutoAwesome, text = "AI understands your content", isExpanded = isExpanded)
            FeatureItem(icon = Icons.Outlined.NotificationsNone, text = "Reminders at the right time", isExpanded = isExpanded)
        }

        Column {
            Text(
                "\"Finally, an app that actually remembers.\"",
                style = if (isExpanded) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontStyle = FontStyle.Italic
            )
            Text(
                "— Victor, early user",
                style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, text: String, isExpanded: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = if (isExpanded) 16.dp else 12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.size(if (isExpanded) 40.dp else 32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isExpanded) 22.dp else 18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(if (isExpanded) 20.dp else 16.dp))
        Text(
            text = text, 
            color = Color.White, 
            fontWeight = FontWeight.Medium,
            style = if (isExpanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SignInForm(
    uiState: SignInUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSignIn: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isFormValid by remember {
        derivedStateOf { email.isNotBlank() && password.isNotBlank() }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(if (isExpanded) 64.dp else 48.dp))

        Text(
            "Welcome back",
            style = if (isExpanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            "Sign in to continue.",
            style = if (isExpanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(if (isExpanded) 64.dp else 48.dp))

        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isExpanded) 64.dp else 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black, containerColor = Color.White)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(if (isExpanded) 24.dp else 20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Continue with Google", 
                    fontWeight = FontWeight.SemiBold,
                    style = if (isExpanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isExpanded) 32.dp else 24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text(
                "or",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(if (isExpanded) 32.dp else 24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color(0xFF2D6A4F),
                focusedBorderColor = Color(0xFF2D6A4F),
                focusedLabelColor = Color(0xFF2D6A4F),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color(0xFF2D6A4F),
                focusedBorderColor = Color(0xFF2D6A4F),
                focusedLabelColor = Color(0xFF2D6A4F),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Forgot password?",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onForgotPassword() },
            textAlign = TextAlign.End,
            color = Color(0xFF2D6A4F),
            style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(if (isExpanded) 48.dp else 32.dp))

        if (uiState is SignInUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { onSignIn(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isExpanded) 64.dp else 56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F), disabledContainerColor = Color(0xFFE7E7E7)),
            enabled = uiState !is SignInUiState.Loading && isFormValid
        ) {
            if (uiState is SignInUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(
                    "Sign In", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = if (isExpanded) 18.sp else 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Don't have an account? ", 
                color = Color.Gray,
                style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
            )
            Text(
                "Sign Up",
                color = Color(0xFF2D6A4F),
                fontWeight = FontWeight.Bold,
                style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun SignInScreenExpandedPreview() {
    RemindlyTheme {
        SignInScreenContent(
            uiState = SignInUiState.Idle,
            windowWidthSizeClass = WindowWidthSizeClass.Expanded,
            onSignIn = { _, _ -> },
            onGoogleSignIn = {},
            onNavigateToSignUp = {},
            onForgotPassword = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun SignInScreenMediumPreview() {
    RemindlyTheme {
        SignInScreenContent(
            uiState = SignInUiState.Idle,
            windowWidthSizeClass = WindowWidthSizeClass.Medium,
            onSignIn = { _, _ -> },
            onGoogleSignIn = {},
            onNavigateToSignUp = {},
            onForgotPassword = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenCompactPreview() {
    RemindlyTheme {
        SignInScreenContent(
            uiState = SignInUiState.Idle,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSignIn = { _, _ -> },
            onGoogleSignIn = {},
            onNavigateToSignUp = {},
            onForgotPassword = {}
        )
    }
}
