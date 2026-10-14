package com.fit5046.ergodesk.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit5046.ergodesk.data.analytics.AnalyticsCalculator
import com.fit5046.ergodesk.data.analytics.AnalyticsPeriod
import com.fit5046.ergodesk.data.analytics.ChartData
import com.fit5046.ergodesk.data.model.ActivityRecord
import com.fit5046.ergodesk.data.repository.RepositoryProvider
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import com.fit5046.ergodesk.ui.theme.ErgoGreenLight

private val SittingPink = Color(0xFFF4B7B7)
private val SittingBackground = Color(0xFFFFF1F1)
private val ActiveGreen = Color(0xFF72B99A)
private val ActiveBackground = Color(0xFFF0FAF2)
private val BreakBackground = Color(0xFFEAF7FA)
private val InsightBackground = Color(0xFFEAF7E4)
private val SelectorBackground = Color(0xFFF5F6F7)
private val SecondaryText = Color(0xFF666B66)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {

    var selectedPeriod by remember {
        mutableStateOf(AnalyticsPeriod.DAILY)
    }

    var selectedDateMillis by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    /*
     * Records come from ActivityRepository (Room). Each Flow
     * is re-created when the selected period or date changes,
     * and re-emits whenever a worker writes new minutes.
     */
    val context = LocalContext.current
    val repository = remember { RepositoryProvider.activityRepository(context) }

    val currentRange = remember(selectedPeriod, selectedDateMillis) {
        AnalyticsCalculator.periodRange(selectedPeriod, selectedDateMillis)
    }

    val previousRange = remember(selectedPeriod, selectedDateMillis) {
        AnalyticsCalculator.previousPeriodRange(selectedPeriod, selectedDateMillis)
    }

    // Daily view shows the whole selected week in "Weekly Progress".
    val weekRange = remember(selectedDateMillis) {
        AnalyticsCalculator.periodRange(AnalyticsPeriod.WEEKLY, selectedDateMillis)
    }

    val currentRecords by remember(currentRange) {
        repository.observeRecordsBetween(currentRange.startMillis, currentRange.endMillis)
    }.collectAsState(initial = emptyList())

    val previousRecords by remember(previousRange) {
        repository.observeRecordsBetween(previousRange.startMillis, previousRange.endMillis)
    }.collectAsState(initial = emptyList())

    val weekRecords by remember(weekRange) {
        repository.observeRecordsBetween(weekRange.startMillis, weekRange.endMillis)
    }.collectAsState(initial = emptyList())

    /*
     * All summary values are calculated from the records.
     */
    val current = AnalyticsCalculator.summarize(currentRecords)
    val previous = AnalyticsCalculator.summarize(previousRecords)

    val sittingMinutes = current.sittingMinutes
    val activeMinutes = current.activeMinutes
    val breaksCompleted = current.breaksCompleted
    val breakTarget = current.breakTarget

    val sittingChange = AnalyticsCalculator.percentageChange(
        current = sittingMinutes,
        previous = previous.sittingMinutes
    )

    val activeChange = AnalyticsCalculator.percentageChange(
        current = activeMinutes,
        previous = previous.activeMinutes
    )

    val breaksChange =
        breaksCompleted - previous.breaksCompleted

    val chartData = AnalyticsCalculator.buildChartData(
        period = selectedPeriod,
        records = currentRecords
    )

    val dateLabel = AnalyticsCalculator.formatPeriodLabel(
        millis = selectedDateMillis,
        period = selectedPeriod
    )

    val comparisonLabel =
        AnalyticsCalculator.comparisonLabel(selectedPeriod)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {

        AnalyticsHeader()

        Column(
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = ErgoGreen,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "Analytics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Track your sitting time, activity and break habits.",
                fontSize = 13.sp,
                color = Color(0xFF5F6368),
                modifier = Modifier.padding(
                    start = 38.dp,
                    top = 2.dp
                )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            /*
             * Date selector
             */
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePicker = true
                    },
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFD9DDDA)
                ),
                color = Color.White
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(21.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Text(
                        text = dateLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Choose date",
                        tint = SecondaryText
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            AnalyticsPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = {
                    selectedPeriod = it
                }
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            /*
             * Dynamically calculated summary cards
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                AnalyticsMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Chair,
                    background = SittingBackground,
                    title = "Sitting Time",
                    value = AnalyticsCalculator.formatMinutes(sittingMinutes),
                    comparison = AnalyticsCalculator.formatPercentageChange(sittingChange),
                    comparisonLabel = comparisonLabel,
                    lowerIsBetter = true
                )

                AnalyticsMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.DirectionsWalk,
                    background = ActiveBackground,
                    title = "Active Time",
                    value = AnalyticsCalculator.formatMinutes(activeMinutes),
                    comparison = AnalyticsCalculator.formatPercentageChange(activeChange),
                    comparisonLabel = comparisonLabel,
                    lowerIsBetter = false
                )

                AnalyticsMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Coffee,
                    background = BreakBackground,
                    title = "Breaks\nCompleted",
                    value = "$breaksCompleted / $breakTarget",
                    comparison = AnalyticsCalculator.formatNumberChange(breaksChange),
                    comparisonLabel = comparisonLabel,
                    lowerIsBetter = false
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Sitting vs Active Time",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                AnalyticsLegend(
                    color = SittingPink,
                    label = "Sitting"
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                AnalyticsLegend(
                    color = ActiveGreen,
                    label = "Active"
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AnalyticsBarChart(
                chartData = chartData
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            InsightCard(
                text = AnalyticsCalculator.createInsight(
                    sittingChange = sittingChange,
                    activeChange = activeChange,
                    breaksCompleted = breaksCompleted,
                    breakTarget = breakTarget
                )
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = when (selectedPeriod) {
                    AnalyticsPeriod.DAILY ->
                        "Weekly Progress"

                    AnalyticsPeriod.WEEKLY ->
                        "Daily Break Progress"

                    AnalyticsPeriod.MONTHLY ->
                        "Break Progress"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            BreakProgressSection(
                period = selectedPeriod,
                currentRecords = currentRecords,
                weekRecords = weekRecords
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Prototype activity records are used for Assessment 2. " +
                        "Daily, weekly and monthly results are dynamically calculated " +
                        "from these records. Room and activity-sensing data can replace " +
                        "the prototype data source in Assessment 4.",
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = Color(0xFF8A8A8A)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }

    /*
     * Material 3 DatePicker
     */
    if (showDatePicker) {

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
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
                state = datePickerState
            )
        }
    }
}

