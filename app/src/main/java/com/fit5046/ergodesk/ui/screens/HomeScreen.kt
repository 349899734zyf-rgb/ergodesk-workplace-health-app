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
 * A4: replace the hard-coded state with a ViewModel reading ActivityRecord
 * from Room plus live context sources.
 */

import android.widget.Toast
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
import com.fit5046.ergodesk.alarm.BreakAlarmScheduler
import com.fit5046.ergodesk.ui.theme.ErgoDeskTheme
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fit5046.ergodesk.ui.viewmodel.WeatherViewModel

/* Sample data — replaced by Room / ViewModel in A4. */

data class TimelineEvent(
    val time: String,
    val title: String,
    val detail: String,
    val isBreak: Boolean
)

private val sampleTimeline = listOf(
    TimelineEvent(
        "09:02 – 09:48",
        "Sitting",
        "46 min · desk",
        false
    ),
    TimelineEvent(
        "09:48 – 09:54",
        "Break taken",
        "Outdoor walk · 6 min",
        true
    ),
    TimelineEvent(
        "09:54 – 10:41",
        "Sitting",
        "47 min · desk",
        false
    ),
    TimelineEvent(
        "10:41 – 10:47",
        "Break taken",
        "Desk stretch · 6 min",
        true
    ),
    TimelineEvent(
        "10:47 – 11:29",
        "Sitting",
        "42 min · in progress",
        false
    )
)

@Composable
fun HomeScreen(
    weatherViewModel: WeatherViewModel = viewModel()
) {

    val context = LocalContext.current
    val weatherAdvice by weatherViewModel.weatherAdvice.collectAsState()

    // Prototype: hard-coded state; A4 supplies this from a ViewModel.
    // 30-min break baseline matches the Settings slider default and the report.
    val breakThresholdMinutes = 30
    val currentSittingMinutes = 22
    val sittingProgress =
        currentSittingMinutes / breakThresholdMinutes.toFloat()

    val breaksCompleted = 5
    val breaksGoal = 8

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
                        "Tue, 8 Sep 2026",
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
                                    sittingProgress
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
                                    "$currentSittingMinutes",
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
                            "Next break in " +
                                    "${breakThresholdMinutes - currentSittingMinutes} minutes",
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
                        "6 h 20 m",
                        "Sitting today",
                        Modifier.weight(1f)
                    )

                    StatCard(
                        "$breaksCompleted / $breaksGoal",
                        "Breaks completed",
                        Modifier.weight(1f)
                    )

                    StatCard(
                        "52m",
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
                            "$breaksCompleted of $breaksGoal breaks",
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
                            breaksCompleted /
                                    breaksGoal.toFloat()
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
                            /* A4: start break session */
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
                                    delayMinutes = 10
                                )

                            Toast.makeText(
                                context,
                                "Break reminder snoozed for 10 minutes",
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
                            "Snooze 10 min"
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

            /* Timeline: latest 3 rows */
            items(
                sampleTimeline.take(3)
            ) { event ->

                TimelineRow(
                    event
                )
            }

            item {

                TextButton(
                    onClick = {
                        /* A4: open full activity history */
                    },
                    contentPadding =
                        PaddingValues(0.dp)
                ) {

                    Text(
                        "View all"
                    )
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
        HomeScreen()
    }
}