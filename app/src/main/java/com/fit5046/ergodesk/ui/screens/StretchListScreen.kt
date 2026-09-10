package com.fit5046.ergodesk.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit5046.ergodesk.ui.theme.ErgoBorder
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import com.fit5046.ergodesk.ui.theme.ErgoTextPrimary
import com.fit5046.ergodesk.ui.theme.ErgoTextSecondary

private val HeaderBackground = Color(0xFFF7FBF2)
private val SoftGreenChip = Color(0xFFF1F8EE)

/**
 * Data model for stretch routines in the A2 prototype.
 */
data class StretchItem(
    val id: String,
    val title: String,
    val targetArea: String,
    val durationSeconds: Int,
    val difficulty: String,
    val instructions: List<String>
)

/**
 * Shared in-memory data store for A2 prototype demonstration.
 */
object StretchDataProvider {
    val items = mutableStateListOf(
        StretchItem(
            id = "1",
            title = "Seated Spinal Twist",
            targetArea = "Lower back",
            durationSeconds = 60,
            difficulty = "Easy",
            instructions = listOf(
                "Sit upright on the front edge of your chair with both feet flat on the floor.",
                "Place your left hand on your right knee and right hand on the chair back/armrest.",
                "Inhale to lengthen your spine, exhale and gently rotate your torso to the right.",
                "Hold for 30 seconds breathing steadily, then repeat on the left side."
            )
        ),
        StretchItem(
            id = "2",
            title = "Desk Neck Extension & Flexion",
            targetArea = "Neck",
            durationSeconds = 45,
            difficulty = "Easy",
            instructions = listOf(
                "Relax your shoulders completely and gently drop your chin down to your chest for 15s.",
                "Slowly raise your head back up and gaze gently toward the ceiling for 15s.",
                "Return to neutral, then gently tilt your right ear toward your right shoulder for 15s."
            )
        ),
        StretchItem(
            id = "3",
            title = "Shoulder Blade Squeeze",
            targetArea = "Shoulders",
            durationSeconds = 60,
            difficulty = "Easy",
            instructions = listOf(
                "Sit tall and bend your elbows at 90 degrees by your sides.",
                "Pull your elbows straight back while firmly squeezing your shoulder blades together.",
                "Hold the squeeze for 5 seconds, relax for 2 seconds, and repeat 8-10 times."
            )
        ),
        StretchItem(
            id = "4",
            title = "Wrist & Finger Flexor Stretch",
            targetArea = "Wrists",
            durationSeconds = 30,
            difficulty = "Quick",
            instructions = listOf(
                "Extend your right arm in front of you with palm facing forward and fingers up.",
                "Use your left hand to gently pull your right fingers back toward your body for 15s.",
                "Point fingers downward, pull gently back for 15s, then switch to your other hand."
            )
        ),
        StretchItem(
            id = "5",
            title = "Seated Chest Opener",
            targetArea = "Chest",
            durationSeconds = 45,
            difficulty = "Easy",
            instructions = listOf(
                "Sit forward on your chair and interlace your fingers behind your lower back.",
                "Roll your shoulders back and gently lift your chest upward toward the ceiling.",
                "Take 5 slow, deep breaths, feeling your chest and anterior shoulder muscles open up."
            )
        ),
        StretchItem(
            id = "6",
            title = "Seated Hamstring Reach",
            targetArea = "Legs",
            durationSeconds = 60,
            difficulty = "Medium",
            instructions = listOf(
                "Extend your right leg straight forward with your heel on the floor and toes up.",
                "Hinge forward from your hips with a flat back until you feel a mild stretch behind the thigh.",
                "Hold for 30 seconds breathing normally, then switch to your left leg."
            )
        )
    )

    fun getById(id: String): StretchItem? = items.find { it.id == id } ?: items.firstOrNull()
}