@Composable
private fun AnalyticsHeader() {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF7FBF2)
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Eco,
                contentDescription = null,
                tint = ErgoGreen,
                modifier = Modifier.size(31.dp)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "ErgoDesk",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Move Better, Work Smarter",
                    fontSize = 9.sp,
                    color = Color(0xFF646A64)
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFD6DDD6)
                ),
                color = Color.White
            ) {

                Icon(
                    imageVector = Icons.Filled.PersonOutline,
                    contentDescription = "Profile",
                    tint = SecondaryText,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsPeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {

    val periods = listOf(
        AnalyticsPeriod.DAILY,
        AnalyticsPeriod.WEEKLY,
        AnalyticsPeriod.MONTHLY
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SelectorBackground,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {

        periods.forEach { period ->

            val isSelected =
                period == selectedPeriod

            val label = when (period) {
                AnalyticsPeriod.DAILY -> "Daily"
                AnalyticsPeriod.WEEKLY -> "Weekly"
                AnalyticsPeriod.MONTHLY -> "Monthly"
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onPeriodSelected(period)
                    },
                shape = RoundedCornerShape(7.dp),
                color = if (isSelected) {
                    ErgoGreen
                } else {
                    Color.Transparent
                }
            ) {

                Box(
                    modifier = Modifier.padding(
                        vertical = 9.dp
                    ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (isSelected) {
                            Color.White
                        } else {
                            Color(0xFF59615B)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    modifier: Modifier,
    icon: ImageVector,
    background: Color,
    title: String,
    value: String,
    comparison: String,
    comparisonLabel: String,
    lowerIsBetter: Boolean
) {

    val isUp =
        comparison.startsWith("↑")

    val isDown =
        comparison.startsWith("↓")

    val isPositive = when {
        lowerIsBetter && isDown -> true
        !lowerIsBetter && isUp -> true
        else -> false
    }

    val comparisonColor = when {
        comparison == "—" ->
            Color.Gray

        isPositive ->
            ErgoGreen

        else ->
            Color(0xFFD65A5A)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = background
    ) {

        Column(
            modifier = Modifier.padding(11.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ErgoGreen,
                modifier = Modifier.size(20.dp)
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = title,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                color = Color(0xFF303530)
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = comparison,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = comparisonColor
            )

            Text(
                text = comparisonLabel,
                fontSize = 8.sp,
                color = Color(0xFF777D78)
            )
        }
    }
}

@Composable
private fun AnalyticsLegend(
    color: Color,
    label: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(
                    width = 18.dp,
                    height = 8.dp
                )
                .background(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                )
        )

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF626762)
        )
    }
}

@Composable
private fun AnalyticsBarChart(
    chartData: ChartData
) {

    if (
        chartData.sittingValues.isEmpty() ||
        chartData.activeValues.isEmpty()
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "No activity data available",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        return
    }

    val maxValue =
        (
                chartData.sittingValues +
                        chartData.activeValues
                )
            .maxOrNull()
            ?.coerceAtLeast(1f)
            ?: 1f

    Column {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 8.dp
                )
        ) {

            val graphHeight =
                size.height

            val groupWidth =
                size.width /
                        chartData.sittingValues.size

            /*
             * Horizontal grid lines
             */
            for (i in 0..4) {

                val y =
                    graphHeight -
                            (
                                    graphHeight *
                                            i / 4f
                                    )

                drawLine(
                    color = Color(0xFFE8EBE8),
                    start = Offset(
                        x = 0f,
                        y = y
                    ),
                    end = Offset(
                        x = size.width,
                        y = y
                    ),
                    strokeWidth = 1.dp.toPx()
                )
            }

            chartData.sittingValues.indices.forEach { index ->

                val centerX =
                    groupWidth * index +
                            groupWidth / 2f

                val sittingHeight =
                    graphHeight *
                            (
                                    chartData.sittingValues[index] /
                                            maxValue
                                    )

                val activeHeight =
                    graphHeight *
                            (
                                    chartData.activeValues[index] /
                                            maxValue
                                    )

                drawLine(
                    color = SittingPink,
                    start = Offset(
                        x = centerX - 6.dp.toPx(),
                        y = graphHeight
                    ),
                    end = Offset(
                        x = centerX - 6.dp.toPx(),
                        y = graphHeight - sittingHeight
                    ),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = ActiveGreen,
                    start = Offset(
                        x = centerX + 6.dp.toPx(),
                        y = graphHeight
                    ),
                    end = Offset(
                        x = centerX + 6.dp.toPx(),
                        y = graphHeight - activeHeight
                    ),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            chartData.labels.forEach { label ->

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = label,
                        fontSize = if (
                            chartData.labels.size > 7
                        ) {
                            7.sp
                        } else {
                            9.sp
                        },
                        color = Color(0xFF656A65)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    text: String
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = InsightBackground
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = ErgoGreen,
                modifier = Modifier.size(30.dp)
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Insight",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF303530)
                )
            }
        }
    }
}

@Composable
private fun BreakProgressSection(
    period: AnalyticsPeriod,
    currentRecords: List<ActivityRecord>,
    weekRecords: List<ActivityRecord>
) {

    /*
     * Daily view shows the selected week's progress.
     * Weekly view shows that week's daily progress.
     * Monthly view summarises each week of the month.
     */
    when (period) {

        AnalyticsPeriod.DAILY,
        AnalyticsPeriod.WEEKLY -> {

            val records = if (
                period == AnalyticsPeriod.DAILY
            ) {
                weekRecords
            } else {
                currentRecords
            }

            if (records.isEmpty()) {

                Text(
                    text = "No break data available.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                return
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                records.forEach { record ->

                    val label =
                        AnalyticsCalculator.weekdayLabel(
                            record.timestamp
                        )

                    BreakProgressItem(
                        modifier = Modifier.weight(1f),
                        label = label,
                        completed = record.breaksCompleted,
                        target = record.breakTarget
                    )
                }
            }
        }

        AnalyticsPeriod.MONTHLY -> {

            val weeklyGroups =
                currentRecords.chunked(7)

            if (weeklyGroups.isEmpty()) {

                Text(
                    text = "No break data available.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                return
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                weeklyGroups.forEachIndexed { index, week ->

                    val completed =
                        week.sumOf {
                            it.breaksCompleted
                        }

                    val target =
                        week.sumOf {
                            it.breakTarget
                        }

                    BreakProgressItem(
                        modifier = Modifier.weight(1f),
                        label = "W${index + 1}",
                        completed = completed,
                        target = target
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakProgressItem(
    modifier: Modifier,
    label: String,
    completed: Int,
    target: Int
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(50),
            color = Color(0xFFF1F8EE),
            border = BorderStroke(
                width = 3.dp,
                color = if (
                    target > 0 &&
                    completed >= target
                ) {
                    ErgoGreen
                } else {
                    ErgoGreenLight
                }
            )
        ) {

            Box(
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "$completed/$target",
                    fontSize = if (
                        completed >= 10 ||
                        target >= 10
                    ) {
                        7.sp
                    } else {
                        8.sp
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = label,
            fontSize = 8.sp,
            color = SecondaryText
        )
    }
}
