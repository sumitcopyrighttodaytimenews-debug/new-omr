package com.example.ui
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.navigation.NavController
import com.example.data.Exam
import com.example.data.QuestionEntity
import com.example.util.OmrGenerator
import java.io.File
import java.io.FileOutputStream



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDashboardScreen(navController: NavController, viewModel: OmrViewModel, examId: Int) {
    var exam by remember { mutableStateOf<Exam?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(examId) {
        exam = viewModel.getExamById(examId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exam?.name ?: "Exam Dashboard", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var showEditDialog by remember { mutableStateOf(false) }
                    
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Exam") },
                            text = { Text("Are you sure you want to delete this exam? All questions, answer keys, and scan results will be deleted.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteExam(examId) {
                                        navController.popBackStack()
                                    }
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    if (showEditDialog && exam != null) {
                        var editName by remember { mutableStateOf(exam!!.name) }
                        var editSubject by remember { mutableStateOf(exam!!.subject) }
                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("Edit Exam") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Exam Name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editSubject,
                                        onValueChange = { editSubject = it },
                                        label = { Text("Subject") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.updateExam(exam!!.copy(name = editName, subject = editSubject)) {
                                        showEditDialog = false
                                        // Update local state to reflect change immediately
                                        exam = exam!!.copy(name = editName, subject = editSubject)
                                    }
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Exam", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Exam", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White, titleContentColor = androidx.compose.ui.graphics.Color.Black, navigationIconContentColor = androidx.compose.ui.graphics.Color.Black, actionIconContentColor = androidx.compose.ui.graphics.Color.Black)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = androidx.compose.ui.graphics.Color(0xFF1B5E20)
                        )
                    }
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Generate", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Questions", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Assignment, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Scanner", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Exam Day", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Event, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Reports", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Assessment, null, modifier = Modifier.size(20.dp)) })
            }
            when (selectedTab) {
                0 -> GenerateTab(navController, viewModel, examId, exam)
                1 -> if (exam != null) CreateQuestionPaperTabInternal(viewModel, exam!!)
                2 -> ScannerTab(navController, viewModel, examId)
                3 -> if (exam != null) ExamDayTab(navController, viewModel, examId, exam!!)
                4 -> if (exam != null) ReportsTab(viewModel, examId, exam!!)
            }
        }
    }
}

@Composable
fun GenerateTab(navController: NavController, viewModel: OmrViewModel, examId: Int, exam: Exam?) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    var isGeneratingMale by remember { mutableStateOf(false) }
    var isGeneratingFemale by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    val enrolledStudents = students.filter { exam?.subject != null && it.subjects.contains(exam.subject, ignoreCase = true) }
    val males = enrolledStudents.filter { it.gender == "Male" }
    val females = enrolledStudents.filter { it.gender == "Female" }


    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && exam != null) {
            when (pendingAction) {
                "Male" -> {
                    isGeneratingMale = true
                    generateOmrPdf(context, exam, males, "Male", uri) {
                        isGeneratingMale = false
                    }
                }
                "Female" -> {
                    isGeneratingFemale = true
                    generateOmrPdf(context, exam, females, "Female", uri) {
                        isGeneratingFemale = false
                    }
                }
                "DeskSlips" -> {
                    generateDeskSlipsPdf(context, exam, enrolledStudents, uri)
                }
                "SeatingPlan" -> {
                    generateSeatingPlanPdf(context, exam, enrolledStudents, uri)
                }
            }
        }
        pendingAction = null
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Generate OMR Sheets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Subject: ${exam?.subject}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Total Enrolled: ${enrolledStudents.size} (M: ${males.size}, F: ${females.size})", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (exam != null) {
                            pendingAction = "Male"
                            createDocumentLauncher.launch("${exam.name}_OMR_Male.pdf")
                        }
                    },
                    enabled = !isGeneratingMale && males.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isGeneratingMale) "Generating..." else "Download Male OMRs PDF")
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (exam != null) {
                            pendingAction = "Female"
                            createDocumentLauncher.launch("${exam.name}_OMR_Female.pdf")
                        }
                    },
                    enabled = !isGeneratingFemale && females.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isGeneratingFemale) "Generating..." else "Download Female OMRs PDF")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        if (exam != null) {
                            pendingAction = "DeskSlips"
                            createDocumentLauncher.launch("${exam.name}_DeskSlips.pdf")
                        }
                    },
                    enabled = enrolledStudents.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Download Desk Slips PDF")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        if (exam != null) {
                            pendingAction = "SeatingPlan"
                            createDocumentLauncher.launch("_SeatingPlan.pdf")
                        }
                    },
                    enabled = enrolledStudents.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Download Seating Plan PDF")
                }
            }
        }
    }
}

