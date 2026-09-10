package com.fit5046.ergodesk.ui.screens

/*
 * FIT5046 Assessment 2 — ErgoDesk
 * Member 2 — Settings screen (skeleton prototype, UI only).
 *
 * Back navigation: onLogout comes from AppNavHost ("Sign out" -> Login).
 * The bottom NavigationBar is app-wide (ErgoDeskBottomNavBar).
 *
 * The work profile (work hours, job type, discomfort areas) lives in
 * Member 3's ErgoForm assessment and is read-only here. Settings only holds
 * app preferences. "Quiet hours" is a do-not-disturb window, not work hours.
 *
 * Components: Expanded (Exposed) Dropdown Menu x2 (basic);
 * Switch x4, Slider x2, TimePicker x2 in a dialog (additional).
 *
 * Step 4 form guidelines:
 *   G1  related fields grouped under section headers (Monitoring / Schedule /
 *       Reminders / Context sources)
 *   G2  control type matches the data type (time -> TimePicker, range ->
 *       Slider, choice -> Dropdown), which blocks bad input
 *   G3  the current value is always visible next to each control ("30 min",
 *       "12:30"), so the user never has to guess
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fit5046.ergodesk.ui.theme.ErgoDeskTheme
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit) {

    // Prototype: local state only; A4 supplies this from a ViewModel + Room/DataStore.
    var monitoringEnabled by remember { mutableStateOf(true) }
    var smartMode by remember { mutableStateOf(true) }
    var weatherAware by remember { mutableStateOf(true) }
    var calendarAware by remember { mutableStateOf(false) }

    var breakInterval by remember { mutableFloatStateOf(30f) }         // 20–90 min
    var movementSensitivity by remember { mutableFloatStateOf(3f) }    // 1–5

    var quietStart by remember { mutableStateOf("12:30") }
    var quietEnd by remember { mutableStateOf("13:30") }
    var showQuietStartPicker by remember { mutableStateOf(false) }
    var showQuietEndPicker by remember { mutableStateOf(false) }

    val reminderStyles = listOf("Notification", "Sound + notification", "Vibration only", "Silent banner")
    var reminderStyle by remember { mutableStateOf(reminderStyles.first()) }

    val breakActivities = listOf("Stretch", "Walk", "Eye rest")
    var breakActivity by remember { mutableStateOf(breakActivities.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        ErgoDeskHeader()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            /* Title */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = ErgoGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            /* User card — read-only; the profile is maintained in the ErgoForm assessment. */
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ErgoSoftGreen
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Sherry Fang", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Office worker · 8 h screen time / day",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErgoMutedText
                    )
                    Text(
                        "From your ergonomic assessment",
                        fontSize = 11.sp,
                        color = ErgoMutedText,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            /* G1 — section 1: Monitoring */
            SectionHeader("Monitoring")

            SettingSwitchRow(
                title = "Enable sitting monitoring",
                subtitle = "Track sitting time in the background",
                checked = monitoringEnabled,
                onCheckedChange = { monitoringEnabled = it }
            )
            SettingSwitchRow(
                title = "Smart context-aware mode",
                subtitle = "Automatically adjust the break interval from your recent break history",
                checked = smartMode,
                onCheckedChange = { smartMode = it }
            )

            // G2 + G3: a Slider instead of a text field; current value shown top-right.
            SettingSliderRow(
                title = "Break interval",
                valueLabel = "${breakInterval.roundToInt()} min",
                value = breakInterval,
                onValueChange = { breakInterval = it },
                valueRange = 20f..90f,
                steps = 13,
                endLabels = "20 min" to "90 min"
            )
            SettingSliderRow(
                title = "Movement sensitivity",
                valueLabel = "Level ${movementSensitivity.roundToInt()}",
                value = movementSensitivity,
                onValueChange = { movementSensitivity = it },
                valueRange = 1f..5f,
                steps = 3,
                endLabels = "Relaxed" to "Sensitive"
            )

            /* G1 — section 2: Schedule */
            SectionHeader("Schedule")

            // G2: time is entered through a TimePicker dialog, so "9pm" / "21:00" style
            // input errors are impossible.
            TimeFieldRow(
                label = "Quiet hours start",
                value = quietStart,
                onClick = { showQuietStartPicker = true }
            )
            TimeFieldRow(
                label = "Quiet hours end",
                value = quietEnd,
                onClick = { showQuietEndPicker = true }
            )
            Text(
                "Reminders are muted during these hours (e.g. lunch or a focus block).",
                style = MaterialTheme.typography.bodySmall,
                color = ErgoMutedText,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            /* G1 — section 3: Reminders */
            SectionHeader("Reminders")

            // Basic component: Expanded (Exposed) Dropdown Menu.
            DropdownField(
                label = "Reminder style",
                options = reminderStyles,
                selected = reminderStyle,
                onSelected = { reminderStyle = it }
            )
            DropdownField(
                label = "Break activity preference",
                options = breakActivities,
                selected = breakActivity,
                onSelected = { breakActivity = it }
            )

            /* G1 — section 4: Context sources */
            SectionHeader("Context sources")

            SettingSwitchRow(
                title = "Weather-based suggestions",
                subtitle = "Suggest an outdoor walk when conditions are suitable",
                checked = weatherAware,
                onCheckedChange = { weatherAware = it }
            )
            SettingSwitchRow(
                title = "Calendar awareness",
                subtitle = "Hold reminders while you are in a meeting",
                checked = calendarAware,
                onCheckedChange = { calendarAware = it }
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { /* A4: persist to Room / DataStore */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) { Text("Save settings") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(1.dp, ErgoGreen)
            ) { Text("Sign out") }

            Spacer(Modifier.height(24.dp))
        }
    }

    /* TimePicker dialogs (additional component). */
    if (showQuietStartPicker) {
        TimePickerDialogM3(
            title = "Quiet hours start",
            initial = quietStart,
            onDismiss = { showQuietStartPicker = false },
            onConfirm = { h, m ->
                quietStart = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                showQuietStartPicker = false
            }
        )
    }
    if (showQuietEndPicker) {
        TimePickerDialogM3(
            title = "Quiet hours end",
            initial = quietEnd,
            onDismiss = { showQuietEndPicker = false },
            onConfirm = { h, m ->
                quietEnd = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                showQuietEndPicker = false
            }
        )
    }
}

