package com.example.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Exam
import com.example.util.OmrScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOmrScreen(navController: NavController, viewModel: OmrViewModel, examId: Int) {
    var exam by remember { mutableStateOf<Exam?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<OmrScanner.ScanResult?>(null) }
    val students by viewModel.students.collectAsStateWithLifecycle()

    LaunchedEffect(examId) {
        exam = viewModel.getExamById(examId)
    }

    val numQuestions = 100
    val numOptions = 4

    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(RESULT_FORMAT_JPEG)
        .setScannerMode(SCANNER_MODE_FULL)
        .setPageLimit(1)
        .build()

    val scanner = GmsDocumentScanning.getClient(options)

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uri = gmsResult?.pages?.firstOrNull()?.imageUri
            if (uri != null) {
                isProcessing = true
                coroutineScope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        
                        val scanRes = withContext(Dispatchers.Default) {
                            val scanResultObj = OmrScanner.scan(softwareBitmap, numQuestions, numOptions)
                            // We return it for now. Evaluation will happen in ResultView or later if we refactor.
                            scanResultObj
                        }
                        if (scanRes != null) {
                            if (students.any { it.rollNo == scanRes.studentId }) {
                                scanResult = scanRes
                            } else {
                                Toast.makeText(context, "Student Roll No. ${scanRes.studentId} not found.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to scan OMR.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessing = false
                    }
                }
            }
        }
    }

    val startScan: () -> Unit = {
        scanner.getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to start scanner", Toast.LENGTH_SHORT).show()
            }
    }

    // Auto-start scanner when opening screen
    LaunchedEffect(Unit) {
        startScan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan OMR") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing OMR...")
                }
            } else if (scanResult != null && exam != null) {
                ResultView(scanResult!!, exam!!, viewModel, onDismiss = {
                    navController.popBackStack()
                }, onRescan = {
                    scanResult = null
                    startScan()
                })
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scan a printed OMR sheet", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = startScan) {
                        Text("Start Scanner")
                    }
                }
            }
        }
    }
}