@Composable
fun CreateQuestionPaperTabInternal(viewModel: OmrViewModel, exam: Exam) {
    val context = LocalContext.current
    val dbQuestionsFlow = remember(exam.id) { viewModel.getQuestionsForExam(exam.id) }
    val dbQuestions by dbQuestionsFlow.collectAsStateWithLifecycle()
    val questions = remember { mutableStateListOf<QuestionEntity>() }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(dbQuestions) {
        if (!isInitialized) {
            questions.clear()
            questions.addAll(dbQuestions)
            isInitialized = true
        } else {
            val dbIds = dbQuestions.map { it.id }.toSet()
            val qIds = questions.map { it.id }.toSet()
            
            if (dbIds != qIds) {
                questions.removeAll { it.id !in dbIds }
                val newItems = dbQuestions.filter { it.id !in qIds }
                questions.addAll(newItems)
            }
        }
        
        if (dbQuestions.isNotEmpty()) {
            val correctAnswers = dbQuestions.map { it.correctIndex }
            viewModel.saveAnswerKey(exam.id, "A", dbQuestions.size, 4, correctAnswers) {}
        }
    }
    
    var isGenerating by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    
    // Paper Settings
    var showSettingsDialog by remember { mutableStateOf(false) }
    var headerText by remember { mutableStateOf("QUESTION PAPER") }
    var runByText by remember { mutableStateOf("") }
    var directorText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("★  ALL THE BEST  ★") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }


    val coroutineScope = rememberCoroutineScope()
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            if (pendingAction == "generatePapers") {
                isGenerating = true
                generateQuestionPapersInternal(context, exam, questions, uri, viewModel, headerText, runByText, directorText, addressText, false, null) {
                    isGenerating = false
                }
            } else if (pendingAction == "downloadAnswerKey") {
                isGenerating = true
                coroutineScope.launch {
                    if (uri != null) { generateAnswerKeyPdf(context, exam, uri!!, viewModel) }
                    isGenerating = false
                }
            }
        }
    }

    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Question Paper Settings") },
            text = {
                Column {
                    OutlinedTextField(
                        value = headerText,
                        onValueChange = { headerText = it },
                        label = { Text("Heading (e.g. QUESTION PAPER)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = runByText,
                        onValueChange = { runByText = it },
                        label = { Text("Run By") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = directorText,
                        onValueChange = { directorText = it },
                        label = { Text("Director") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        label = { Text("Bottom Address / Footer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("OK") }
            }
        )
    }

    if (showPreviewDialog && previewBitmap != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Paper Preview") },
            text = {
                androidx.compose.foundation.Image(
                    bitmap = previewBitmap!!,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().aspectRatio(previewBitmap!!.width.toFloat() / previewBitmap!!.height.toFloat())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Question Paper Settings") },
            text = {
                Column {
                    OutlinedTextField(
                        value = headerText,
                        onValueChange = { headerText = it },
                        label = { Text("Heading (e.g. QUESTION PAPER)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = runByText,
                        onValueChange = { runByText = it },
                        label = { Text("Run By") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = directorText,
                        onValueChange = { directorText = it },
                        label = { Text("Director") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        label = { Text("Bottom Address / Footer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("OK") }
            }
        )
    }

    if (showPreviewDialog && previewBitmap != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Paper Preview") },
            text = {
                androidx.compose.foundation.Image(
                    bitmap = previewBitmap!!,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().aspectRatio(previewBitmap!!.width.toFloat() / previewBitmap!!.height.toFloat())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Close") }
            }
        )
    }

    if (showJsonDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Import Questions (JSON)") },
            text = {
                Column {
                    Text("Paste JSON array format:\n[\n  {\"text\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correctIndex\": 0}\n]")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        label = { Text("JSON Data") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(type)
                        val parsed = adapter.fromJson(jsonInput)
                        if (parsed != null) {
                            val entities = parsed.map { map ->
                                val text = map["text"] as? String ?: ""
                                val options = map["options"] as? List<String> ?: listOf("", "", "", "")
                                val correctIndex = (map["correctIndex"] as? Double)?.toInt() ?: 0
                                QuestionEntity(
                                    examId = exam.id,
                                    text = text,
                                    optionA = options.getOrNull(0) ?: "",
                                    optionB = options.getOrNull(1) ?: "",
                                    optionC = options.getOrNull(2) ?: "",
                                    optionD = options.getOrNull(3) ?: "",
                                    correctIndex = correctIndex
                                )
                            }
                            viewModel.saveQuestions(entities) {
                                showJsonDialog = false
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showJsonDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Questions (${questions.size}/100)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pendingAction = "downloadAnswerKey"; createDocumentLauncher.launch("${exam.subject}_AnswerKey.pdf") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                Text(if (isGenerating && pendingAction == "downloadAnswerKey") "Key" else "Ans Key", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = { showJsonDialog = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                Text("Import JSON", style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                if (questions.size < 100) {
                    viewModel.saveQuestion(QuestionEntity(examId = exam.id, text = "", optionA = "", optionB = "", optionC = "", optionD = "", correctIndex = 0)) {}
                }
            }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                Text("Add Question", style = MaterialTheme.typography.labelSmall)
            }
        }

        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f)) {
            items(
                count = questions.size,
                key = { index -> questions[index].id }
            ) { index ->
                val q = questions[index]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Question ${index + 1}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteQuestion(q) {} }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        OutlinedTextField(
                            value = q.text,
                            onValueChange = { 
                                questions[index] = q.copy(text = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Question Text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = q.optionA,
                            onValueChange = { 
                                questions[index] = q.copy(optionA = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option A") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = q.optionB,
                            onValueChange = { 
                                questions[index] = q.copy(optionB = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option B") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = q.optionC,
                            onValueChange = { 
                                questions[index] = q.copy(optionC = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option C") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = q.optionD,
                            onValueChange = { 
                                questions[index] = q.copy(optionD = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option D") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Correct Answer:", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val labels = listOf("A", "B", "C", "D")
                            for (i in 0..3) {
                                FilterChip(
                                    selected = q.correctIndex == i,
                                    onClick = { 
                                        questions[index] = q.copy(correctIndex = i)
                                        viewModel.updateQuestion(questions[index]) {} 
                                    },
                                    label = { Text(labels[i]) }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    generateQuestionPapersInternal(context, exam, questions, null, viewModel, headerText, runByText, directorText, addressText, true, pdfDoc) {
                        Thread {
                            try {
                                val tempFile = java.io.File(context.cacheDir, "preview.pdf")
                                pdfDoc.writeTo(java.io.FileOutputStream(tempFile))
                                pdfDoc.close()
                                
                                val fd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                val renderer = android.graphics.pdf.PdfRenderer(fd)
                                val page = renderer.openPage(0)
                                val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                renderer.close()
                                fd.close()
                                
                                val imageBmp = bitmap.asImageBitmap()
                                (context as? android.app.Activity)?.runOnUiThread {
                                    previewBitmap = imageBmp
                                    showPreviewDialog = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = !isGenerating && questions.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Preview")
            }
            
            Button(
                onClick = {
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    pendingAction = "generatePapers"
                    createDocumentLauncher.launch("${exam.subject}_QuestionPapers.pdf")
                },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = !isGenerating && questions.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Download")
            }
        }
    }
}

private fun generateQuestionPapersInternal(
    context: Context,
    exam: Exam,
    baseQuestions: List<QuestionEntity>,
    uri: android.net.Uri?,
    viewModel: OmrViewModel,
    headerText: String,
    runByText: String,
    directorText: String,
    addressText: String,
    isPreview: Boolean,
    providedPdfDoc: android.graphics.pdf.PdfDocument?,
    onDone: () -> Unit
) {
    Thread {
        val sets = if (isPreview) listOf("A") else listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        val pdfDocument = providedPdfDoc ?: PdfDocument()

        val pageW = 793f
        val pageH = 1122f

        val thinStroke = Paint().apply { strokeWidth = 1f; style = Paint.Style.STROKE; color = android.graphics.Color.BLACK }
        val thickStroke = Paint().apply { strokeWidth = 2f; style = Paint.Style.STROKE; color = android.graphics.Color.BLACK }
        val blackFill = Paint().apply { color = android.graphics.Color.BLACK; style = Paint.Style.FILL }

        val boldSmallPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val whiteLargePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 70f
            textAlign = Paint.Align.CENTER
        }
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 34f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }
        val whiteNormalPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        val instrBoldPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = android.graphics.Color.BLACK
        }
        val instrRegularPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 13f
            color = android.graphics.Color.BLACK
        }
        val qTextPaint = Paint().apply { 
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val qDotsPaint = Paint().apply { 
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) 
            color = android.graphics.Color.BLACK
        }
        val optPaint = Paint().apply { 
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) 
            color = android.graphics.Color.BLACK
        }
        val optLetterPaint = Paint().apply {
            textSize = 10f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.BLACK
        }
        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }
        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 700f
            textAlign = Paint.Align.CENTER
        }
        val verticalWatermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B0B0B0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        for (setName in sets) {
            val shuffledQuestions = baseQuestions.shuffled()
            val correctAnswers = shuffledQuestions.map { it.correctIndex }
            viewModel.saveAnswerKey(exam.id, setName, shuffledQuestions.size, 4, correctAnswers) {}

            var questionsDrawn = 0
            val totalQuestions = shuffledQuestions.size
            var pageIndex = 1

            while (questionsDrawn < totalQuestions) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), pageIndex).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Border
                canvas.drawRect(20f, 20f, pageW - 20f, pageH - 20f, thinStroke)

                // Watermark
                canvas.drawText(setName, pageW / 2f, pageH / 2f + 250f, watermarkPaint)

                // Vertical Watermarks
                canvas.save()
                canvas.translate(35f, pageH / 2f)
                canvas.rotate(-90f)
                canvas.drawText("MADE BY SUMIT SHARMA", 0f, 0f, verticalWatermarkPaint)
                canvas.restore()
                
                canvas.save()
                canvas.translate(pageW - 35f, pageH / 2f)
                canvas.rotate(90f)
                canvas.drawText("MADE BY SUMIT SHARMA", 0f, 0f, verticalWatermarkPaint)
                canvas.restore()

                // Footer
                canvas.drawLine(40f, pageH - 80f, pageW - 40f, pageH - 80f, thinStroke)
                canvas.drawText(addressText.ifBlank { "★  ALL THE BEST  ★" }, pageW / 2f, pageH - 45f, footerPaint)

                val questionsOnThisPage = if (pageIndex == 1) 24 else 26
                val qPerCol = questionsOnThisPage / 2
                
                var startY = 0f
                var rowSpacing = 0f

                if (pageIndex == 1) {
                    // Top Left
                    canvas.drawText("SET CODE", 40f, 45f, boldSmallPaint)
                    canvas.drawRect(40f, 55f, 120f, 135f, blackFill)
                    canvas.drawText(setName, 80f, 120f, whiteLargePaint)

                    // Top Center
                    canvas.drawText(headerText.ifBlank { "QUESTION PAPER" }, pageW / 2f, 65f, titlePaint)
                    canvas.drawLine(pageW / 2f - 180f, 85f, pageW / 2f - 20f, 85f, thickStroke)
                    canvas.drawLine(pageW / 2f + 20f, 85f, pageW / 2f + 180f, 85f, thickStroke)
                    

                    val diamondPath = android.graphics.Path().apply {
                        moveTo(pageW / 2f, 80f)
                        lineTo(pageW / 2f + 8f, 85f)
                        lineTo(pageW / 2f, 90f)
                        lineTo(pageW / 2f - 8f, 85f)
                        close()
                    }
                    canvas.drawPath(diamondPath, blackFill)

                    val mcqRect = android.graphics.RectF(pageW / 2f - 170f, 105f, pageW / 2f + 170f, 135f)
                    canvas.drawRoundRect(mcqRect, 15f, 15f, blackFill)
                    canvas.drawText("MULTIPLE CHOICE QUESTIONS (MCQ)", pageW / 2f, 127f, whiteNormalPaint)

                    // Top Right
                    val detailsRect = android.graphics.RectF(pageW - 220f, 35f, pageW - 40f, 115f)
                    canvas.drawRoundRect(detailsRect, 5f, 5f, thinStroke)
                    
                    val labelX = pageW - 210f
                    val colonX = pageW - 105f
                    val valueX = pageW - 95f
                    
                    canvas.drawText("Total Questions", labelX, 60f, boldSmallPaint)
                    canvas.drawText(":", colonX, 60f, boldSmallPaint)
                    canvas.drawText("${baseQuestions.size}", valueX, 60f, boldSmallPaint)
                    
                    canvas.drawText("Total Marks", labelX, 80f, boldSmallPaint)
                    canvas.drawText(":", colonX, 80f, boldSmallPaint)
                    canvas.drawText("${baseQuestions.size}", valueX, 80f, boldSmallPaint)
                    
                    canvas.drawText("Time Allowed", labelX, 100f, boldSmallPaint)
                    canvas.drawText(":", colonX, 100f, boldSmallPaint)
                    canvas.drawText("60 Minutes", valueX, 100f, boldSmallPaint)

                    if (runByText.isNotBlank()) {
                        canvas.drawText("Run By: $runByText", 40f, 150f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.LEFT })
                    }
                    if (directorText.isNotBlank()) {
                        canvas.drawText("Director: $directorText", pageW - 40f, 150f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
                    }
                    boldSmallPaint.textAlign = android.graphics.Paint.Align.LEFT

                    // Instructions Box
                    val instrRect = android.graphics.RectF(40f, 160f, pageW - 40f, 220f)
                    canvas.drawRoundRect(instrRect, 5f, 5f, thinStroke)
                    canvas.drawText("निर्देश : ", 60f, 185f, instrBoldPaint)
                    canvas.drawText("प्रत्येक प्रश्न 1 अंक का है। सही विकल्प (A / B / C / D) चुनें और", 185f, 185f, instrRegularPaint)
                    canvas.drawText("अपना उत्तर OMR शीट पर अंकित करें।", 185f, 205f, instrRegularPaint)

                    // Center Divider
                    canvas.drawLine(pageW / 2f, 230f, pageW / 2f, pageH - 80f, thinStroke)
                    
                    startY = 245f
                    rowSpacing = 65f
                } else {
                    // Small header for subsequent pages
                    canvas.drawText("SET CODE : $setName", pageW - 150f, 45f, boldSmallPaint)
                    // Center Divider
                    canvas.drawLine(pageW / 2f, 60f, pageW / 2f, pageH - 80f, thinStroke)
                    
                    startY = 70f
                    rowSpacing = 72f
                }

                // Questions
                val endQIndex = Math.min(questionsDrawn + questionsOnThisPage, totalQuestions)

                for (i in questionsDrawn until endQIndex) {
                    val qIndexOnPage = i - questionsDrawn
                    val isCol2 = qIndexOnPage >= qPerCol
                    val colX = if (isCol2) pageW / 2f + 20f else 40f
                    val rowIndex = qIndexOnPage % qPerCol
                    val qY = startY + (rowIndex * rowSpacing)
                    
                    val q = shuffledQuestions[i]
                    val qNum = "${i + 1}."
                    canvas.drawText(qNum, colX, qY, qTextPaint)
                    
                    var text = q.text
                    if (text.isBlank()) text = "......................................................................... ?"
                    
                    val maxW = 320f
                    var line1 = text
                    var line2 = ""
                    
                    val breakIndex = qDotsPaint.breakText(text, true, maxW, null)
                    if (breakIndex < text.length) {
                        val spaceIdx = text.lastIndexOf(' ', breakIndex)
                        val splitIdx = if (spaceIdx > 0) spaceIdx else breakIndex
                        line1 = text.substring(0, splitIdx)
                        line2 = text.substring(splitIdx).trim()
                        
                        val b2 = qDotsPaint.breakText(line2, true, maxW, null)
                        if (b2 < line2.length) {
                            line2 = line2.substring(0, b2) + "..."
                        }
                    }
                    
                    val isTwoLines = line2.isNotEmpty()
                    val optOffset = if (isTwoLines) 14f else 0f
                    
                    canvas.drawText(line1, colX + 25f, qY, qDotsPaint)
                    if (isTwoLines) {
                        canvas.drawText(line2, colX + 25f, qY + 14f, qDotsPaint)
                    }
                    
                    val optY1 = qY + 17f + optOffset
                    val optY2 = qY + 34f + optOffset
                    val colSpacing = 160f
                    val labels = listOf("(a)", "(b)", "(c)", "(d)")
                    
                    for (optIndex in 0..3) {
                        val isOptCol2 = optIndex % 2 != 0
                        val isOptRow2 = optIndex >= 2
                        
                        val optX = colX + 25f + if (isOptCol2) colSpacing else 0f
                        val currentOptY = if (isOptRow2) optY2 else optY1
                        
                        canvas.drawText(labels[optIndex], optX, currentOptY, optPaint)
                        
                        val optStrText = when (optIndex) {
                            0 -> q.optionA.ifBlank { "Option" }
                            1 -> q.optionB.ifBlank { "Option" }
                            2 -> q.optionC.ifBlank { "Option" }
                            3 -> q.optionD.ifBlank { "Option" }
                            else -> "Option"
                        }
                        var displayStr = optStrText
                        if (displayStr.length > 18) displayStr = displayStr.substring(0, 16) + ".."
                        
                        canvas.drawText(displayStr, optX + 20f, currentOptY, optPaint)
                    }
                }
                pdfDocument.finishPage(page)
                pageIndex++
                questionsDrawn = endQIndex
            }
        }

        if (!isPreview && uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Saved 10 Sets successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pdfDocument.close()
                (context as? android.app.Activity)?.runOnUiThread {
                    onDone()
                }
            }
        } else {
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }
    }.start()
}


@Composable
fun ScannerTab(navController: NavController, viewModel: OmrViewModel, examId: Int) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("OMR Scanner", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.ScanOmr.createRoute(examId)) }) {
            Text("Launch Scanner")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Scan Results", style = MaterialTheme.typography.titleMedium)
        
        val resultsFlow = remember(examId) { viewModel.getScanResultsForExam(examId) }
        val results by resultsFlow.collectAsStateWithLifecycle()
        val allStudents by viewModel.students.collectAsStateWithLifecycle()
        
        if (results.isEmpty()) {
            Text("No scans yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            androidx.compose.foundation.lazy.LazyColumn {
                items(results.size) { i ->
                    val result = results[i]
                    val studentName = allStudents.find { it.rollNo == result.studentId }?.name ?: "Unknown"
                    var expanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = { expanded = !expanded })
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(studentName, style = MaterialTheme.typography.titleMedium)
                            Text("Roll: ${result.studentId} | Set: ${result.paperSet}")
                            Text("Score: ${result.score} / ${result.totalQuestions}", color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
                            
                            if (expanded && result.questionStatuses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Question Analysis", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                val statusesList = com.example.data.Converters().toList(result.questionStatuses)
                                
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val chunkedStatuses = statusesList.chunked(10)
                                    chunkedStatuses.forEachIndexed { rowIndex, rowStatuses ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                            rowStatuses.forEachIndexed { colIndex, status ->
                                                val qIndex = rowIndex * 10 + colIndex
                                                val color = when (status) {
                                                    1 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                                    0 -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = 4.dp)
                                                        .size(24.dp)
                                                        .background(color, shape = RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${qIndex + 1}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateOmrPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, gender: String, uri: android.net.Uri, onDone: () -> Unit) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            for (student in students) {
                // High quality OMR sheet by creating the page with exact OMR dimensions (1000x1414)
                val pageInfo = PdfDocument.PageInfo.Builder(com.example.util.OmrGenerator.SHEET_WIDTH, com.example.util.OmrGenerator.SHEET_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                // Draw OMR vector directly onto the PDF canvas for infinite scalability
                com.example.util.OmrGenerator.drawOmrToCanvas(
                    context = context, 
                    canvas = canvas, 
                    numQuestions = 100, 
                    numOptions = 4, 
                    student = student, 
                    title = exam.title, 
                    logoPath = exam.logoUrl, 
                    logoOpacity = exam.logoOpacity, 
                    logoSize = exam.logoSize, 
                    logoPosition = exam.logoPosition
                )
                
                pdfDocument.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Saved $gender OMRs successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument?.close()
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }
    }.start()
}

private fun generateDeskSlipsPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, uri: android.net.Uri) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val males = students.filter { it.gender.equals("Male", ignoreCase = true) }
            val females = students.filter { it.gender.equals("Female", ignoreCase = true) }
            
            val orderedStudents = males + females // Print males first, then females
            
            val pageWidth = 595
            val pageHeight = 842
            
            val marginX = 20f
            val marginY = 30f
            val cols = 5
            val rows = 18
            val cellWidth = (pageWidth - 2 * marginX) / cols
            val cellHeight = (pageHeight - 2 * marginY) / rows
            val cellsPerPage = cols * rows
            
            var currentPageInfo: PdfDocument.PageInfo? = null
            var currentPage: PdfDocument.Page? = null
            var canvas: android.graphics.Canvas? = null
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
            }
            
            // To match the image exactly, we will print a full grid on each page.
            // We will fill the grid with roll numbers from the list.
            val totalPages = Math.max(1, Math.ceil(orderedStudents.size.toDouble() / cellsPerPage).toInt())
            
            for (page in 0 until totalPages) {
                currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, page + 1).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                
                // Draw Grid
                for (r in 0..rows) {
                    val y = marginY + r * cellHeight
                    canvas.drawLine(marginX, y, pageWidth - marginX, y, borderPaint)
                }
                for (c in 0..cols) {
                    val x = marginX + c * cellWidth
                    canvas.drawLine(x, marginY, x, pageHeight - marginY, borderPaint)
                }
                
                // In the image, the top-left cell says "ROLL NO"
                // But if we put data, we should probably fill all cells.
                // We'll add "ROLL NO" to the top-left cell ONLY if it's the first page and maybe it's just a label?
                // Actually, the user wants the desk slips to look like this table. 
                // Let's just fill the table with roll numbers. If the first cell is meant to be a header, 
                // we can reserve the first cell for "ROLL NO" and start data from the second cell.
                // Let's do that for the very first page just in case.
                
                var cellIndex = 0
                if (page == 0) {
                    // Draw "ROLL NO" in the first cell
                    val x = marginX + (0.5f * cellWidth)
                    val y = marginY + (0.5f * cellHeight) + 5f // adjust for text baseline
                    canvas.drawText("ROLL NO", x, y, textPaint)
                    cellIndex = 1
                }
                
                val startIndex = page * cellsPerPage - if (page > 0) 1 else 0
                
                for (i in cellIndex until cellsPerPage) {
                    val studentIndex = startIndex + i - if (page == 0) 1 else 0
                    if (studentIndex < orderedStudents.size && studentIndex >= 0) {
                        val student = orderedStudents[studentIndex]
                        
                        val col = i % cols
                        val row = i / cols
                        
                        val x = marginX + col * cellWidth + (cellWidth / 2f)
                        val y = marginY + row * cellHeight + (cellHeight / 2f) + 5f
                        
                        canvas.drawText(student.rollNo, x, y, textPaint)
                    }
                }
                
                pdfDocument.finishPage(currentPage)
            }

            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            (context as? android.app.Activity)?.runOnUiThread {
                android.widget.Toast.makeText(context, "Saved Desk Slips successfully", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                android.widget.Toast.makeText(context, "Error saving Desk Slips", android.widget.Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument?.close()
        }
    }.start()
}

@Composable
fun ReportsTab(viewModel: OmrViewModel, examId: Int, exam: Exam) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingReport by remember { mutableStateOf<String?>(null) }
    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingReport == "CSV Exporter") {
            val r = viewModel.getScanResultsForExam(examId).value
            val s = viewModel.students.value
            coroutineScope.launch {
                com.example.util.CsvExporter.exportResults(context, uri, exam, r, s)
            }
        }
        pendingReport = null
    }
    val results by viewModel.getScanResultsForExam(examId).collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val allStudents = students.filter { it.subjects.contains(exam.subject, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Evaluation & Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Scanned", style = MaterialTheme.typography.labelMedium)
                    Text("${results.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val passCount = results.count { it.score >= exam.passMarks }
                    Text("Passed", style = MaterialTheme.typography.labelMedium)
                    Text("$passCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        val reports = listOf(
            "Rank List" to "Top scoring students",
            "Merit List" to "Students grouped by grades",
            "Pass / Fail Report" to "Status based on Pass Marks",
            "Topper Report" to "Top 10 performing students",
            "Subject Analysis" to "Average marks for the subject",
            "Question-wise Analysis" to "Difficulty and accuracy per question",
            "Attendance Report" to "Scanned vs Total Enrolled",
            "CSV Exporter" to "Export all data as CSV format"
        )
        
        Text("Available Reports", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        reports.forEach { (title, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { 
                        if (title == "CSV Exporter") {
                            pendingReport = title
                            csvLauncher.launch("results.csv")
                        }
                    }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Generate", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ExamDayTab(navController: NavController, viewModel: OmrViewModel, examId: Int, exam: Exam) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val enrolledStudents = students.filter { it.subjects.contains(exam.subject, ignoreCase = true) }
    val attendanceMap by viewModel.attendanceMap.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<String?>(null) }
    
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            when (pendingAction) {
                "SeatingPlan" -> generateSeatingPlanPdf(context, exam, enrolledStudents, uri)
                "AttendanceReport" -> generateAttendanceReportPdf(context, exam, enrolledStudents, attendanceMap, uri)
            }
        }
        pendingAction = null
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Exam Day Operations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("1. Attendance (उपस्थिति)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Mark attendance for students. Check the box if present.", style = MaterialTheme.typography.bodySmall)
        
        Card(modifier = Modifier.fillMaxWidth().height(250.dp).padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(4.dp)) {
                items(enrolledStudents.size) { i ->
                    val student = enrolledStudents[i]
                    val isPresent = attendanceMap[student.rollNo] ?: false
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        val newMap = attendanceMap.toMutableMap()
                        newMap[student.rollNo] = !isPresent
                        viewModel.attendanceMap.value = newMap
                    }.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = isPresent,
                            onCheckedChange = { checked ->
                                val newMap = attendanceMap.toMutableMap()
                                newMap[student.rollNo] = checked
                                viewModel.attendanceMap.value = newMap
                            },
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Roll No: ${student.rollNo}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
        
        OutlinedButton(
            onClick = {
                pendingAction = "AttendanceReport"
                createDocumentLauncher.launch("${exam.name}_Attendance.pdf")
            },
            enabled = enrolledStudents.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Download Attendance PDF", style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text("2. Seating Arrangement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        
        OutlinedButton(
            onClick = {
                pendingAction = "SeatingPlan"
                createDocumentLauncher.launch("${exam.name}_SeatingPlan.pdf")
            },
            enabled = enrolledStudents.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Generate Seating Plan PDF", style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text("3. OMR Errors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("List of scanned OMRs with invalid roll numbers, multiple selected options, or missing sets.", style = MaterialTheme.typography.bodySmall)
        
        val results by viewModel.getScanResultsForExam(examId).collectAsStateWithLifecycle()
        // Simple mock of pending/error checking:
        val errorResults = results.filter { it.paperSet.isEmpty() || it.studentAnswers.contains("[-1,-1]") || it.studentId.isEmpty() }
        
        if (errorResults.isEmpty()) {
            Text("No pending or error OMRs found.", color = androidx.compose.ui.graphics.Color(0xFF1B5E20), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
        } else {
            errorResults.forEach { res ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Unknown Roll / Set: ${res.paperSet.ifEmpty { "MISSING" }}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("Score: ${res.score}", style = MaterialTheme.typography.labelSmall)
                        Button(onClick = { /* Handle manual resolve */ }, modifier = Modifier.align(Alignment.End).height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Resolve Manually", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun generateSeatingPlanPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, uri: android.net.Uri) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val males = students.filter { it.gender.equals("Male", ignoreCase = true) }.sortedBy { it.rollNo }
            val females = students.filter { it.gender.equals("Female", ignoreCase = true) }.sortedBy { it.rollNo }
            val orderedStudents = males + females
            
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            
            val studentsPerRoom = 40
            val rooms = orderedStudents.chunked(studentsPerRoom)
            
            for ((roomIndex, roomStudents) in rooms.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, roomIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                canvas.drawText("Seating Arrangement Plan - ${exam.title}", pageWidth / 2f, 50f, titlePaint)
                canvas.drawText("Subject: ${exam.subject}   |   Room: ${roomIndex + 1}", pageWidth / 2f, 80f, textPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
                
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                var y = 120f
                canvas.drawText("Roll Number", margin, y, titlePaint.apply { textSize = 12f; textAlign = android.graphics.Paint.Align.LEFT })
                canvas.drawText("Name", margin + 120f, y, titlePaint)
                canvas.drawText("Bench No", pageWidth - margin - 80f, y, titlePaint)
                
                y += 20f
                canvas.drawLine(margin, y, pageWidth - margin, y, textPaint)
                y += 20f
                
                for ((index, student) in roomStudents.withIndex()) {
                    canvas.drawText(student.rollNo, margin, y, textPaint)
                    canvas.drawText(student.name, margin + 120f, y, textPaint)
                    canvas.drawText("Bench ${(index / 2) + 1}${if(index%2==0) "A" else "B"}", pageWidth - margin - 80f, y, textPaint)
                    y += 20f
                }
                
                pdfDocument.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument?.close()
        }
    }.start()
}

private fun generateAttendanceReportPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, attendanceMap: Map<String, Boolean>, uri: android.net.Uri) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val orderedStudents = students.sortedBy { it.rollNo }
            
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 16f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            
            val studentsPerPage = 35
            val chunks = orderedStudents.chunked(studentsPerPage)
            
            for ((pageIndex, chunk) in chunks.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                canvas.drawText("Attendance & Absentee Report - ${exam.title}", pageWidth / 2f, 50f, titlePaint)
                canvas.drawText("Subject: ${exam.subject}   |   Date: ${exam.date}", pageWidth / 2f, 75f, textPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
                
                val presentCount = students.count { attendanceMap[it.rollNo] == true }
                val absentCount = students.size - presentCount
                canvas.drawText("Total: ${students.size} | Present: $presentCount | Absent: $absentCount", pageWidth / 2f, 95f, textPaint)
                
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                var y = 130f
                canvas.drawText("Roll Number", margin, y, titlePaint.apply { textSize = 12f; textAlign = android.graphics.Paint.Align.LEFT })
                canvas.drawText("Name", margin + 120f, y, titlePaint)
                canvas.drawText("Status", pageWidth - margin - 100f, y, titlePaint)
                canvas.drawText("Sign", pageWidth - margin - 40f, y, titlePaint)
                
                y += 15f
                canvas.drawLine(margin, y, pageWidth - margin, y, textPaint)
                y += 20f
                
                for (student in chunk) {
                    val isPresent = attendanceMap[student.rollNo] == true
                    val statusText = if (isPresent) "Present" else "Absent"
                    
                    if (!isPresent) textPaint.color = android.graphics.Color.RED else textPaint.color = android.graphics.Color.BLACK
                    
                    canvas.drawText(student.rollNo, margin, y, textPaint)
                    canvas.drawText(student.name, margin + 120f, y, textPaint)
                    canvas.drawText(statusText, pageWidth - margin - 100f, y, textPaint)
                    canvas.drawText(if(isPresent) "_____" else "N/A", pageWidth - margin - 40f, y, textPaint)
                    y += 20f
                }
                
                pdfDocument.finishPage(page)
            }
            
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument?.close()
        }
    }.start()
}
