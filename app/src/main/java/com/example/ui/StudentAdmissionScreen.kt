package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAdmissionScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    val stream = "ARTS" // Fixed for now
    
    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }
    val availableSubjects = listOf("HINDI", "HISTORY", "GEOGRAPHY", "POLITICAL SCIENCE", "PSYCHOLOGY", "MUSIC", "SOCIOLOGY")
    var subjectsExpanded by remember { mutableStateOf(false) }

    var datePickerVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        dob = sdf.format(java.util.Date(millis))
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

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var compressedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imagePath by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Process and compress image
            val bmp = compressImage(context, it)
            compressedBitmap = bmp
            imagePath = saveImageToInternalStorage(context, bmp)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Admission", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White, titleContentColor = androidx.compose.ui.graphics.Color.Black, navigationIconContentColor = androidx.compose.ui.graphics.Color.Black, actionIconContentColor = androidx.compose.ui.graphics.Color.Black)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Image Picker
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imagePicker.launch("image/*") }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (compressedBitmap != null) {
                        Image(
                            bitmap = compressedBitmap!!.asImageBitmap(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                Text("Tap to add photo (Auto resized <100kb)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Gender
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gender", style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                            Text("Male", modifier = Modifier.clickable { gender = "Male" })
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                            Text("Female", modifier = Modifier.clickable { gender = "Female" })
                        }
                    }
                }

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text("Mother's Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Box(modifier = Modifier.fillMaxWidth().clickable { datePickerVisible = true }) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = {},
                        label = { Text("Date of Birth (DOB)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                OutlinedTextField(
                    value = mobileNo,
                    onValueChange = { mobileNo = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = stream,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Stream") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Subjects Dropdown (Multiple Selection)
                ExposedDropdownMenuBox(
                    expanded = subjectsExpanded,
                    onExpandedChange = { subjectsExpanded = !subjectsExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedSubjects.isEmpty()) "Select Subjects" else selectedSubjects.joinToString(", "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subjects") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectsExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectsExpanded,
                        onDismissRequest = { subjectsExpanded = false }
                    ) {
                        availableSubjects.forEach { subject ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedSubjects.contains(subject),
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(subject)
                                    }
                                },
                                onClick = {
                                    selectedSubjects = if (selectedSubjects.contains(subject)) {
                                        selectedSubjects - subject
                                    } else {
                                        selectedSubjects + subject
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.addStudent(
                            name = name,
                            fatherName = fatherName,
                            motherName = motherName,
                            gender = gender,
                            dob = dob,
                            mobileNo = mobileNo,
                            email = email,
                            stream = stream,
                            subjects = selectedSubjects.joinToString(", "),
                            imagePath = imagePath
                        ) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                    enabled = name.isNotBlank() && fatherName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Submit Admission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

fun compressImage(context: Context, uri: Uri): Bitmap {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    // Resize if too large
    var width = bitmap.width
    var height = bitmap.height
    val maxSize = 800
    if (width > maxSize || height > maxSize) {
        val ratio = width.toFloat() / height.toFloat()
        if (ratio > 1) {
            width = maxSize
            height = (width / ratio).toInt()
        } else {
            height = maxSize
            width = (height * ratio).toInt()
        }
    }
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

    var quality = 90
    var stream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    
    // Target 100kb (100 * 1024 bytes)
    while (stream.toByteArray().size > 100 * 1024 && quality > 10) {
        stream.reset()
        quality -= 10
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    }

    val compressedBytes = stream.toByteArray()
    return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
}

fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    val filename = "student_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    return file.absolutePath
}
