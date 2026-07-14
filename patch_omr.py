import re
with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Replace the corner finding logic
old_logic = """        // Find the 4 corner squares of the OMR sheet
        val tl = findCorner(bitmap, isLeft = true, isTop = true)
        val tr = findCorner(bitmap, isLeft = false, isTop = true)
        val bl = findCorner(bitmap, isLeft = true, isTop = false)
        val br = findCorner(bitmap, isLeft = false, isTop = false)

        Log.d("OmrScanner", "Corners: TL=$tl, TR=$tr, BL=$bl, BR=$br")

        // Draw corners on annotated bitmap
        canvas.drawCircle(tl.first, tl.second, 20f, paintBlue)
        canvas.drawCircle(tr.first, tr.second, 20f, paintBlue)
        canvas.drawCircle(bl.first, bl.second, 20f, paintBlue)
        canvas.drawCircle(br.first, br.second, 20f, paintBlue)

        // Source points (camera image centers of the corner squares)
        val src = floatArrayOf(
            tl.first, tl.second,
            tr.first, tr.second,
            br.first, br.second,
            bl.first, bl.second
        )
        // Destination points (virtual sheet 1000x1414, mapping to the outer edges of the 40x40 corner squares at margin 30)
        val dst = floatArrayOf(
            30f, 30f,
            970f, 30f,
            970f, 1384f,
            30f, 1384f
        )"""

new_logic = """        // Assume the bitmap is cropped by ML Kit Document Scanner
        // So the corners of the image roughly correspond to the corners of the 1000x1414 virtual sheet
        val src = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        )
        val dst = floatArrayOf(
            0f, 0f,
            1000f, 0f,
            1000f, 1414f,
            0f, 1414f
        )"""

content = content.replace(old_logic, new_logic)

# Increase search radius for timing marks
content = content.replace("val searchRadius = Math.max(10, bitmap.width / 60)", "val searchRadius = Math.max(20, bitmap.width / 25)")

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
