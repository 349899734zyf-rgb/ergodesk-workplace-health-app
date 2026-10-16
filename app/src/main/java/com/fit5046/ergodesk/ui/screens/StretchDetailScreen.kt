package com.fit5046.ergodesk.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fit5046.ergodesk.ui.viewmodel.StretchViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit5046.ergodesk.ui.theme.ErgoBorder
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import com.fit5046.ergodesk.ui.theme.ErgoGreenLight
import com.fit5046.ergodesk.ui.theme.ErgoTextPrimary
import com.fit5046.ergodesk.ui.theme.ErgoTextSecondary
import kotlinx.coroutines.delay

private val HeaderBackground = Color(0xFFF7FBF2)
private val SoftGreenBubble = Color(0xFFF1F8EE)

/**
 * Stretch Detail Screen — Guided routine instructions and interactive countdown timer.
 * Member 4 responsibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchDetailScreen(
    stretchId: String,
    onBack: () -> Unit,
    viewModel: StretchViewModel = viewModel()
) {
    val id = stretchId.toLongOrNull() ?: 1L
    val stretchEntity by viewModel.getStretchById(id).collectAsState(initial = null)

    val totalTimeSeconds = stretchEntity?.durationSeconds ?: 60
    var remainingSeconds by remember(stretchEntity?.id, totalTimeSeconds) { mutableStateOf(totalTimeSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var showCompletionBanner by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val instructionsList = remember(stretchEntity?.instructions) {
        stretchEntity?.instructions?.split("\n")?.filter { it.isNotBlank() } ?: listOf(
            "Position yourself comfortably with your back straight.",
            "Perform gentle, controlled movements.",
            "Breathe steadily and hold for 30 seconds."
        )
    }

    // Dynamic countdown loop
    LaunchedEffect(isTimerRunning, remainingSeconds) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (remainingSeconds == 0 && isTimerRunning) {
            isTimerRunning = false
            showCompletionBanner = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (totalTimeSeconds > 0) remainingSeconds.toFloat() / totalTimeSeconds.toFloat() else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "TimerProgress"
    )

    // Formatted duration string for clean display
    val durationLabel = when {
        totalTimeSeconds >= 60 && totalTimeSeconds % 60 == 0 -> "${totalTimeSeconds / 60} min (${totalTimeSeconds}s)"
        totalTimeSeconds >= 60 -> "${totalTimeSeconds / 60}m ${totalTimeSeconds % 60}s (${totalTimeSeconds}s)"
        else -> "${totalTimeSeconds}s"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ---------------------------------------------------------
        // Top App Bar
        // ---------------------------------------------------------
        TopAppBar(
            title = {
                Text(
                    text = stretchEntity?.title ?: "Desk Stretch",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErgoTextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to list",
                        tint = ErgoGreen
                    )
                }
            },
            actions = {
                if (stretchEntity?.isCustom == true) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Routine",
                            tint = Color(0xFFE53935)
                        )
                    }
                }
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE57373) else ErgoTextSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HeaderBackground
            )
        )

        // ---------------------------------------------------------
        // Main Content Area (Scrollable)
        // ---------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------------------------------------------------------
            // 1. Clean Duration Time Bubble
            // ---------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SoftGreenBubble,
                border = BorderStroke(1.dp, ErgoBorder),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = ErgoGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Session Duration: $durationLabel",
                        fontSize = 13.sp,
                        color = ErgoGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ---------------------------------------------------------
            // 2. Tailored Step-by-Step Instructions Card
            // ---------------------------------------------------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ErgoBorder),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step-by-Step Guidance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErgoTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftGreenBubble
                        ) {
                            Text(
                                text = stretchEntity?.targetArea ?: "General",
                                fontSize = 11.sp,
                                color = ErgoGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    instructionsList.forEachIndexed { index, stepText ->
                        InstructionBullet(
                            number = "${index + 1}",
                            text = stepText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------------------------------------------------
            // 3. Interactive Guided Timer Card
            // ---------------------------------------------------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ErgoBorder),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Interactive Stretch Timer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErgoTextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular Progress Timer
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(136.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 7.dp,
                            color = ErgoGreenLight,
                            trackColor = SoftGreenBubble
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErgoTextPrimary
                            )
                            Text(
                                text = "of %02d:%02d".format(totalTimeSeconds / 60, totalTimeSeconds % 60),
                                fontSize = 11.sp,
                                color = ErgoTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Timer Controls
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { isTimerRunning = !isTimerRunning },
                            colors = ButtonDefaults.buttonColors(containerColor = ErgoGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTimerRunning) "Pause" else "Start Timer", fontSize = 13.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                isTimerRunning = false
                                remainingSeconds = totalTimeSeconds
                                showCompletionBanner = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ErgoBorder)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = ErgoTextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 13.sp, color = ErgoTextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------------------------------------------------------
            // 4. Primary Completion Button (#2E7032)
            // ---------------------------------------------------------
            Button(
                onClick = {
                    showCompletionBanner = true
                    isTimerRunning = false
                    remainingSeconds = 0
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErgoGreen)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Stretch & Reset Fatigue", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Completion Banner
            if (showCompletionBanner) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SoftGreenBubble,
                    border = BorderStroke(1.dp, ErgoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Awesome! Stretch logged. Sedentary fatigue timer has been reset.",
                        fontSize = 12.sp,
                        color = ErgoGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ---------------------------------------------------------
        // Delete Confirmation Dialog for Custom Stretches
        // ---------------------------------------------------------
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Delete Routine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErgoTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${stretchEntity?.title}'? This action cannot be undone.",
                        fontSize = 14.sp,
                        color = ErgoTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteStretch(id)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = ErgoTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun InstructionBullet(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = ErgoGreen
        ) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = ErgoTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