/**
 * Stretch List screen — Browse, search and manage ergonomic stretch routines.
 * Member 4 responsibility.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StretchListScreen(
    onStretchClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Neck", "Shoulders", "Lower back", "Wrists", "Chest", "Legs")

    // Real-time search & category filter
    val filteredList = StretchDataProvider.items.filter { item ->
        val matchesCategory = (selectedCategory == "All") || item.targetArea.equals(selectedCategory, ignoreCase = true)
        val matchesQuery = item.title.contains(searchQuery, ignoreCase = true) ||
                item.targetArea.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ErgoGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Stretch")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // ---------------------------------------------------------
            // 1. Unified App Header (Left-aligned, matching Member 3)
            // ---------------------------------------------------------
            StretchListHeader()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))

                // ---------------------------------------------------------
                // 2. Screen Title with Left Icon & Indented Subtitle
                // ---------------------------------------------------------
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SelfImprovement,
                        contentDescription = null,
                        tint = ErgoGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Exercises",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErgoTextPrimary
                    )
                }

                Text(
                    text = "Targeted desk routines to relieve tension and fatigue.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = ErgoTextSecondary,
                    modifier = Modifier.padding(start = 38.dp, top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------------------------------------------------------
                // 3. Search Bar with Clean Border
                // ---------------------------------------------------------
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by routine or body part...", fontSize = 13.sp, color = ErgoTextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ErgoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ErgoTextSecondary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = ErgoGreen,
                        unfocusedBorderColor = ErgoBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ---------------------------------------------------------
                // 4. Horizontal Filter Chips
                // ---------------------------------------------------------
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ErgoGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = ErgoTextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) ErgoGreen else ErgoBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ---------------------------------------------------------
                // 5. Scrollable Routine Cards Feed
                // ---------------------------------------------------------
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { stretch ->
                        StretchCard(
                            item = stretch,
                            onClick = { onStretchClick(stretch.id) }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Add Custom Stretch Dialog (Dropdown selection + Minutes/Seconds input)
    // -------------------------------------------------------------
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newArea by remember { mutableStateOf("Neck") }
        var areaDropdownExpanded by remember { mutableStateOf(false) }
        var durationMinutesInput by remember { mutableStateOf("1") }
        var durationSecondsInput by remember { mutableStateOf("0") }

        val areaOptions = listOf("Neck", "Shoulders", "Lower back", "Wrists", "Chest", "Legs")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Add Custom Stretch",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErgoTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Routine Name
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Routine Name *") },
                        placeholder = { Text("e.g. Desk Upper Back Stretch") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErgoGreen,
                            unfocusedBorderColor = ErgoBorder
                        ),
                        singleLine = true
                    )

                    // Target Body Area (Exposed Dropdown Menu)
                    ExposedDropdownMenuBox(
                        expanded = areaDropdownExpanded,
                        onExpandedChange = { areaDropdownExpanded = !areaDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = newArea,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Body Area *") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErgoGreen,
                                unfocusedBorderColor = ErgoBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = areaDropdownExpanded,
                            onDismissRequest = { areaDropdownExpanded = false }
                        ) {
                            areaOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 13.sp) },
                                    onClick = {
                                        newArea = option
                                        areaDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Duration (Separate integer Minutes & Seconds fields)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = durationMinutesInput,
                            onValueChange = { durationMinutesInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes *") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErgoGreen,
                                unfocusedBorderColor = ErgoBorder
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = durationSecondsInput,
                            onValueChange = { durationSecondsInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Seconds *") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErgoGreen,
                                unfocusedBorderColor = ErgoBorder
                            ),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val mins = durationMinutesInput.toIntOrNull() ?: 0
                            val secs = durationSecondsInput.toIntOrNull() ?: 0
                            val totalSecs = (mins * 60 + secs).coerceAtLeast(10)
                            val holdSeconds = (totalSecs / 2).coerceAtLeast(5)

                            StretchDataProvider.items.add(
                                0,
                                StretchItem(
                                    id = (StretchDataProvider.items.size + 1).toString(),
                                    title = newTitle.trim(),
                                    targetArea = newArea,
                                    durationSeconds = totalSecs,
                                    difficulty = "Custom",
                                    instructions = listOf(
                                        "Position yourself comfortably with your back straight and relaxed shoulders.",
                                        "Perform gentle, controlled movements focusing directly on your $newArea.",
                                        "Breathe steadily and hold the stretch for $holdSeconds seconds per side."
                                    )
                                )
                            )
                        }
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErgoGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Routine", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = ErgoTextSecondary)
                }
            }
        )
    }
}

/**
 * Top Global Header matching Member 3 (Left aligned + Profile Icon).
 */
@Composable
private fun StretchListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HeaderBackground
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

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "ErgoDesk",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErgoTextPrimary
                )

                Text(
                    text = "Move Better, Work Smarter",
                    fontSize = 9.sp,
                    color = ErgoTextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ErgoBorder),
                color = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonOutline,
                    contentDescription = "Profile",
                    tint = ErgoTextSecondary,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(22.dp)
                )
            }
        }
    }
}

/**
 * Individual Stretch Card component matching Member 3's card elevation & border.
 */
@Composable
private fun StretchCard(
    item: StretchItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ErgoBorder),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErgoTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle with | separation (e.g. "Lower back | 1 min | Easy")
                val durationText = when {
                    item.durationSeconds >= 60 && item.durationSeconds % 60 == 0 -> "${item.durationSeconds / 60} min"
                    item.durationSeconds >= 60 -> "${item.durationSeconds / 60}m ${item.durationSeconds % 60}s"
                    else -> "${item.durationSeconds}s"
                }

                val subtitleText = "${item.targetArea}  |  $durationText  |  ${item.difficulty}"

                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    color = ErgoTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Detail",
                tint = ErgoTextSecondary
            )
        }
    }
}
