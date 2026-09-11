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
    var dateOfBirth by remember {mutableStateOf("")}

    //Job Type Dropdown State
    val jobTypes = listOf("University Student", "Software Developer", "Office Worker", "Remote Worker", "Other")
    var isJobMenuExpanded by remember {mutableStateOf(false)}
    var selectedJobType by remember {mutableStateOf("")}

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
                label = {Text("Full Name")},
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Email Address Field
            OutlinedTextField(
                value = email,
                onValueChange = {email = it},
                label = {Text("Email Address")},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {password = it},
                label = {Text("Password")},
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

            //Date of Birth Field
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = {},
                readOnly = true,
                label = {Text("Date of Birth")},
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

            //Job Type Dropdown
            ExposedDropdownMenuBox(
                expanded = isJobMenuExpanded,
                onExpandedChange = {isJobMenuExpanded = it},
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedJobType,
                    onValueChange = {},
                    readOnly = true,
                    label = {Text("Job Type")},
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isJobMenuExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                )

                ExposedDropdownMenu(
                    expanded = isJobMenuExpanded,
                    onDismissRequest = {isJobMenuExpanded = false}
                ) {
                    jobTypes.forEach {option ->
                        DropdownMenuItem(
                            text = {Text(option)},
                            onClick = {
                                selectedJobType = option
                                isJobMenuExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

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
                    label = {Text("Primary Ergonomic Concern")},
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

            //Sign Up Button
            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank() || password.isBlank() || dateOfBirth.isBlank()|| selectedJobType.isBlank() || selectedDiscomfort.isBlank()) {
                        Toast.makeText(context, "Please fill out all details!", Toast.LENGTH_SHORT).show()
                    } else {Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        onSignUpSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {Text("Sign Up")}

            Spacer(modifier = Modifier.height(20.dp))

            //Navigation to Login
            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Log In!")
            }
        }
    }
}