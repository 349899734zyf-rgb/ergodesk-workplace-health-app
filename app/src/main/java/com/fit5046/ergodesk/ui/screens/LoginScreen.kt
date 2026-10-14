package com.fit5046.ergodesk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*

//Email Validation
import android.util.Patterns

//Google Sign-In Result Handling
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

//Firebase and Google Authentication
import com.fit5046.ergodesk.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

//Required Field Indicator Styling
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var isPasswordVisible by remember {mutableStateOf(false)}
    var isLoading by remember {mutableStateOf(false)}
    var errorMessage by remember {mutableStateOf("")}

    val context = LocalContext.current
    val auth = remember {FirebaseAuth.getInstance()}

    val googleSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    errorMessage = "Google login failed: ${task.exception?.localizedMessage}"
                }
            }
        } catch (error: ApiException) {
            isLoading = false
            errorMessage = "Google Sign-In error: ${error.statusCode}"
        } catch (error: Exception) {
            isLoading = false
            errorMessage = "Google Sign-In failed: ${error.message}"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to ErgoDesk!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            //Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""},
                label = {Text(buildAnnotatedString {
                    append("Email ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Email Icon"
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            //Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {password = it
                    errorMessage = ""},
                label = {Text(buildAnnotatedString {
                    append("Password ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon"
                    )
                },
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(if (isPasswordVisible) "Hide" else "Show")
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            //Error Message
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            //Login Button
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() -> {
                            errorMessage = "Please enter both email and password!"
                        }
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                            errorMessage = "Please enter a valid email address!"
                        }

                        else -> {
                            isLoading = true
                            errorMessage = ""

                            auth.signInWithEmailAndPassword(
                                email.trim(),
                                password
                            ).addOnCompleteListener {task -> isLoading = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    errorMessage = "Login Failed! Check your email and password!"
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {Text("Login")}

            Spacer(modifier = Modifier.height(20.dp))

            //Google Login Button
            OutlinedButton(
                onClick = {
                    isLoading = true
                    errorMessage = ""

                    googleSignInClient.signOut().addOnCompleteListener {
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Continue with Google")
            }

            Spacer(modifier = Modifier.height(20.dp))

            //Loading Indicator
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            //Navigation to Sign Up
            TextButton(onClick = onNavigateToSignUp) {
                Text("Don't have an account? Sign Up!")
            }
        }
    }
}