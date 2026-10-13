package com.fit5046.ergodesk.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit5046.ergodesk.data.model.ErgonomicAssessment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fit5046.ergodesk.data.repository.toErgonomicAssessment
import com.fit5046.ergodesk.data.repository.toRoomAssessment
import com.fit5046.ergodesk.viewmodel.AssessmentViewModel
import com.fit5046.ergodesk.ui.theme.ErgoGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FormHeaderBackground = Color(0xFFF7FBF2)
private val FormSoftGreen = Color(0xFFF1F8EE)
private val FormBorder = Color(0xFFD9DDD9)
private val FormSecondaryText = Color(0xFF666B66)
private val FormDeleteRed = Color(0xFFC94B4B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErgoFormScreen(assessmentViewModel: AssessmentViewModel = viewModel()) {

    /*
     * These are fixed domain options, not user data.
     * Keeping fixed form choices in code is appropriate
     * for this A2 prototype.
     */
    val jobTypes = listOf(
        "University student",
        "Office worker",
        "Developer",
        "Other desk-based user"
    )

    val discomfortAreas = listOf(
        "None",
        "Neck",
        "Shoulders",
        "Lower back",
        "Wrists"
    )

    val concernOptions = listOf(
        "Neck",
        "Shoulders",
        "Lower back",
        "Wrists"
    )

    var jobType by remember {
        mutableStateOf(jobTypes.first())
    }

    var mainDiscomfort by remember {
        mutableStateOf(discomfortAreas.first())
    }

    var assessmentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var startTime by remember {
        mutableStateOf("09:00")
    }

    var endTime by remember {
        mutableStateOf("17:00")
    }

    var concerns by remember {
        mutableStateOf(emptySet<String>())
    }

    var notes by remember {
        mutableStateOf("")
    }

    var jobExpanded by remember {
        mutableStateOf(false)
    }

    var discomfortExpanded by remember {
        mutableStateOf(false)
    }

    var showStartTimePicker by remember {
        mutableStateOf(false)
    }

    var showEndTimePicker by remember {
        mutableStateOf(false)
    }

    var editingAssessmentId by remember {
        mutableStateOf<Long?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val startTimePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true
    )

    val endTimePickerState = rememberTimePickerState(
        initialHour = 17,
        initialMinute = 0,
        is24Hour = true
    )

    val roomAssessments by assessmentViewModel.allAssessments
        .collectAsState(initial = emptyList())

    val savedAssessments = roomAssessments.map { it.toErgonomicAssessment() }

    fun resetForm() {
        jobType = jobTypes.first()
        mainDiscomfort = discomfortAreas.first()
        assessmentDate = System.currentTimeMillis()
        startTime = "09:00"
        endTime = "17:00"
        concerns = emptySet()
        notes = ""
        editingAssessmentId = null
    }

    fun loadAssessment(
        assessment: ErgonomicAssessment
    ) {
        jobType = assessment.jobType
        mainDiscomfort = assessment.mainDiscomfortArea
        assessmentDate = assessment.createdAt
        startTime = assessment.workStartTime
        endTime = assessment.workEndTime
        concerns = assessment.concernAreas
        notes = assessment.notes
        editingAssessmentId = assessment.id

        message = "Assessment selected. You can now edit it."
    }

    fun saveAssessment() {
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startMinutes = startParts.getOrNull(0)?.toIntOrNull()?.let { hour ->
            startParts.getOrNull(1)?.toIntOrNull()?.let { minute -> hour * 60 + minute }
        } ?: -1
        val endMinutes = endParts.getOrNull(0)?.toIntOrNull()?.let { hour ->
            endParts.getOrNull(1)?.toIntOrNull()?.let { minute -> hour * 60 + minute }
        } ?: -1

        if (startMinutes < 0 || endMinutes <= startMinutes) {
            message = "End time must be later than start time."
            return
        }

        val editingId = editingAssessmentId
        val existing = roomAssessments.firstOrNull { it.uid.toLong() == editingId }

        if (editingId != null && existing == null) {
            message = "Assessment not found."
            return
        }

        val assessment = ErgonomicAssessment(
            id = existing?.uid?.toLong() ?: 0L,
            createdAt = assessmentDate,
            jobType = jobType,
            mainDiscomfortArea = mainDiscomfort,
            workStartTime = startTime,
            workEndTime = endTime,
            concernAreas = concerns,
            notes = notes
        )

        if (existing == null) {
            assessmentViewModel.insertAssessment(assessment.toRoomAssessment()) { success ->
                if (success) {
                    resetForm()
                    message = "Assessment saved successfully."
                } else {
                    message = "Failed to save assessment."
                }
            }
        } else {
            assessmentViewModel.updateAssessment(
                assessment.toRoomAssessment(active = existing.isActive)
            ) { success ->
                if (success) {
                    resetForm()
                    message = "Assessment updated successfully."
                } else {
                    message = "Failed to update assessment."
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item { ErgoFormHeader() }
        item {
            Column(
                modifier = Modifier.padding(
                    horizontal = 18.dp
                )
            ) {

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Filled.Assignment,
                        contentDescription = null,
                        tint = ErgoGreen,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Ergonomic Assessment",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Tell us about your work habits so ErgoDesk can personalise your experience.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = FormSecondaryText,
                    modifier = Modifier.padding(
                        start = 38.dp,
                        top = 3.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                FormSectionTitle(
                    number = "1",
                    title = "About Your Work"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Job Type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                jobExpanded = true
                            },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            FormBorder
                        ),
                        color = Color.White
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 13.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = jobType,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )

                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = FormSecondaryText
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = jobExpanded,
                        onDismissRequest = {
                            jobExpanded = false
                        }
                    ) {

                        jobTypes.forEach { option ->

                            DropdownMenuItem(
                                text = {
                                    Text(option)
                                },
                                onClick = {
                                    jobType = option
                                    jobExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Main Area of Discomfort",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                discomfortExpanded = true
                            },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            FormBorder
                        ),
                        color = Color.White
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 13.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = mainDiscomfort,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )

                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = FormSecondaryText
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = discomfortExpanded,
                        onDismissRequest = {
                            discomfortExpanded = false
                        }
                    ) {

                        discomfortAreas.forEach { option ->

                            DropdownMenuItem(
                                text = {
                                    Text(option)
                                },
                                onClick = {
                                    mainDiscomfort = option
                                    discomfortExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                FormSectionTitle(
                    number = "2",
                    title = "Work Hours"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(text = "Assessment Date", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, FormBorder),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Choose assessment date", tint = ErgoGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date(assessmentDate)))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    TimeField(
                        modifier = Modifier.weight(1f),
                        label = "Start Time",
                        value = startTime,
                        onClick = {
                            showStartTimePicker = true
                        }
                    )

                    TimeField(
                        modifier = Modifier.weight(1f),
                        label = "End Time",
                        value = endTime,
                        onClick = {
                            showEndTimePicker = true
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                FormSectionTitle(
                    number = "3",
                    title = "Areas of Concern"
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Select all areas where you experience discomfort.",
                    fontSize = 12.sp,
                    color = FormSecondaryText
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                concernOptions.chunked(2).forEach { rowOptions ->

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        rowOptions.forEach { option ->

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {

                                        concerns =
                                            if (option in concerns) {
                                                concerns - option
                                            } else {
                                                concerns + option
                                            }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Checkbox(
                                    checked = option in concerns,
                                    onCheckedChange = { checked ->

                                        concerns =
                                            if (checked) {
                                                concerns + option
                                            } else {
                                                concerns - option
                                            }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ErgoGreen
                                    )
                                )

                                Text(
                                    text = option,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (rowOptions.size == 1) {
                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = FormSoftGreen
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Text(
                            text = "Personalised for you",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErgoGreen
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = buildPersonalisationMessage(
                                jobType = jobType,
                                mainDiscomfort = mainDiscomfort,
                                concerns = concerns
                            ),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF3D463E)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                FormSectionTitle(
                    number = "4",
                    title = "Additional Notes"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = {
                        Text(
                            text = "Anything else you would like ErgoDesk to know?",
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Button(
                    onClick = {
                        saveAssessment()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErgoGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {

                    Text(
                        text = if (
                            editingAssessmentId == null
                        ) {
                            "Save Assessment"
                        } else {
                            "Update Assessment"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (editingAssessmentId != null) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    TextButton(
                        onClick = {
                            resetForm()
                            message = "Editing cancelled."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = "Cancel Editing",
                            color = FormSecondaryText
                        )
                    }
                }

                message?.let { currentMessage ->

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = FormSoftGreen
                    ) {

                        Text(
                            text = currentMessage,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = ErgoGreen
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Saved Assessments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${savedAssessments.size} saved",
                        fontSize = 11.sp,
                        color = FormSecondaryText
                    )
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Select a previous assessment to view or edit it.",
                    fontSize = 12.sp,
                    color = FormSecondaryText
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (savedAssessments.isEmpty()) {

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            FormBorder
                        ),
                        color = Color.White
                    ) {

                        Text(
                            text = "No saved assessments yet.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 12.sp,
                            color = FormSecondaryText
                        )
                    }

                }
            }
        }
        items(savedAssessments, key = { it.id }) { assessment ->
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                SavedAssessmentCard(
                    assessment = assessment,
                    selected = editingAssessmentId == assessment.id,
                    onSelect = { loadAssessment(assessment) },
                    onEdit = { loadAssessment(assessment) },
                    onDelete = {
                        roomAssessments.firstOrNull { it.uid.toLong() == assessment.id }?.let {
                            assessmentViewModel.deleteAssessment(it) { success ->
                                if (success) {
                                    if (editingAssessmentId == assessment.id) resetForm()
                                    message = "Assessment deleted successfully."
                                } else {
                                    message = "Failed to delete assessment."
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Assessments are stored on this device.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF8A8A8A)
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = assessmentDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { assessmentDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker) {

        ErgoTimePickerDialog(
            title = "Select start time",
            state = startTimePickerState,
            onConfirm = {

                startTime = formatTime(
                    hour = startTimePickerState.hour,
                    minute = startTimePickerState.minute
                )

                showStartTimePicker = false
            },
            onDismiss = {
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {

        ErgoTimePickerDialog(
            title = "Select end time",
            state = endTimePickerState,
            onConfirm = {

                endTime = formatTime(
                    hour = endTimePickerState.hour,
                    minute = endTimePickerState.minute
                )

                showEndTimePicker = false
            },
            onDismiss = {
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun ErgoFormHeader() {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FormHeaderBackground
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
                    color = FormSecondaryText
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    FormBorder
                ),
                color = Color.White
            ) {

                Icon(
                    imageVector = Icons.Filled.PersonOutline,
                    contentDescription = "Profile",
                    tint = FormSecondaryText,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun FormSectionTitle(
    number: String,
    title: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
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

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimeField(
    modifier: Modifier,
    label: String,
    value: String,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
    ) {

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                1.dp,
                FormBorder
            ),
            color = Color.White
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = ErgoGreen,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = value,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SavedAssessmentCard(
    assessment: ErgonomicAssessment,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val date = remember(
        assessment.createdAt
    ) {
        SimpleDateFormat(
            "d MMM yyyy",
            Locale.ENGLISH
        ).format(
            Date(assessment.createdAt)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect()
            },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                ErgoGreen
            } else {
                FormBorder
            }
        ),
        color = if (selected) {
            FormSoftGreen
        } else {
            Color.White
        }
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = date,
                    fontSize = 11.sp,
                    color = FormSecondaryText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = assessment.jobType,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = buildAssessmentSummary(
                        assessment
                    ),
                    fontSize = 11.sp,
                    color = FormSecondaryText
                )
            }

            IconButton(
                onClick = {
                    onEdit()
                }
            ) {

                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit assessment",
                    tint = ErgoGreen
                )
            }

            IconButton(
                onClick = {
                    onDelete()
                }
            ) {

                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete assessment",
                    tint = FormDeleteRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErgoTimePickerDialog(
    title: String,
    state: TimePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                TimePicker(
                    state = state
                )
            }
        },
        confirmButton = {

            TextButton(
                onClick = onConfirm
            ) {
                Text("OK")
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(
    hour: Int,
    minute: Int
): String {

    val formattedHour =
        hour.toString().padStart(
            length = 2,
            padChar = '0'
        )

    val formattedMinute =
        minute.toString().padStart(
            length = 2,
            padChar = '0'
        )

    return "$formattedHour:$formattedMinute"
}

private fun buildPersonalisationMessage(
    jobType: String,
    mainDiscomfort: String,
    concerns: Set<String>
): String {

    val areas =
        buildList {

            if (
                mainDiscomfort != "None"
            ) {
                add(mainDiscomfort)
            }

            addAll(concerns)
        }
            .distinct()

    return if (areas.isEmpty()) {

        "Your $jobType profile and work hours will help ErgoDesk " +
                "prepare suitable break recommendations."

    } else {

        "Based on your $jobType profile and selected concern areas " +
                "(${areas.joinToString()}), ErgoDesk can prioritise " +
                "more relevant ergonomic break suggestions."
    }
}

private fun buildAssessmentSummary(
    assessment: ErgonomicAssessment
): String {

    val areas =
        buildList {

            if (
                assessment.mainDiscomfortArea != "None"
            ) {
                add(
                    assessment.mainDiscomfortArea
                )
            }

            addAll(
                assessment.concernAreas
            )
        }
            .distinct()

    val concernText =
        if (areas.isEmpty()) {
            "No discomfort selected"
        } else {
            areas.joinToString(
                separator = " · "
            )
        }

    return "$concernText  •  " +
            "${assessment.workStartTime}–${assessment.workEndTime}"
}