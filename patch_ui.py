import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

target = """        // Show grid of questions
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {"""

replacement = """        // Show Annotated Image
        val imageBitmap = remember(scanResult) { scanResult?.annotatedBitmap?.asImageBitmap() }
        if (imageBitmap != null) {
            Text("Scanned Sheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = "Annotated OMR",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Show grid of questions
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
        f.write(content)
    print("Updated ScanOmrScreen.kt UI")
else:
    print("Target not found")
