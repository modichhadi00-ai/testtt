package com.wormgpt.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.wormgpt.app.R
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.UserRepository
import com.wormgpt.app.ui.theme.WormRed
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoggedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSignUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken ?: return@rememberLauncherForActivityResult
            scope.launch {
                isLoading = true
                error = null
                authRepository.signInWithGoogle(idToken).fold(
                    onSuccess = { user ->
                        userRepository.createOrUpdateProfile(
                            user.uid,
                            user.email,
                            user.displayName,
                            user.photoUrl?.toString()
                        )
                        onLoggedIn()
                    },
                    onFailure = { e -> error = e.message; isLoading = false }
                )
            }
        } catch (e: ApiException) {
            error = when (e.statusCode) {
                10 -> "Add your app SHA-1 in Firebase Console (see FIREBASE_SETUP.md)"
                7 -> "Network error. Check your connection."
                12501 -> "Sign-in cancelled"
                else -> "Google sign in failed (${e.statusCode}). Check Web Client ID and SHA-1 in Firebase."
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WORMGPT",
            style = MaterialTheme.typography.headlineLarge,
            color = WormRed
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    val result = if (isSignUp) {
                        authRepository.createUserWithEmailAndPassword(email, password)
                    } else {
                        authRepository.signInWithEmailAndPassword(email, password)
                    }
                    result.fold(
                        onSuccess = { user ->
                            userRepository.createOrUpdateProfile(
                                user.uid,
                                user.email,
                                user.displayName,
                                user.photoUrl?.toString()
                            )
                            onLoggedIn()
                        },
                        onFailure = { e ->
                            error = when {
                                e.message?.contains("configuration_not_found") == true ->
                                    "Enable Identity Toolkit API in Google Cloud Console (see FIREBASE_SETUP.md)"
                                e.message?.contains("INVALID_EMAIL") == true -> "Invalid email address"
                                e.message?.contains("EMAIL_NOT_FOUND") == true -> "No account with this email"
                                e.message?.contains("WRONG_PASSWORD") == true -> "Wrong password"
                                e.message?.contains("EMAIL_EXISTS") == true -> "Email already registered. Sign in instead."
                                e.message?.contains("WEAK_PASSWORD") == true -> "Password must be at least 6 characters"
                                else -> e.message ?: "Sign in failed"
                            }
                            isLoading = false
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.length >= 6
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (isSignUp) "Sign up" else "Sign in")
        }
        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Sign in" else "Create account")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val webClientId = context.getString(R.string.default_web_client_id)
                if (webClientId.isBlank() || webClientId == "YOUR_WEB_CLIENT_ID" || !webClientId.endsWith(".apps.googleusercontent.com")) {
                    error = "Firebase not set up. Add your Web Client ID in app/src/main/res/values/strings.xml (see FIREBASE_SETUP.md)"
                    return@Button
                }
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val signInClient = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(signInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google")
        }
    }
}
