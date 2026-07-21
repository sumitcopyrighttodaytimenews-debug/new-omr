package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

object OmrScanner {
    private const val TAG = "OmrScanner"

    data class ScanResult(
        val studentId: String,
        val paperSet: String,
        val answers: List<Int>,
        val annotatedBitmap: Bitmap,
        val optionCoords: List<List<Pair<Float, Float>>> = emptyList()
    )

    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, templateType: String = "Standard"): ScanResult {
        // Assume the bitmap is ALREADY properly cropped and warped by ML Kit Document Scanner
        // We will scale it to a standard size for consistent processing
        val w = 1000
        val h = 1000
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true)
        
        // Prepare an annotated bitmap to draw results on
        val annotatedBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotatedBitmap)
        
        val paintRed = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            alpha = 150
        }
        val paintGreen = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            alpha = 150
        }
        val paintBlue = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val paintYellow = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        // We will work directly with the bitmap's pixels
        val pixels = IntArray(w * h)
        scaledBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        fun getFillPercentage(cx: Float, cy: Float, radius: Float): Double {
            val r = (radius * 0.6f).toInt()
            val startX = (cx - r).toInt().coerceAtLeast(0)
            val startY = (cy - r).toInt().coerceAtLeast(0)
            val endX = (cx + r).toInt().coerceAtMost(w - 1)
            val endY = (cy + r).toInt().coerceAtMost(h - 1)

            var darkPixels = 0
            var totalPixels = 0

            for (y in startY..endY) {
                for (x in startX..endX) {
                    val dx = x - cx
                    val dy = y - cy
                    if (dx * dx + dy * dy <= r * r) {
                        totalPixels++
                        val pixel = pixels[y * w + x]
                        // Simple grayscale conversion
                        val rVal = Color.red(pixel)
                        val gVal = Color.green(pixel)
                        val bVal = Color.blue(pixel)
                        val luminance = (0.299 * rVal + 0.587 * gVal + 0.114 * bVal)
                        // Threshold for "darkness"
                        if (luminance < 130) {
                            darkPixels++
                        }
                    }
                }
            }
            return if (totalPixels > 0) darkPixels.toDouble() / totalPixels else 0.0
        }

        // Read Set
        val setStartX = 120f
        val setStartY = 50f
        val setSpacingY = 94.736f
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")

        var bestSetRow = -1
        var maxSetDarkness = 0.0
        var secondMaxSetDarkness = 0.0
        val bubbleRadius = 14f

        for (i in setSets.indices) {
            val cx = setStartX
            val cy = setStartY + i * setSpacingY

            val fillPercentage = getFillPercentage(cx, cy, bubbleRadius)
            canvas.drawCircle(cx, cy, bubbleRadius, paintBlue)

            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }

        val fillThreshold = 0.25 // Slightly lower because we aren't adaptive thresholding
        val marginThreshold = 0.15

        val paperSet = if (maxSetDarkness > fillThreshold) {
            if (secondMaxSetDarkness > maxSetDarkness * 0.75 || (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                val cx = setStartX
                val cy = setStartY + bestSetRow * setSpacingY
                canvas.drawCircle(cx, cy, bubbleRadius, paintGreen)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }

        // Read Answers
        val ansColStrideX = 160f
        val ansBaseX = 230f
        val ansSpacingX = 30f
        val ansSpacingY = 47.368f
        val ansBaseY = 50f
        val ansBubbleRadius = 14f

        val questionsPerColumn = 20
        val answers = mutableListOf<Int>()
        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()

        for (q in 0 until numQuestions) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn

            val qStartX = ansBaseX + col * ansColStrideX
            val qStartY = ansBaseY + row * ansSpacingY

            var maxDarkness = 0.0
            var secondMaxDarkness = 0.0
            var bestOpt = -1

            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()

            for (opt in 0 until numOptions) {
                val cx = qStartX + opt * ansSpacingX
                val cy = qStartY

                currentOptionCoords.add(Pair(cx, cy))

                val fillPercentage = getFillPercentage(cx, cy, ansBubbleRadius)
                canvas.drawCircle(cx, cy, ansBubbleRadius, paintBlue)

                if (fillPercentage > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = fillPercentage
                    bestOpt = opt
                } else if (fillPercentage > secondMaxDarkness) {
                    secondMaxDarkness = fillPercentage
                }
            }

            allOptionCoords.add(currentOptionCoords)

            var studentAns = -1

            if (maxDarkness > fillThreshold) {
                if (secondMaxDarkness > maxDarkness * 0.75 || (maxDarkness - secondMaxDarkness) < marginThreshold) {
                    studentAns = -2 // MULTIPLE
                } else {
                    studentAns = bestOpt
                }
            }

            if (studentAns >= 0) {
                val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                canvas.drawCircle(cx, cy, ansBubbleRadius, paintRed)
            } else if (studentAns == -2) {
                val cx = qStartX + bestOpt * ansSpacingX
                val cy = qStartY
                canvas.drawCircle(cx, cy, ansBubbleRadius, paintYellow)
            }

            answers.add(studentAns)
        }

        // Student ID QR Code
        var studentId = "?"
        try {
            val qrId = readQr(scaledBitmap)
            if (qrId != null) {
                studentId = qrId
            }
        } catch (e: Exception) {
            Log.e(TAG, "QR code read failed", e)
        }

        return ScanResult(studentId, paperSet, answers, annotatedBitmap, allOptionCoords)
    }

    private fun readQr(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)

            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))

            val hints = java.util.EnumMap<com.google.zxing.DecodeHintType, Any>(com.google.zxing.DecodeHintType::class.java)
            hints[com.google.zxing.DecodeHintType.TRY_HARDER] = true

            MultiFormatReader().decode(binary, hints).text
        } catch (e: Exception) {
            null
        }
    }
}
