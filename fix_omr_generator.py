import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    content = f.read()

# Replace the start
pattern_start = re.compile(r"    fun generateOmrBitmap\(context: android.content.Context, numQuestions: Int, numOptions: Int, student: Student\? = null, title: String = \"बिहार विद्यालय परीक्षा , समिति\", logoPath: String = \"\", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = \"Left\"\): Bitmap \{\n        val bitmap = Bitmap.createBitmap\(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888\)\n        val canvas = Canvas\(bitmap\)\n        canvas.drawColor\(Color.WHITE\)")

new_start = """    fun drawOmrToCanvas(context: android.content.Context, canvas: Canvas, numQuestions: Int, numOptions: Int, student: Student? = null, title: String = "बिहार विद्यालय परीक्षा , समिति", logoPath: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left") {
        canvas.drawColor(Color.WHITE)"""

content = pattern_start.sub(new_start, content)

# Replace the end
pattern_end = re.compile(r"        return bitmap\n    \}\n\}")
new_end = """    }

    fun generateOmrBitmap(context: android.content.Context, numQuestions: Int, numOptions: Int, student: Student? = null, title: String = "बिहार विद्यालय परीक्षा , समिति", logoPath: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left"): Bitmap {
        val bitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawOmrToCanvas(context, canvas, numQuestions, numOptions, student, title, logoPath, logoOpacity, logoSize, logoPosition)
        return bitmap
    }
}"""
content = pattern_end.sub(new_end, content)

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
    f.write(content)

print("Replaced!")
