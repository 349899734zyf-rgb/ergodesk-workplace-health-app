package com.fit5046.ergodesk.ui.screens

/*
 * FIT5046 Assessment 2 — ErgoDesk
 * Member 2 — Settings Screen (skeleton prototype, UI only)
 *
 * 反向导航：由 AppNavHost 传入 onLogout（"Sign out" 按钮触发，回到 Login）。
 * 底部 NavigationBar 由 ErgoDeskBottomNavBar 全局提供，本屏不再自绘。
 *
 * 工作画像（上下班时间、职业、工位、不适部位）由 Member 3 的 ErgoForm
 * ergonomic assessment 维护并存进 Room，本屏只读展示、不重复采集。
 * Settings 只管 app 偏好。这里的 Quiet hours 是"免打扰时段"，
 * 和 ErgoForm 的"工作时段"是两回事。
 *
 * 本文件包含的 Android components：
 *   基础 —— Expanded (Exposed) Dropdown Menu ×2
 *   附加 —— Switch ×4、Slider ×2、TimePicker ×2（对话框）
 *
 * 同时演示 Step 4 的三条 FORM DESIGN GUIDELINES：
 *   G1 相关字段分组并加区块标题（Monitoring / Schedule / Reminders / Context sources）
 *   G2 控件类型匹配数据类型（时间用 TimePicker、区间用 Slider、选项用 Dropdown，杜绝格式错误）
 *   G3 每个控件旁边实时显示当前值（"30 min"、"12:30"），用户不用猜
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fit5046.ergodesk.ui.theme.ErgoDeskTheme
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit) {

    /* ── 骨架原型的本地状态；A4 时改由 ViewModel + Room/DataStore 提供 ── */
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        /* ── 用户信息条（只读；工作画像由 ErgoForm 的 ergonomic assessment 维护）── */
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "From your ergonomic assessment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        /* ══ G1: 区块一 —— Monitoring ══ */
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

        // G2 + G3: 用 Slider 而不是输入框；数值实时显示在右上角
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

        /* ══ G1: 区块二 —— Schedule ══ */
        SectionHeader("Schedule")

        // G2: 时间用 TimePicker 对话框输入，避免用户输入 "9pm"/"21:00" 这类格式错误
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        /* ══ G1: 区块三 —— Reminders ══ */
        SectionHeader("Reminders")

        // 基础组件：Expanded (Exposed) Dropdown Menu
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

        /* ══ G1: 区块四 —— Context sources ══ */
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
                .height(50.dp)
        ) { Text("Sign out") }

        Spacer(Modifier.height(24.dp))
    }

    /* ── TimePicker 对话框（附加组件）── */
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

/* ───────────────────────────────── 复用小组件 ───────────────────────────────── */

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    HorizontalDivider()
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        // 附加组件：Switch
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
            // G3: 当前值始终可见
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // 附加组件：Slider
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
            Text(
                endLabels.first,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                endLabels.second,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* 时间字段：整行可点击，弹出 TimePicker。
 * 不用 OutlinedTextField 是因为 readOnly 的输入框仍可能被键盘聚焦，
 * 用可点击的行更符合 G2「控件类型匹配数据类型」。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFieldRow(label: String, value: String, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            // G3: 当前值直接显示在控件上
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Change",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

    // 基础组件：Expanded (Exposed) Dropdown Menu
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

/* Material3 没有现成的 TimePickerDialog，这里用 Dialog 包一层 TimePicker */
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
                // 附加组件：TimePicker
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