@Composable
fun ResultView(result: OmrScanner.ScanResult, exam: Exam, viewModel: OmrViewModel, onDismiss: () -> Unit, onRescan: () -> Unit) {
    var editedStudentId by remember { mutableStateOf(result.studentId) }
    var editedPaperSet by remember { mutableStateOf(result.paperSet) }

    var key by remember { mutableStateOf<com.example.data.AnswerKey?>(null) }
    var keyLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(editedPaperSet) {
        keyLoaded = false
        key = viewModel.getAnswerKeyForExamAndSet(exam.id, editedPaperSet)
        keyLoaded = true
    }

    if (!keyLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val students by viewModel.students.collectAsStateWithLifecycle()
    val studentName = students.find { it.rollNo == editedStudentId }?.name ?: "Unknown"
    val isRollMatch = students.any { it.rollNo == editedStudentId }
    
    var showEditRollNo by remember { mutableStateOf(false) }
    var showEditSet by remember { mutableStateOf(false) }

    if (showEditRollNo) {
        var tempRoll by remember { mutableStateOf(editedStudentId) }
        AlertDialog(
            onDismissRequest = { showEditRollNo = false },
            title = { Text("Edit Roll No") },
            text = {
                OutlinedTextField(
                    value = tempRoll,
                    onValueChange = { tempRoll = it },
                    label = { Text("Roll No") }
                )
            },
            confirmButton = {
                TextButton(onClick = { editedStudentId = tempRoll; showEditRollNo = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditRollNo = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditSet) {
        var tempSet by remember { mutableStateOf(editedPaperSet) }
        AlertDialog(
            onDismissRequest = { showEditSet = false },
            title = { Text("Edit Paper Set") },
            text = {
                OutlinedTextField(
                    value = tempSet,
                    onValueChange = { tempSet = it.uppercase() },
                    label = { Text("Set (A, B, C...)") }
                )
            },
            confirmButton = {
                TextButton(onClick = { editedPaperSet = tempSet; showEditSet = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditSet = false }) { Text("Cancel") }
            }
        )
    }

    if (key == null) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (editedPaperSet == "?") {
                Text("Could not detect Paper Set.", color = MaterialTheme.colorScheme.error)
            } else {
                Text("Answer Key for Set $editedPaperSet not found.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { showEditSet = true }) { Text("Enter Set Manually") }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRescan) { Text("Rescan") }
        }
        return
    }

    val correctAnswersList = viewModel.converters.toList(key!!.correctAnswers)
    
    var editedAnswers by remember { mutableStateOf(result.answers.toList()) }
    var showEditDialogForQ by remember { mutableStateOf<Int?>(null) }
    
    var correct = 0
    var wrong = 0
    var empty = 0
    val statuses = mutableListOf<Int>() // 1 = correct, 0 = wrong, -1 = empty

    for (i in 0 until key!!.numQuestions) {
        val studentAns = if (i < editedAnswers.size) editedAnswers[i] else -1
        val correctAns = if (i < correctAnswersList.size) correctAnswersList[i] else -1

        if (studentAns == -1) {
            empty++
            statuses.add(-1)
        } else if (studentAns == correctAns) {
            correct++
            statuses.add(1)
        } else {
            wrong++
            statuses.add(0)
        }
    }
    
    val attempted = key!!.numQuestions - empty
    val rawScore = correct * (exam?.marksPerQuestion ?: 1f) - (attempted - correct) * (exam?.negativeMarks ?: 0f) + (exam?.bonusMarks ?: 0f)
    val score = Math.max(0f, rawScore)

    var autoSaved by remember { mutableStateOf(false) }

    LaunchedEffect(result, key, editedStudentId, editedPaperSet, editedAnswers) {
        if (!autoSaved && key != null && isRollMatch && editedPaperSet != "?") {
            viewModel.saveScanResult(exam.id, editedStudentId, editedPaperSet, score, key!!.numQuestions, editedAnswers, statuses) {
                autoSaved = true
            }
        }
    }
    
    if (showEditDialogForQ != null) {
        val qIndex = showEditDialogForQ!!
        AlertDialog(
            onDismissRequest = { showEditDialogForQ = null },
            title = { Text("Edit Answer for Q${qIndex + 1}") },
            text = {
                Column {
                    Text("Select the correct answer bubbled by student:")
                    Spacer(modifier = Modifier.height(8.dp))
                    val options = listOf("A", "B", "C", "D", "Empty")
                    val currentAns = if (qIndex < editedAnswers.size) editedAnswers[qIndex] else -1
                    options.forEachIndexed { optIndex, optText ->
                        val value = if (optText == "Empty") -1 else optIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val newAnswers = editedAnswers.toMutableList()
                                if (qIndex >= newAnswers.size) {
                                    while (newAnswers.size <= qIndex) newAnswers.add(-1)
                                }
                                newAnswers[qIndex] = value
                                editedAnswers = newAnswers
                                autoSaved = false // allow saving again
                                showEditDialogForQ = null
                            }.padding(8.dp)
                        ) {
                            RadioButton(
                                selected = currentAns == value,
                                onClick = {
                                    val newAnswers = editedAnswers.toMutableList()
                                    if (qIndex >= newAnswers.size) {
                                        while (newAnswers.size <= qIndex) newAnswers.add(-1)
                                    }
                                    newAnswers[qIndex] = value
                                    editedAnswers = newAnswers
                                    autoSaved = false // allow saving again
                                    showEditDialogForQ = null
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(optText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditDialogForQ = null }) { Text("Cancel") }
            }
        )
    }

    val context = LocalContext.current
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                Thread {
                    try {
                        val pdfDocument = android.graphics.pdf.PdfDocument()
                        val bitmap = result.annotatedBitmap
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)
                        
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            pdfDocument.writeTo(out)
                        }
                        pdfDocument.close()
                        (context as? android.app.Activity)?.runOnUiThread {
                            android.widget.Toast.makeText(context, "PDF saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        (context as? android.app.Activity)?.runOnUiThread {
                            android.widget.Toast.makeText(context, "Failed to save PDF", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Scan Successful!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Image(
            bitmap = result.annotatedBitmap.asImageBitmap(),
            contentDescription = "Annotated OMR Scan",
            modifier = Modifier.height(300.dp).fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Student: $studentName", style = MaterialTheme.typography.titleLarge)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showEditRollNo = true }.padding(4.dp)) {
            Text("Roll No: $editedStudentId", style = MaterialTheme.typography.titleMedium, color = if (!isRollMatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit Roll No", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showEditSet = true }.padding(4.dp)) {
            Text("Set: $editedPaperSet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit Set", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Score: $score / ${key!!.numQuestions}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Total: ${key!!.numQuestions}", style = MaterialTheme.typography.bodyLarge)
                        Text("Attempted: $attempted", style = MaterialTheme.typography.bodyLarge)
                        Text("Empty: $empty", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Correct: $correct", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4CAF50))
                        Text("Wrong: $wrong", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Question Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Tap a question circle to manually edit the answer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)) }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show grid of questions
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val chunkedQuestions = (0 until key!!.numQuestions).chunked(8)
            for (rowQuestions in chunkedQuestions) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (i in rowQuestions) {
                        val status = statuses.getOrNull(i) ?: -1
                        val color = when (status) {
                            1 -> Color(0xFF4CAF50)
                            0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(color, shape = CircleShape)
                                .clickable { showEditDialogForQ = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { savePdfLauncher.launch("Scanned_OMR_${editedStudentId}.pdf") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download PDF")
            }
            OutlinedButton(
                onClick = onRescan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Next")
            }
            Button(
                onClick = {
                    if (!autoSaved) {
                        viewModel.saveScanResult(exam.id, editedStudentId, editedPaperSet, score, key!!.numQuestions, editedAnswers, statuses) {
                            onDismiss()
                        }
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (autoSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (autoSaved) "Auto Saved (Done)" else "Save Result")
            }
        }
    }
}
