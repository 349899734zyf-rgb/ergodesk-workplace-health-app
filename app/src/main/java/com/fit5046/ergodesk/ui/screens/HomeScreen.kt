package com.fit5046.ergodesk.ui.screens

/*
 * FIT5046 Assessment 2 — ErgoDesk
 * Member 2 — Home screen (skeleton prototype, UI only).
 *
 * Components here: LazyColumn, Card/Surface, Button/OutlinedButton,
 * CircularProgressIndicator, LinearProgressIndicator.
 * The bottom NavigationBar is app-wide (ErgoDeskBottomNavBar).
 * Route object is Screen.Dashboard ("dashboard"); the team label is "Home".
 *
 * A4: live posture, sitting bout, break interval, today's totals and the
 * activity timeline come from HomeViewModel; the weather card comes from
 * WeatherViewModel (Member 4).
 */

import android.widget.Toast
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fit5046.ergodesk.alarm.BreakAlarmScheduler
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.network.model.WeatherAdvice
import com.fit5046.ergodesk.data.sensor.ActivitySession
import com.fit5046.ergodesk.data.sensor.ActivitySessions
import com.fit5046.ergodesk.data.sensor.PostureState
import com.fit5046.ergodesk.data.sensor.SessionType
import com.fit5046.ergodesk.ui.home.HomeUiState
import com.fit5046.ergodesk.ui.home.HomeViewModel
import com.fit5046.ergodesk.ui.theme.ErgoDeskTheme
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import com.fit5046.ergodesk.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* One row of the "Recent activity" timeline. */

data class TimelineEvent(
    val time: String,
    val title: String,
    val detail: String,
    val isBreak: Boolean
)

/*
 * Newest first. The last session of the day is the one
 * still running, so it ends "now".
 */
