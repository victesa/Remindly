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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.victorkirui.core.ui.theme.RemindlyTheme
import com.victorkirui.core.R
import org.koin.androidx.compose.koinViewModel

import com.google.android.gms.common.api.Scope

@Composable
fun SignUpScreen(
    onNavigateToSignIn: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: SignUpViewModel = koinViewModel()
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
        if (uiState is SignUpUiState.Success) {
            onSignUpSuccess()
            viewModel.resetState()
        }
    }

    SignUpScreenContent(
        uiState = uiState,
        windowWidthSizeClass = windowWidthSizeClass,
        onSignUp = { name, email, password ->
            viewModel.signUp(email, password, name)
        },
        onGoogleSignIn = { launchGoogleSignIn() },
        onNavigateToSignIn = onNavigateToSignIn
    )

    if (uiState is SignUpUiState.Loading) {
        AuthLoadingDialog()
    }
}

@Composable
fun SignUpScreenContent(
    uiState: SignUpUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSignUp: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val isCompact = windowWidthSizeClass == WindowWidthSizeClass.Compact
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    Row(modifier = Modifier.fillMaxSize()) {
        if (!isCompact) {
            SignUpBrandingPanel(
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
            SignUpForm(
                uiState = uiState,
                windowWidthSizeClass = windowWidthSizeClass,
                onSignUp = onSignUp,
                onGoogleSignIn = onGoogleSignIn,
                onNavigateToSignIn = onNavigateToSignIn,
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
private fun SignUpBrandingPanel(
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
                "Your AI memory,\nalways with you.",
                style = if (isExpanded) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = if (isExpanded) 56.sp else 44.sp
            )
            
            Spacer(modifier = Modifier.height(if (isExpanded) 32.dp else 24.dp))
            
            Text(
                "Share anything from any app. Remindly captures,\nunderstands, and reminds you at exactly the right time.",
                style = if (isExpanded) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = if (isExpanded) 32.sp else 24.sp
            )

            Spacer(modifier = Modifier.height(if (isExpanded) 64.dp else 48.dp))

            FeatureItem(
                icon = Icons.Outlined.Share, 
                text = "Share from any app instantly",
                isExpanded = isExpanded
            )
            FeatureItem(
                icon = Icons.Outlined.AutoAwesome, 
                text = "AI understands your content",
                isExpanded = isExpanded
            )
            FeatureItem(
                icon = Icons.Outlined.NotificationsNone, 
                text = "Get reminded at the right time",
                isExpanded = isExpanded
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector, 
    text: String,
    isExpanded: Boolean
) {
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
private fun SignUpForm(
    uiState: SignUpUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSignUp: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isFormValid by remember {
        derivedStateOf { fullName.isNotBlank() && email.isNotBlank() && password.length >= 6 }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(if (isExpanded) 64.dp else 48.dp))
        
        Text(
            "Create your account",
            style = if (isExpanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Text(
            "Start remembering what matters.",
            style = if (isExpanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(if (isExpanded) 64.dp else 48.dp))
        
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth().height(if (isExpanded) 64.dp else 56.dp),
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
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.Gray) },
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = buildAnnotatedString {
                append("By signing up, you agree to our ")
                withStyle(SpanStyle(color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)) {
                    append("Terms of Service")
                }
                append(" and ")
                withStyle(SpanStyle(color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)) {
                    append("Privacy Policy")
                }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(if (isExpanded) 48.dp else 32.dp))
        
        if (uiState is SignUpUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { onSignUp(fullName, email, password) },
            modifier = Modifier.fillMaxWidth().height(if (isExpanded) 64.dp else 56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F), disabledContainerColor = Color(0xFFE7E7E7)),
            enabled = uiState !is SignUpUiState.Loading && isFormValid
        ) {
            if (uiState is SignUpUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(
                    "Create Account", 
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
                "Already have an account? ", 
                color = Color.Gray,
                style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
            )
            Text(
                "Sign In", 
                color = Color(0xFF2D6A4F), 
                fontWeight = FontWeight.Bold,
                style = if (isExpanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onNavigateToSignIn() }
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun SignUpScreenExpandedPreview() {
    RemindlyTheme {
        SignUpScreenContent(
            uiState = SignUpUiState.Idle,
            windowWidthSizeClass = WindowWidthSizeClass.Expanded,
            onSignUp = { _, _, _ -> },
            onGoogleSignIn = {},
            onNavigateToSignIn = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
fun SignUpScreenMediumPreview() {
    RemindlyTheme {
        SignUpScreenContent(
            uiState = SignUpUiState.Idle,
            windowWidthSizeClass = WindowWidthSizeClass.Medium,
            onSignUp = { _, _, _ -> },
            onGoogleSignIn = {},
            onNavigateToSignIn = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun SignUpScreenCompactPreview() {
    RemindlyTheme {
        SignUpScreenContent(
            uiState = SignUpUiState.Idle,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSignUp = { _, _, _ -> },
            onGoogleSignIn = {},
            onNavigateToSignIn = {}
        )
    }
}
