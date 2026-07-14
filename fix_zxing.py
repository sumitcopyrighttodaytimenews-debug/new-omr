with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

# Fix the duplicate `pixels` in ZXing
text = text.replace(
"""        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)""",
"""        try {
            val source = com.google.zxing.RGBLuminanceSource(pwidth, pheight, pixels)""")

text = text.replace(
"""            // Fallback: try to decode only the top half of the image
            try {
                val width = bitmap.width
                val height = bitmap.height / 2
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                
                val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)""",
"""            // Fallback: try to decode only the top half of the image
            try {
                val halfPixels = pixels.copyOfRange(0, pwidth * (pheight / 2))
                val source = com.google.zxing.RGBLuminanceSource(pwidth, pheight / 2, halfPixels)""")

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)