private fun timelineEvents(
    sessions: List<ActivitySession>
): List<TimelineEvent> {

    val clock =
        SimpleDateFormat("HH:mm", Locale.ENGLISH)

    return sessions.mapIndexed { index, session ->

        val inProgress =
            index == sessions.lastIndex

        val isBreak =
            session.type == SessionType.BREAK

        val end =
            if (inProgress) "now" else clock.format(Date(session.endMillis))

        TimelineEvent(
            time = "${clock.format(Date(session.startMillis))} – $end",
            title = if (isBreak) "Break" else "Sitting",
            detail = AnalyticsCalculator.formatMinutes(session.minutes) +
                    if (inProgress) " · in progress" else "",
            isBreak = isBreak
        )
    }.asReversed()
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherAdvice by weatherViewModel.weatherAdvice.collectAsStateWithLifecycle()
    val sessions by viewModel.todaySessions.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        weatherAdvice = weatherAdvice,
        sessions = sessions,
        onTakeBreak = viewModel::takeBreakNow
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    weatherAdvice: WeatherAdvice,
    sessions: List<ActivitySession>,
    onTakeBreak: () -> Unit
) {

    val context = LocalContext.current

    val events = remember(sessions) {
        timelineEvents(sessions)
    }

    var showAllEvents by rememberSaveable {
        mutableStateOf(false)
    }

    val todayLabel = remember {
        AnalyticsCalculator.formatPeriodLabel(
            System.currentTimeMillis(),
            AnalyticsPeriod.DAILY
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        ErgoDeskHeader()

        // LazyColumn (required component).
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {

            /* Title + date */
            item {
                Column {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            tint = ErgoGreen,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Text(
                            "Home",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        todayLabel,
                        fontSize = 13.sp,
                        color = ErgoMutedText,
                        modifier = Modifier.padding(
                            start = 32.dp,
                            top = 2.dp
                        )
                    )
                }
            }

            /* Sitting progress ring */
            item {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ErgoSoftGreen
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            CircularProgressIndicator(
                                progress = {
                                    state.sittingProgress
                                },
                                modifier = Modifier.size(168.dp),
                                strokeWidth = 12.dp,
                                color = ErgoGreen,
                                trackColor = Color.White
                            )

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    "${state.sittingBoutMinutes}",
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErgoGreen
                                )

                                Text(
                                    "minutes sitting",
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    color = ErgoMutedText
                                )
                            }
                        }

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        Text(
                            "Now: ${state.postureLabel}",
                            fontSize = 13.sp,
                            color = ErgoMutedText
                        )

                        Spacer(
                            Modifier.height(4.dp)
                        )

                        Text(
                            state.statusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ErgoGreen
                        )
                    }
                }
            }

            /* Context-aware suggestion card */
            item {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        ErgoHairline
                    ),
                    color = Color.White
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ErgoGreen),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = weatherAdvice.temperatureFormatted,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(
                            Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                text = weatherAdvice.headline,
                                style =
                                    MaterialTheme.typography.titleSmall,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                text = weatherAdvice.subtitle,
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color = ErgoMutedText
                            )
                        }
                    }
                }
            }

            /* Today's stats */
            item {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    StatCard(
                        AnalyticsCalculator.formatMinutes(
                            state.sittingTodayMinutes
                        ),
                        "Sitting today",
                        Modifier.weight(1f)
                    )

                    StatCard(
                        "${state.breaksCompleted} / ${state.breakTarget}",
                        "Breaks completed",
                        Modifier.weight(1f)
                    )

                    StatCard(
                        AnalyticsCalculator.formatMinutes(
                            ActivitySessions.longestSittingMinutes(sessions)
                        ),
                        "Longest sit",
                        Modifier.weight(1f)
                    )
                }
            }

            /* Daily goal bar */
            item {

                Column {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            "Daily movement goal",
                            style =
                                MaterialTheme.typography.labelLarge
                        )

                        Text(
                            "${state.breaksCompleted} of ${state.breakTarget} breaks",
                            style =
                                MaterialTheme.typography.labelLarge,
                            color = ErgoGreen
                        )
                    }

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            state.breakProgress
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(
                                RoundedCornerShape(5.dp)
                            ),
                        color = ErgoGreen,
                        trackColor = ErgoSoftGreen
                    )
                }
            }

            /* Primary actions */
            item {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = {

                            onTakeBreak()

                            Toast.makeText(
                                context,
                                "Break logged. Enjoy a short stretch!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {

                        Text(
                            "Take a break now"
                        )
                    }

                    /*
                     * Person 3 — AlarmManager advanced feature.
                     *
                     * The user explicitly chooses to snooze
                     * the break reminder for 10 minutes.
                     */
                    OutlinedButton(
                        onClick = {

                            BreakAlarmScheduler
                                .scheduleBreakReminder(
                                    context = context,
                                    delayMinutes = 5
                                )

                            Toast.makeText(
                                context,
                                "Break reminder snoozed for 5 minutes",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        border = BorderStroke(
                            1.dp,
                            ErgoGreen
                        )
                    ) {

                        Text(
                            "Snooze 5 min"
                        )
                    }
                }
            }

            /* Recent activity heading */
            item {

                Text(
                    "Recent activity",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier.padding(top = 4.dp)
                )
            }

            /* Timeline: today's sessions from the sensor, newest first */
            if (events.isEmpty()) {

                item {

                    Text(
                        "No activity recorded yet today",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color = ErgoMutedText
                    )
                }
            }

            items(
                if (showAllEvents) events else events.take(3)
            ) { event ->

                TimelineRow(
                    event
                )
            }

            if (events.size > 3) {

                item {

                    TextButton(
                        onClick = {
                            showAllEvents = !showAllEvents
                        },
                        contentPadding =
                            PaddingValues(0.dp)
                    ) {

                        Text(
                            if (showAllEvents) "Show less" else "View all (${events.size})"
                        )
                    }
                }
            }
        }
    }
}

/* Reusable pieces */

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            ErgoHairline
        ),
        color = Color.White
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 16.dp,
                    horizontal = 8.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ErgoGreen,
                textAlign = TextAlign.Center
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                label,
                fontSize = 12.sp,
                color = ErgoMutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimelineRow(
    event: TimelineEvent
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (event.isBreak)
                        ErgoGreen
                    else
                        ErgoHairline
                )
        )

        Spacer(
            Modifier.width(14.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                event.title,
                style =
                    MaterialTheme.typography.bodyLarge
            )

            Text(
                event.detail,
                style =
                    MaterialTheme.typography.bodySmall,
                color = ErgoMutedText
            )
        }

        Text(
            event.time,
            style =
                MaterialTheme.typography.labelSmall,
            color = ErgoMutedText
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 393,
    heightDp = 851
)
@Composable
private fun HomeScreenPreview() {

    ErgoDeskTheme {
        HomeContent(
            HomeUiState(
                posture = PostureState.SITTING,
                sittingBoutMinutes = 22,
                sittingTodayMinutes = 380,
                breaksCompleted = 5
            ),
            WeatherAdvice(
                temperatureFormatted = "21°",
                headline = "Good time for an outdoor break",
                subtitle = "21 °C, sunny, UV 3 · Ideal for a 5-min walk",
                isOutdoorFriendly = true
            ),
            sessions = listOf(
                ActivitySession(SessionType.SITTING, 32_400_000L, 35_160_000L),
                ActivitySession(SessionType.BREAK, 35_160_000L, 35_520_000L),
                ActivitySession(SessionType.SITTING, 35_520_000L, 38_340_000L)
            ),
            onTakeBreak = {}
        )
    }
}