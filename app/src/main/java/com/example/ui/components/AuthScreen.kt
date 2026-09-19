package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser

/**
 * Account screen: email/password sign-up + sign-in, Google Sign-In, and the
 * signed-in state with sign-out. All Firebase-missing states degrade to an
 * explanatory message instead of crashing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    user: FirebaseUser?,
    isFirebaseConfigured: Boolean,
    isGoogleSignInConfigured: Boolean,
    isBusy: Boolean,
    errorMessage: String?,
    onSignUp: (email: String, password: String) -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: (activity: Activity) -> Unit,
    onSignOut: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("auth_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isFirebaseConfigured) {
                Text(
                    text = "Firebase is not configured yet. Email and Google sign-in are " +
                        "disabled until google-services.json is added (see README).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (user != null) {
                Text(
                    text = "Signed in",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email ?: user.uid,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_sign_out")
                ) {
                    Text("Sign out")
                }
            } else {
                AuthForm(
                    enabled = isFirebaseConfigured && !isBusy,
                    isBusy = isBusy,
                    errorMessage = errorMessage,
                    onSignUp = { email, password -> onClearError(); onSignUp(email, password) },
                    onSignIn = { email, password -> onClearError(); onSignIn(email, password) }
                )

                val activity = LocalContext.current as? Activity
                Button(
                    onClick = { activity?.let { onGoogleSignIn(it) } },
                    enabled = isGoogleSignInConfigured && !isBusy && activity != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_google_sign_in")
                ) {
                    Text("Continue with Google")
                }
                if (!isGoogleSignInConfigured) {
                    Text(
                        text = "Google Sign-In needs a Web client ID (GOOGLE_WEB_CLIENT_ID).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.testTag("auth_busy"))
            }
        }
    }
}

@Composable
private fun AuthForm(
    enabled: Boolean,
    isBusy: Boolean,
    errorMessage: String?,
    onSignUp: (email: String, password: String) -> Unit,
    onSignIn: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    val valid = email.isNotBlank() && password.length >= 6

    Text(
        text = if (isSignUpMode) "Create your account" else "Welcome back",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_email")
    )

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min 6 characters)") },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_password")
    )

    if (errorMessage != null) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("auth_error")
        )
    }

    Button(
        onClick = {
            if (isSignUpMode) onSignUp(email, password) else onSignIn(email, password)
        },
        enabled = enabled && valid && !isBusy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_submit")
    ) {
        Text(if (isSignUpMode) "Sign up" else "Sign in")
    }

    TextButton(
        onClick = { isSignUpMode = !isSignUpMode },
        enabled = enabled,
        modifier = Modifier.testTag("auth_toggle_mode")
    ) {
        Text(
            if (isSignUpMode) "Already have an account? Sign in"
            else "New here? Create an account"
        )
    }
}
