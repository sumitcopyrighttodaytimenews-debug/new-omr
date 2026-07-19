package com.example.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.util.OmrScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveScannerScreen(
    numQuestions: Int,
    numOptions: Int,
    onScanSuccess: (OmrScanner.ScanResult) -> Unit,
    onCancel: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        LiveCameraPreview(numQuestions, numOptions, onScanSuccess, onCancel)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan OMR sheets.")
        }
    }
}

@Composable
fun LiveCameraPreview(
    numQuestions: Int,
    numOptions: Int,
    onScanSuccess: (OmrScanner.ScanResult) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Align OMR sheet within the frame") }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isAnalyzing) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                
                                val bitmap = imageProxy.toBitmap()
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                imageProxy.close()
                                
                                if (bitmap == null) return@setAnalyzer

                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                isAnalyzing = true
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        scanStatus = "Analyzing..."
                                        val result = OmrScanner.scan(rotatedBitmap, numQuestions, numOptions)
                                        
                                        // Confidence check
                                        val isConfident = result.studentId != "?" && result.studentId.isNotEmpty()
                                        
                                        if (isConfident) {
                                            launch(Dispatchers.Main) {
                                                scanStatus = "Success!"
                                                onScanSuccess(result)
                                            }
                                        } else {
                                            launch(Dispatchers.Main) {
                                                scanStatus = "Keep camera steady..."
                                            }
                                            delay(500) // Don't burn CPU
                                            isAnalyzing = false
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LiveScanner", "Error analyzing frame", e)
                                        delay(500)
                                        isAnalyzing = false
                                    }
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("LiveScanner", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Guide
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameWidth = canvasWidth * 0.8f
            val frameHeight = frameWidth * 1.414f // A4 ratio
            
            val left = (canvasWidth - frameWidth) / 2f
            val top = (canvasHeight - frameHeight) / 2f
            
            val boxSize = 80.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val blueColor = Color.Blue
            
            // Top Left Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
            
            // Top Right Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left + frameWidth - boxSize, top),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
            
            // Bottom Left Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left, top + frameHeight - boxSize),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
            
            // Bottom Right Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left + frameWidth - boxSize, top + frameHeight - boxSize),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(72.dp)
                .background(Color.White, shape = CircleShape)
                .clickable { /* Shutter button action to be implemented later */ },
            contentAlignment = Alignment.Center
        ) {
            // White circle button
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = scanStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}
