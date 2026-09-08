package com.fit5046.ergodesk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Analytics screen — sitting and active behaviour summary.
 * Member 3 responsibility.
 *
 * A2 requirements demonstrated:
 * - DatePicker: allows users to select a date for reviewing activity.
 * - Data visualisation: compares sitting and active time across the week.
 *
 * A2 skeleton:
 * Sample data is used to demonstrate the intended UI and visualisation.
 *
 * A4 extension:
 * The sample values will be replaced with real stored activity data
 * retrieved from Room and contextual activity information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    val dateState = rememberDatePickerState()

    var selectedDateLabel by remember {
        mutableStateOf("This week")
    }

    /*
     * Sample data for the A2 skeleton prototype.
     *
     * Values represent example hours recorded during a typical
     * monitored workday. They are NOT real user data.
     *
     * A4 will replace these values with stored activity records.
     */
    val dayLabels = listOf(
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    )

    val sittingHours = listOf(
        6.5f, 6.0f, 7.0f, 5.5f, 6.2f, 3.5f, 3.0f
    )

    val activeHours = listOf(
        1.8f, 2.2f, 1.5f, 2.8f, 2.0f, 4.0f, 4.5f
    )

    // Used only to scale the prototype chart consistently.
    val chartMaximumHours = 8f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 100.dp
            )
    ) {

        // ---------------------------------------------------------
        // Header
        // ---------------------------------------------------------

        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Review your sitting, activity and break patterns.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // ---------------------------------------------------------
        // Date selection
        // ---------------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "Selected period",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = selectedDateLabel,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            TextButton(
                onClick = {
                    showDatePicker = true
                }
            ) {
                Text("Choose date")
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ---------------------------------------------------------
        // Summary
        // ---------------------------------------------------------

        Text(
            text = "Activity Summary",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Example values for the selected period.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                top = 2.dp,
                bottom = 10.dp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StatCard(
                modifier = Modifier.weight(1f),
                title = "Sitting",
                value = "5h 20m",
                color = MaterialTheme.colorScheme.errorContainer
            )

            StatCard(
                modifier = Modifier.weight(1f),
                title = "Active",
                value = "2h 10m",
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "Breaks completed",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "6 of 8",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Text(
                    text = "75%",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // ---------------------------------------------------------
        // Chart
        // ---------------------------------------------------------

        Text(
            text = "Daily Activity — This Week",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Compare sitting and active hours for each day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Legend
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            LegendDot(
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = " Sitting",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            LegendDot(
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = " Active",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        /*
         * Grouped bar chart.
         *
         * Each day has TWO separate bars:
         * - red/error = sitting hours
         * - primary = active hours
         *
         * This is clearer than the previous stacked chart because
         * both values use the same scale and can be directly compared.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {

            dayLabels.forEachIndexed { index, day ->

                DayBarGroup(
                    day = day,
                    sittingHours = sittingHours[index],
                    activeHours = activeHours[index],
                    maximumHours = chartMaximumHours
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // ---------------------------------------------------------
        // Insight
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
                    text = "Weekly Insight",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Saturday and Sunday show more active time than weekdays. Consider adding short movement breaks during longer weekday sitting periods.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Prototype data shown for Assessment 2. Real activity records will be integrated in Assessment 4.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // -------------------------------------------------------------
    // Date picker
    // -------------------------------------------------------------

    if (showDatePicker) {

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val millis =
                            dateState.selectedDateMillis

                        if (millis != null) {

                            val localDate =
                                Instant
                                    .ofEpochMilli(millis)
                                    .atZone(
                                        ZoneId.systemDefault()
                                    )
                                    .toLocalDate()

                            selectedDateLabel =
                                localDate.format(
                                    DateTimeFormatter
                                        .ofPattern("dd MMM yyyy")
                                )
                        }

                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {

            DatePicker(
                state = dateState
            )
        }
    }
}


/**
 * Displays one day's sitting and active values using
 * two bars with the same scale.
 */
@Composable
private fun DayBarGroup(
    day: String,
    sittingHours: Float,
    activeHours: Float,
    maximumHours: Float
) {

    val maximumBarHeight = 125.dp

    val sittingHeight =
        maximumBarHeight *
                (sittingHours / maximumHours)
                    .coerceIn(0f, 1f)

    val activeHeight =
        maximumBarHeight *
                (activeHours / maximumHours)
                    .coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        Row(
            modifier = Modifier.height(maximumBarHeight),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {

            Box(
                modifier = Modifier
                    .width(9.dp)
                    .height(sittingHeight)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp
                        )
                    )
                    .background(
                        MaterialTheme.colorScheme.error
                    )
            )

            Box(
                modifier = Modifier
                    .width(9.dp)
                    .height(activeHeight)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp
                        )
                    )
                    .background(
                        MaterialTheme.colorScheme.primary
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = day,
            style = MaterialTheme.typography.bodySmall
        )
    }
}


/**
 * Summary statistic card.
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}


/**
 * Small coloured circle used in the chart legend.
 */
@Composable
private fun LegendDot(
    color: Color
) {

    Box(
        modifier = Modifier
            .width(10.dp)
            .height(10.dp)
            .clip(
                RoundedCornerShape(50)
            )
            .background(color)
    )
}