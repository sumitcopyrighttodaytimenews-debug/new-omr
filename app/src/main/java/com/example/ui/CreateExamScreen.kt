package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.OmrGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamScreen(navController: NavController, viewModel: OmrViewModel) {
    var examName by remember { mutableStateOf("") }
    
    var examDate by remember { mutableStateOf("") }
    var datePickerVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    var examTitle by remember { mutableStateOf("बिहार विद्यालय परीक्षा , समिति") }
    var logoPath by remember { mutableStateOf("") }
    var logoOpacity by remember { mutableStateOf(0.2f) }
    var logoSize by remember { mutableStateOf(100f) }
    var logoPosition by remember { mutableStateOf("Left") }
    
    var marksPerQuestion by remember { mutableStateOf("1.0") }
    var negativeMarks by remember { mutableStateOf("0.0") }
    var passMarks by remember { mutableStateOf("30.0") }
    var bonusMarks by remember { mutableStateOf("0.0") }
    var templateType by remember { mutableStateOf("Standard") }
    var templateDropdownExpanded by remember { mutableStateOf(false) }
    
    var previewVisible by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            logoPath = it.toString()
        }
    }
    
    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        examDate = sdf.format(java.util.Date(millis))
                    }
                    datePickerVisible = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    val availableSubjects = listOf("HINDI", "HISTORY", "GEOGRAPHY", "POLITICAL SCIENCE", "PSYCHOLOGY", "MUSIC", "SOCIOLOGY")
    var selectedSubject by remember { mutableStateOf("HINDI") }
    var subjectsExpanded by remember { mutableStateOf(false) }

    val students by viewModel.students.collectAsStateWithLifecycle()
    val mappedStudents = students.filter { it.subjects.contains(selectedSubject, ignoreCase = true) }

    if (previewVisible) {
        Dialog(
            onDismissRequest = { previewVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    IconButton(onClick = { previewVisible = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
                    
                    LaunchedEffect(examTitle, logoPath, logoOpacity, logoSize, logoPosition, templateType) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val bmp = OmrGenerator.generateOmrBitmap(context, 100, 4, null, examTitle, logoPath, logoOpacity, logoSize, logoPosition, templateType)
                            previewBmp = bmp
                        }
                    }
                    
                    if (previewBmp != null) {
                        Image(
                            bitmap = previewBmp!!.asImageBitmap(),
                            contentDescription = "OMR Preview",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1000f/1414f)
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Exam") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White, titleContentColor = androidx.compose.ui.graphics.Color.Black, navigationIconContentColor = androidx.compose.ui.graphics.Color.Black, actionIconContentColor = androidx.compose.ui.graphics.Color.Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = examName,
                onValueChange = { examName = it },
                label = { Text("Exam Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Box(modifier = Modifier.fillMaxWidth().clickable { datePickerVisible = true }) {
                OutlinedTextField(
                    value = examDate,
                    onValueChange = {},
                    label = { Text("Exam Date (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            ExposedDropdownMenuBox(
                expanded = subjectsExpanded,
                onExpandedChange = { subjectsExpanded = !subjectsExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSubject,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Subject") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectsExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = subjectsExpanded,
                    onDismissRequest = { subjectsExpanded = false }
                ) {
                    availableSubjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject) },
                            onClick = {
                                selectedSubject = subject
                                subjectsExpanded = false
                            }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = examTitle,
                onValueChange = { examTitle = it },
                label = { Text("OMR Title (Header Text)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { launcher.launch("image/*") }) {
                    Text(if (logoPath.isEmpty()) "Select Logo" else "Change Logo")
                }
                Spacer(modifier = Modifier.width(16.dp))
                if (logoPath.isNotEmpty()) {
                    Text("Logo Selected", color = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (logoPath.isNotEmpty()) {
                Text("Logo Opacity: ${(logoOpacity * 100).toInt()}%")
                Slider(
                    value = logoOpacity,
                    onValueChange = { logoOpacity = it },
                    valueRange = 0.05f..1f
                )
                Text("Logo Size: ${logoSize.toInt()}px")
                Slider(
                    value = logoSize,
                    onValueChange = { logoSize = it },
                    valueRange = 50f..200f
                )
                Text("Logo Position")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Left", "Center", "Right").forEach { pos ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = logoPosition == pos,
                                onClick = { logoPosition = pos }
                            )
                            Text(pos)
                        }
                    }
                }
            }

            Text("Evaluation Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = marksPerQuestion,
                    onValueChange = { marksPerQuestion = it },
                    label = { Text("Marks/Q") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = negativeMarks,
                    onValueChange = { negativeMarks = it },
                    label = { Text("Negative") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = passMarks,
                    onValueChange = { passMarks = it },
                    label = { Text("Pass Marks") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bonusMarks,
                    onValueChange = { bonusMarks = it },
                    label = { Text("Bonus Marks") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Automatic Mapping", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("${mappedStudents.size} students enrolled in $selectedSubject will be automatically assigned to this exam.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            
            OutlinedButton(
                onClick = { previewVisible = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview OMR")
            }

            ExposedDropdownMenuBox(
                expanded = templateDropdownExpanded,
                onExpandedChange = { templateDropdownExpanded = !templateDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (templateType == "Standard") "Standard (All Details + QR)" else "Simple (Roll No Only)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("OMR Template") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = templateDropdownExpanded,
                    onDismissRequest = { templateDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Standard (All Details + QR)") },
                        onClick = {
                            templateType = "Standard"
                            templateDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Simple (Roll No Only)") },
                        onClick = {
                            templateType = "RollNoOnly"
                            templateDropdownExpanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
            onClick = {
                    val mk = marksPerQuestion.toFloatOrNull() ?: 1f
                    val neg = negativeMarks.toFloatOrNull() ?: 0f
                    val ps = passMarks.toFloatOrNull() ?: 30f
                    val bns = bonusMarks.toFloatOrNull() ?: 0f
                    
                    viewModel.createExam(
                        examName, selectedSubject, examDate, examTitle, 
                        logoPath, logoOpacity, logoSize, logoPosition,
                        mk, neg, ps, bns, templateType
                    ) { examId ->
                        navController.popBackStack()
                        navController.navigate(Screen.ExamDashboard.createRoute(examId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = examName.isNotBlank() && examDate.isNotBlank()
            ) {
                Text("Create Exam")
            }
        }
    }
}
