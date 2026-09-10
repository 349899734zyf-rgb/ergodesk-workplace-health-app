package com.fit5046.ergodesk.ui.screens

/*
 * FIT5046 Assessment 2 — ErgoDesk
 * Member 2 — Dashboard Screen (skeleton prototype, UI only)
 *
 * Android components in this file:
 *   LazyColumn, Card, Button / OutlinedButton,
 *   CircularProgressIndicator, LinearProgressIndicator (progress indicators).
 *   The bottom NavigationBar is provided app-wide by ErgoDeskBottomNavBar
 *   (see navigation/), so this screen no longer draws its own.
 *
 * A4: replace the hard-coded state below with a ViewModel + Room / context sources.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit5046.ergodesk.ui.theme.ErgoDeskTheme

/* ───────────────────────── 假数据（骨架原型，A4 时再换成 Room/ViewModel） ───────────────────────── */

data class TimelineEvent(
    val time: String,
    val title: String,
    val detail: String,
    val isBreak: Boolean
)

private val sampleTimeline = listOf(
    TimelineEvent("09:02 – 09:48", "Sitting", "46 min · desk", false),
    TimelineEvent("09:48 – 09:54", "Break taken", "Outdoor walk · 6 min", true),
    TimelineEvent("09:54 – 10:41", "Sitting", "47 min · slouch detected", false),
    TimelineEvent("10:41 – 10:47", "Break taken", "Desk stretch · 6 min", true),
    TimelineEvent("10:47 – 11:29", "Sitting", "42 min · in progress", false)
)

/* ───────────────────────────────────── 主屏幕 ───────────────────────────────────── */

@Composable
fun DashboardScreen() {

    // 骨架原型：状态写死，A4 时由 ViewModel 提供
    // breakThresholdMinutes 与 Settings 的 "Break interval" 滑块默认值保持一致（35 min）
    val breakThresholdMinutes = 35
    val currentSittingMinutes = 32
    val sittingProgress = currentSittingMinutes / breakThresholdMinutes.toFloat()
    val breaksTaken = 4
    val breaksGoal = 8

    // LazyColumn —— 作业要求的六大基础组件之一
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {

        /* ── 0. 标题 + 日期 ── */
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Tuesday, 8 September",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        /* ── 1. 当前久坐进度环 ── */
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // 附加组件：Progress indicator
                        CircularProgressIndicator(
                            progress = { sittingProgress },
                            modifier = Modifier.size(168.dp),
                            strokeWidth = 12.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$currentSittingMinutes",
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "minutes sitting",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Next break in ${breakThresholdMinutes - currentSittingMinutes} minutes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        /* ── 2. 上下文感知建议卡片（体现 1.4 context-awareness）── */
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) { Text("21°", color = Color.White, fontWeight = FontWeight.Bold) }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            "Good time for an outdoor break",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "21 °C, clear, UV 3 · no meeting until 12:30",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        /* ── 3. 今日统计 ── */
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("4 h 12 m", "Sitting today", Modifier.weight(1f))
                StatCard("$breaksTaken / $breaksGoal", "Breaks completed", Modifier.weight(1f))
                StatCard("82", "Posture score", Modifier.weight(1f))
            }
        }

        /* ── 4. 日目标进度条 ── */
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily movement goal", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "$breaksTaken of $breaksGoal breaks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                // 附加组件：Progress indicator（线性）
                LinearProgressIndicator(
                    progress = { breaksTaken / breaksGoal.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            }
        }

        /* ── 5. 主操作按钮 ── */
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* A4: start break session */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) { Text("Take a break now") }

                OutlinedButton(
                    onClick = { /* A4: snooze reminder */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) { Text("Snooze 10 min") }
            }
        }

        /* ── 6. 今日时间线标题 ── */
        item {
            Text(
                "Today's activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        /* ── 7. 时间线列表项 ── */
        items(sampleTimeline) { event -> TimelineRow(event) }
    }
}

/* ───────────────────────────────── 复用小组件 ───────────────────────────────── */

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimelineRow(event: TimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (event.isBreak) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                event.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            event.time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun DashboardScreenPreview() {
    ErgoDeskTheme { DashboardScreen() }
}
