package com.fit5046.ergodesk.ui.screens

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

import androidx.compose.ui.platform.LocalContext
import com.fit5046.ergodesk.data.local.AppSettings
import com.fit5046.ergodesk.data.local.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    //DataStore Repository
    val context = LocalContext.current
    val repository = remember(context) {SettingsRepository(context.applicationContext)}
    val scope = rememberCoroutineScope()

    //Current Firebase User
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.displayName?.takeIf {it.isNotBlank()} ?: currentUser?.email ?: "User"

    //Settings Values
    var monitoringEnabled by remember {mutableStateOf(true)}
    var smartMode by remember {mutableStateOf(true)}
    var weatherAware by remember {mutableStateOf(true)}
    var calendarAware by remember {mutableStateOf(false)}

    var breakInterval by remember {mutableFloatStateOf(30f)}
    var movementSensitivity by remember {mutableFloatStateOf(3f)}

    var quietStart by remember {mutableStateOf("12:30")}
    var quietEnd by remember {mutableStateOf("13:30")}
    var showQuietStartPicker by remember {mutableStateOf(false)}
    var showQuietEndPicker by remember {mutableStateOf(false)}

    val reminderStyles = listOf("Notification", "Sound + notification", "Vibration only", "Silent banner")
    var reminderStyle by remember {mutableStateOf(reminderStyles.first())}

    val breakActivities = listOf("Stretch", "Walk", "Eye rest")
    var breakActivity by remember {mutableStateOf(breakActivities.first())}

    var settingsLoaded by remember {mutableStateOf(false)}
    var isSaving by remember {mutableStateOf(false)}
    var saveMessage by remember {mutableStateOf("")}

    LaunchedEffect(repository) {
        try {
            val saved = repository.settings.first()
            monitoringEnabled = saved.monitoringEnabled
            smartMode = saved.smartMode
            weatherAware = saved.weatherAware
            calendarAware = saved.calendarAware
            breakInterval = saved.breakInterval
            movementSensitivity = saved.movementSensitivity
            quietStart = saved.quietStart
            quietEnd = saved.quietEnd
            reminderStyle = saved.reminderStyle
            breakActivity = saved.breakActivity
        } catch (error: Exception) {
            saveMessage = "Unable to load saved settings!"
        } finally {
            settingsLoaded = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        ErgoDeskHeader()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    tint = ErgoGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            //User Card: Current Logged-In User
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = ErgoSoftGreen) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(userName, style = MaterialTheme.typography.titleMedium)
                    Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodySmall, color = ErgoMutedText)
                }
            }

            //G1 — Section 1: Monitoring
            SectionHeader("Monitoring")

            SettingSwitchRow(
                title = "Enable sitting monitoring",
                subtitle = "Track sitting time in the background",
                checked = monitoringEnabled,
                onCheckedChange = {monitoringEnabled = it}
            )
            SettingSwitchRow(
                title = "Smart context-aware mode",
                subtitle = "Automatically adjust the break interval from your recent break history",
                checked = smartMode,
                onCheckedChange = {smartMode = it}
            )

            //G2 + G3: A Slider instead of a Text Field; current value shown top-right
            SettingSliderRow(
                title = "Break interval",
                valueLabel = "${breakInterval.roundToInt()} min",
                value = breakInterval,
                onValueChange = {breakInterval = it},
                valueRange = 20f..90f,
                steps = 13,
                endLabels = "20 min" to "90 min"
            )
            SettingSliderRow(
                title = "Movement sensitivity",
                valueLabel = "Level ${movementSensitivity.roundToInt()}",
                value = movementSensitivity,
                onValueChange = {movementSensitivity = it},
                valueRange = 1f..5f,
                steps = 3,
                endLabels = "Relaxed" to "Sensitive"
            )

            //G1 — Section 2: Schedule
            SectionHeader("Schedule")

            //G2: Time is entered through a TimePicker dialog, so "9pm" / "21:00" style input errors are impossible
            TimeFieldRow(
                label = "Quiet hours start",
                value = quietStart,
                onClick = {showQuietStartPicker = true}
            )
            TimeFieldRow(
                label = "Quiet hours end",
                value = quietEnd,
                onClick = {showQuietEndPicker = true}
            )
            Text(
                "Reminders are muted during these hours (e.g. lunch or a focus block).",
                style = MaterialTheme.typography.bodySmall,
                color = ErgoMutedText,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            //G1 — Section 3: Reminders
            SectionHeader("Reminders")

            //Basic Component: Expanded (Exposed) Dropdown Menu.
            DropdownField(
                label = "Reminder style",
                options = reminderStyles,
                selected = reminderStyle,
                onSelected = {reminderStyle = it}
            )
            DropdownField(
                label = "Break activity preference",
                options = breakActivities,
                selected = breakActivity,
                onSelected = {breakActivity = it}
            )

            //G1 — Section 4: Context Sources
            SectionHeader("Context sources")

            SettingSwitchRow(
                title = "Weather-based suggestions",
                subtitle = "Suggest an outdoor walk when conditions are suitable",
                checked = weatherAware,
                onCheckedChange = {weatherAware = it}
            )
            SettingSwitchRow(
                title = "Calendar awareness",
                subtitle = "Hold reminders while you are in a meeting",
                checked = calendarAware,
                onCheckedChange = {calendarAware = it}
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        saveMessage = ""

                        try {
                            repository.saveSettings(
                                AppSettings(
                                    monitoringEnabled = monitoringEnabled,
                                    smartMode = smartMode,
                                    weatherAware = weatherAware,
                                    calendarAware = calendarAware,
                                    breakInterval = breakInterval,
                                    movementSensitivity = movementSensitivity,
                                    quietStart = quietStart,
                                    quietEnd = quietEnd,
                                    reminderStyle = reminderStyle,
                                    breakActivity = breakActivity
                                )
                            )

                            saveMessage = "Settings saved successfully!"

                        } catch (error: Exception) {
                            saveMessage = "Unable to save settings."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = settingsLoaded && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save settings")
            }

            if (saveMessage.isNotEmpty()) {
                Text(
                    text = saveMessage,
                    color = if (saveMessage == "Settings saved successfully!")
                        ErgoGreen
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

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

    //TimePicker dialogs (Additional Component)
    if (showQuietStartPicker) {
        TimePickerDialogM3(
            title = "Quiet hours start",
            initial = quietStart,
            onDismiss = {showQuietStartPicker = false},
            onConfirm = {h, m ->
                quietStart = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                showQuietStartPicker = false
            }
        )
    }
    if (showQuietEndPicker) {
        TimePickerDialogM3(
            title = "Quiet hours end",
            initial = quietEnd,
            onDismiss = {showQuietEndPicker = false},
            onConfirm = {h, m ->
                quietEnd = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                showQuietEndPicker = false
            }
        )
    }
}

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
        //Additional Component: Switch.
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
            //G3: Current value stays visible
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ErgoGreen
            )
        }
        //Additional Component: Slider.
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

//Whole-row tap target that opens a TimePicker. A read-only text field can still grab keyboard focus, so a clickable row fits G2 better. */
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
            //G3: Current value shown on the control.
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
    var expanded by remember {mutableStateOf(false)}

    //Basic Component: Expanded (Exposed) Dropdown Menu
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {expanded = it},
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { },
            readOnly = true,
            label = {Text(label)},
            trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {expanded = false}
        ) {
            options.forEach {option ->
                DropdownMenuItem(
                    text = {Text(option)},
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

//Material3 has no ready-made TimePickerDialog, so wrap TimePicker in a Dialog
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
                //Additional Component: TimePicker
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {Text("Cancel")}
                    TextButton(onClick = {onConfirm(state.hour, state.minute)}) {Text("OK")}
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun SettingsScreenPreview() {
    ErgoDeskTheme {SettingsScreen(onLogout = {})}
}