/* Reusable pieces */

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = ErgoGreen,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    HorizontalDivider(color = ErgoHairline)
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ErgoMutedText
            )
        }
        Spacer(Modifier.width(12.dp))
        // Additional component: Switch.
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    endLabels: Pair<String, String>
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            // G3: current value stays visible.
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ErgoGreen
            )
        }
        // Additional component: Slider.
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(endLabels.first, style = MaterialTheme.typography.labelSmall, color = ErgoMutedText)
            Text(endLabels.second, style = MaterialTheme.typography.labelSmall, color = ErgoMutedText)
        }
    }
}

/* Whole-row tap target that opens a TimePicker. A read-only text field can still
 * grab keyboard focus, so a clickable row fits G2 better. */
@Composable
private fun TimeFieldRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ErgoHairline),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            // G3: current value shown on the control.
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ErgoGreen
            )
            Spacer(Modifier.width(12.dp))
            Text("Change", style = MaterialTheme.typography.labelMedium, color = ErgoMutedText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Basic component: Expanded (Exposed) Dropdown Menu.
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* Material3 has no ready-made TimePickerDialog, so wrap TimePicker in a Dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogM3(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val (initHour, initMinute) = remember(initial) {
        val parts = initial.split(":")
        (parts.getOrNull(0)?.toIntOrNull() ?: 12) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    val state = rememberTimePickerState(
        initialHour = initHour,
        initialMinute = initMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                // Additional component: TimePicker.
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun SettingsScreenPreview() {
    ErgoDeskTheme { SettingsScreen(onLogout = {}) }
}
