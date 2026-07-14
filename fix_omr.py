with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

text = text.replace(
"""    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)""",
"""    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)"""
)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)
