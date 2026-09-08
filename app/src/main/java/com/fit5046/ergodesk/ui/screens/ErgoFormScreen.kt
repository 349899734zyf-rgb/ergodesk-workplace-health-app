package com.fit5046.ergodesk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * ErgoForm screen — ergonomic and work-profile assessment.
 * Member 3 responsibility.
 *
 * A2 skeleton prototype:
 * - Expanded Dropdown Menu: job type and main discomfort
 * - TimePicker: work start and end time
 * - Checkbox: multiple areas of concern
 *
 * Form design:
 * - Single-column structure
 * - Related fields grouped into logical sections
 * - Selection-based input where appropriate
 * - Required and optional information clearly identified
 *
 * A4 extension:
 * The profile can later be stored using Room and combined with
 * contextual activity information to personalise break recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErgoFormScreen() {

    val jobTypes = listOf(
        "Student",
        "Developer",
        "Office worker",
        "Other"
    )

    val painAreas = listOf(
        "None",
        "Neck",
        "Shoulders",
        "Lower back",
        "Wrists"
    )

    val concernOptions = listOf(
        "Neck",
        "Shoulders",
        "Lower back",
        "Wrists"
    )

    // Job type
    var jobType by remember { mutableStateOf("Student") }
    var jobExpanded by remember { mutableStateOf(false) }

    // Main discomfort
    var painArea by remember { mutableStateOf("None") }
    var painExpanded by remember { mutableStateOf(false) }

    // Optional multiple concerns
    var checkedConcerns by remember {
        mutableStateOf(setOf<String>())
    }

    // Start time
    var showStartTimeDialog by remember {
        mutableStateOf(false)
    }

    val startTimeState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true
    )

    var workStartLabel by remember {
        mutableStateOf("09:00")
    }

    // End time
    var showEndTimeDialog by remember {
        mutableStateOf(false)
    }

    val endTimeState = rememberTimePickerState(
        initialHour = 17,
        initialMinute = 0,
        is24Hour = true
    )

    var workEndLabel by remember {
        mutableStateOf("17:00")
    }

    // Simple UI-only confirmation for A2 prototype
    var showSavedMessage by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 100.dp
            ),
        verticalArrangement = Arrangement.Top
    ) {

        // ---------------------------------------------------------
        // Header
        // ---------------------------------------------------------

        Text(
            text = "Ergonomic Assessment",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Help ErgoDesk personalise your break and ergonomic recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 20.dp
            )
        )

        // ---------------------------------------------------------
        // Work profile
        // ---------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Work Profile",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Tell us about your typical desk-based work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 14.dp
                    )
                )

                // Job type dropdown
                ExposedDropdownMenuBox(
                    expanded = jobExpanded,
                    onExpandedChange = {
                        jobExpanded = !jobExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = jobType,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Job type *")
                        },
                        supportingText = {
                            Text("Required")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = jobExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = jobExpanded,
                        onDismissRequest = {
                            jobExpanded = false
                        }
                    ) {

                        jobTypes.forEach { option ->

                            DropdownMenuItem(
                                text = {
                                    Text(option)
                                },
                                onClick = {
                                    jobType = option
                                    jobExpanded = false
                                    showSavedMessage = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // Main discomfort dropdown
                ExposedDropdownMenuBox(
                    expanded = painExpanded,
                    onExpandedChange = {
                        painExpanded = !painExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = painArea,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Main area of discomfort *")
                        },
                        supportingText = {
                            Text("Choose the area that affects you most")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = painExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = painExpanded,
                        onDismissRequest = {
                            painExpanded = false
                        }
                    ) {

                        painAreas.forEach { option ->

                            DropdownMenuItem(
                                text = {
                                    Text(option)
                                },
                                onClick = {
                                    painArea = option
                                    painExpanded = false
                                    showSavedMessage = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ---------------------------------------------------------
        // Work schedule
        // ---------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Work Schedule",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Your usual work hours help ErgoDesk plan appropriate break reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 14.dp
                    )
                )

                // Start time
                Text(
                    text = "Start time *",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = workStartLabel,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = {
                            showStartTimeDialog = true
                        }
                    ) {
                        Text("Change")
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // End time
                Text(
                    text = "End time *",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = workEndLabel,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = {
                            showEndTimeDialog = true
                        }
                    ) {
                        Text("Change")
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ---------------------------------------------------------
        // Optional concerns
        // ---------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Additional Areas of Concern",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Optional — select all other areas that regularly feel uncomfortable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 8.dp
                    )
                )

                concernOptions.forEach { concern ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = checkedConcerns.contains(
                                concern
                            ),
                            onCheckedChange = { isChecked ->

                                checkedConcerns =
                                    if (isChecked) {
                                        checkedConcerns + concern
                                    } else {
                                        checkedConcerns - concern
                                    }

                                showSavedMessage = false
                            }
                        )

                        Text(
                            text = concern,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // ---------------------------------------------------------
        // Primary action
        // ---------------------------------------------------------

        Button(
            onClick = {
                /*
                 * A2 skeleton:
                 * Demonstrates the intended form interaction only.
                 *
                 * A4:
                 * Replace this with persistent Room storage and
                 * connect the saved profile to personalised
                 * context-aware recommendations.
                 */
                showSavedMessage = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Assessment")
        }

        if (showSavedMessage) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Assessment saved for this prototype.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "* Required fields",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Your profile will be used to personalise ErgoDesk recommendations and break reminders.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    // -------------------------------------------------------------
    // Start time dialog
    // -------------------------------------------------------------

    if (showStartTimeDialog) {

        Dialog(
            onDismissRequest = {
                showStartTimeDialog = false
            }
        ) {

            Surface(
                shape = MaterialTheme.shapes.extraLarge
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Select start time",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TimePicker(
                        state = startTimeState
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        onClick = {

                            workStartLabel =
                                "%02d:%02d".format(
                                    startTimeState.hour,
                                    startTimeState.minute
                                )

                            showStartTimeDialog = false
                            showSavedMessage = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // End time dialog
    // -------------------------------------------------------------

    if (showEndTimeDialog) {

        Dialog(
            onDismissRequest = {
                showEndTimeDialog = false
            }
        ) {

            Surface(
                shape = MaterialTheme.shapes.extraLarge
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Select end time",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TimePicker(
                        state = endTimeState
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        onClick = {

                            workEndLabel =
                                "%02d:%02d".format(
                                    endTimeState.hour,
                                    endTimeState.minute
                                )

                            showEndTimeDialog = false
                            showSavedMessage = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}