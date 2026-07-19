import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

imports_new = """
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
"""
if "GmsDocumentScannerOptions" not in text:
    text = text.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\n" + imports_new.strip())

scanner_launcher = """
                var startLiveScanner by remember { mutableStateOf(false) }
                
                val scannerOptions = remember {
                    GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(1)
                        .setResultFormats(RESULT_FORMAT_JPEG)
                        .setScannerMode(SCANNER_MODE_FULL)
                        .build()
                }
                
                val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }
                
                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { activityResult ->
                    if (activityResult.resultCode == Activity.RESULT_OK) {
                        val resultData = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
                        resultData?.pages?.firstOrNull()?.imageUri?.let { uri ->
                            isProcessing = true
                            coroutineScope.launch(Dispatchers.Default) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                    inputStream?.close()
                                    
                                    if (bitmap != null) {
                                        val res = OmrScanner.scan(bitmap, numQuestions, numOptions, "Standard")
                                        withContext(Dispatchers.Main) {
                                            scanResult = res
                                            isProcessing = false
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed to decode image", Toast.LENGTH_SHORT).show()
                                            isProcessing = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        isProcessing = false
                                    }
                                }
                            }
                        }
                    }
                }

                if (startLiveScanner) {
                    // Start ML Kit scanner
                    LaunchedEffect(Unit) {
                        scanner.getStartScanIntent(context as Activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                startLiveScanner = false
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to start scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                                startLiveScanner = false
                            }
                    }
                } else {
"""

old_scanner_start = """
                var startLiveScanner by remember { mutableStateOf(false) }
                if (startLiveScanner) {
                    LiveScanner(
                        numQuestions = numQuestions,
                        numOptions = numOptions,
                        onScanSuccess = { res ->
                            scanResult = res
                            startLiveScanner = false
                        },
                        onCancel = {
                            startLiveScanner = false
                        }
                    )
                } else {
"""

text = text.replace(old_scanner_start.strip(), scanner_launcher.strip())

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(text)

