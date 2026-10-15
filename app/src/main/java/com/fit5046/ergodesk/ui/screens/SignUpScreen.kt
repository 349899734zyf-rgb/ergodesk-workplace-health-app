package com.fit5046.ergodesk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

//Email Validation
import android.util.Patterns

//Firebase Authentication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

//Required Field Indicator Styling
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {}
) {
    var fullName by remember {mutableStateOf("")}
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var isPasswordVisible by remember {mutableStateOf(false)}
    var confirmPassword by remember {mutableStateOf("")}
    var isConfirmPasswordVisible by remember {mutableStateOf(false)}
    var dateOfBirth by remember {mutableStateOf("")}

    var isLoading by remember {mutableStateOf(false)}
    var errorMessage by remember {mutableStateOf("")}

    //Primary Ergonomic Concern Dropdown State
    val discomfortAreas = listOf("Neck & Shoulders", "Lower Back", "Wrists & Hands", "General Sedentary")
    var isDiscomfortExpanded by remember {mutableStateOf(false)}
    var selectedDiscomfort by remember {mutableStateOf("")}

    //DatePicker State
    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )
    var showDatePicker by remember {mutableStateOf(false)}

    val context = LocalContext.current
    val auth = remember {FirebaseAuth.getInstance()}
    val firestore = remember {FirebaseFirestore.getInstance()}

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {showDatePicker = false},
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        dateOfBirth = formatter.format(Date(millis))
                    }
                }) {Text("OK")}
            },
            dismissButton = {
                TextButton(onClick = {showDatePicker = false}) {Text("Cancel")}
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            //Full Name Field
            OutlinedTextField(
                value = fullName,
                onValueChange = {fullName = it},
                label = {Text(buildAnnotatedString {
                    append("Full Name ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Email Address Field
            OutlinedTextField(
                value = email,
                onValueChange = {email = it},
                label = {Text(buildAnnotatedString {
                    append("Email Address ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {password = it},
                label = {Text(buildAnnotatedString {
                    append("Password ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                supportingText = {Text("Must be at least 8 characters long")},
                trailingIcon = {
                    TextButton(onClick = {isPasswordVisible = !isPasswordVisible}) {
                        Text(if (isPasswordVisible) "Hide" else "Show")
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = ""
                },
                label = {Text(buildAnnotatedString {
                    append("Confirm Password ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})
                },
                trailingIcon = {
                    TextButton(onClick = {isConfirmPasswordVisible = !isConfirmPasswordVisible}) {
                        Text(if (isConfirmPasswordVisible) "Hide" else "Show")
                    }
                },
                visualTransformation = if (isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Date of Birth Field
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = {},
                readOnly = true,
                label = {Text(buildAnnotatedString {
                    append("Date of Birth ")
                    withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        modifier = Modifier.clickable {showDatePicker = true}
                    )
                },
                modifier = Modifier.fillMaxWidth().clickable {showDatePicker = true}
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Primary Ergonomic Concern Dropdown
            ExposedDropdownMenuBox(
                expanded = isDiscomfortExpanded,
                onExpandedChange = {isDiscomfortExpanded = it},
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDiscomfort,
                    onValueChange = {},
                    readOnly = true,
                    label = {Text(buildAnnotatedString {
                        append("Primary Ergonomic Concern ")
                        withStyle(SpanStyle(color = Color.Red)) {append("*")}})},
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDiscomfortExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                )

                ExposedDropdownMenu(
                    expanded = isDiscomfortExpanded,
                    onDismissRequest = {isDiscomfortExpanded = false}
                ) {
                    discomfortAreas.forEach {option ->
                        DropdownMenuItem(
                            text = {Text(option)},
                            onClick = {
                                selectedDiscomfort = option
                                isDiscomfortExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            //Error Message
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            //Sign Up Button
            Button(
                onClick = {
                    when {
                        fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || dateOfBirth.isBlank() || selectedDiscomfort.isBlank() -> {
                            errorMessage = "Please fill out all details!"
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                            errorMessage = "Please enter a valid email address!"
                        }

                        password.length < 8 -> {
                            errorMessage = "Password must be at least 8 characters long!"
                        }

                        password != confirmPassword -> {
                            errorMessage = "Passwords do not match!"
                        }

                        else -> {
                            isLoading = true
                            errorMessage = ""

                            auth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener {task ->
                                    if (task.isSuccessful) {
                                        val user = task.result?.user
                                        if (user != null) {
                                            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()

                                            val userDetails = hashMapOf("fullName" to fullName.trim(), "email" to email.trim(), "dateOfBirth" to dateOfBirth, "primaryDiscomfort" to selectedDiscomfort)

                                            //Save the user's extra details to ErgoDesk's collection
                                            firestore.collection("ergodesk_users")
                                                .document(user.uid)
                                                .set(userDetails)
                                                .addOnSuccessListener {
                                                    //Keep the existing Firebase display name
                                                    user.updateProfile(profileUpdates)
                                                        .addOnCompleteListener { profileTask ->
                                                            isLoading = false

                                                            if (profileTask.isSuccessful) {
                                                                Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                                                onSignUpSuccess()
                                                            } else {
                                                                errorMessage = "Details Saved! But display name update failed: " + (profileTask.exception?.localizedMessage ?: "Unknown Error")
                                                            }
                                                        }
                                                }
                                                .addOnFailureListener { exception ->
                                                    isLoading = false
                                                    errorMessage = "Account Created! but details could not be saved: " + (exception.localizedMessage ?: "Unknown Error")
                                                }
                                        } else {
                                            isLoading = false
                                            errorMessage = "Account Created! But user information is unavailable!"
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = task.exception?.localizedMessage ?: "Registration Failed! Please try again!"
                                    }
                                }
                            }
                        }
                    },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {Text("Sign Up")}

            Spacer(modifier = Modifier.height(20.dp))

            //Loading Indicator
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            //Navigation to Login
            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Log In!")
            }
        }
    }
}