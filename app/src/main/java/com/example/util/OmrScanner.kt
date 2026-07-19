package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log

object OmrScanner {

    data class ScanResult(val studentId: String, val paperSet: String, val answers: List<Int>, val annotatedBitmap: Bitmap, val optionCoords: List<List<Pair<Float, Float>>> = emptyList())

    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, templateType: String = "Standard"): ScanResult {
                val width = bitmap.width
        val height = bitmap.height
        val annotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(annotatedBitmap)
        val paintGreen = android.graphics.Paint().apply { color = Color.GREEN; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }
        val paintRed = android.graphics.Paint().apply { color = Color.RED; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }
        val paintBlue = android.graphics.Paint().apply { color = Color.BLUE; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }
        val paintText = android.graphics.Paint().apply { color = Color.RED; textSize = 40f; isAntiAlias = true; style = android.graphics.Paint.Style.FILL }


        // Find the 4 corner squares of the OMR sheet
        // Find the 4 corner squares of the OMR sheet
        val tl = findCorner(bitmap, isLeft = true, isTop = true)
        val tr = findCorner(bitmap, isLeft = false, isTop = true)
        val bl = findCorner(bitmap, isLeft = true, isTop = false)
        val br = findCorner(bitmap, isLeft = false, isTop = false)

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
        // Destination points (virtual sheet 1000x1414)
        // The corner squares are drawn at margin 30, with size 40x40.
        // So their centers are at (50, 50), (950, 50), (950, 1364), (50, 1364)
        val dst = floatArrayOf(
            50f, 50f,
            950f, 50f,
            950f, 1364f,
            50f, 1364f
        )
        val matrix = Matrix()
        // Map from Virtual Sheet coordinates to Camera Image coordinates
        matrix.setPolyToPoly(dst, 0, src, 0, 4)

        fun mapVirtualToActual(vx: Float, vy: Float): Pair<Float, Float> {
            val pts = floatArrayOf(vx, vy)
            matrix.mapPoints(pts)
            return Pair(pts[0], pts[1])
        }

        // Read Set
        val setStartX = 215f
        val setStartY = 590f
        val setSpacingY = 60f
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        var bestSetRow = -1
        var maxSetDarkness = 0f
        var secondMaxSetDarkness = 0f
        
        for (i in setSets.indices) {
            val vx = setStartX
            val vy = setStartY + i * setSpacingY
            val actual = mapVirtualToActual(vx, vy)
            val darkness = sampleDarkness(bitmap, matrix, vx, vy, actual.first, actual.second, 14f)
            
            canvas.drawCircle(actual.first, actual.second, 15f, paintRed)
            
            if (darkness > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = darkness
                bestSetRow = i
            } else if (darkness > secondMaxSetDarkness) {
                secondMaxSetDarkness = darkness
            }
        }
        val paperSet = if (maxSetDarkness > 0.25f && (maxSetDarkness - secondMaxSetDarkness) > 0.10f && bestSetRow != -1) {
            val vx = setStartX
            val vy = setStartY + bestSetRow * setSpacingY
            val actual = mapVirtualToActual(vx, vy)
            canvas.drawCircle(actual.first, actual.second, 15f, paintGreen)
            setSets[bestSetRow] 
        } else {
            "?"
        }

        
        var studentId = "?"
        if (templateType == "Standard") {
            // Read Student ID using ZXing
        
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            
            val hints = java.util.EnumMap<com.google.zxing.DecodeHintType, Any>(com.google.zxing.DecodeHintType::class.java)
            hints[com.google.zxing.DecodeHintType.TRY_HARDER] = true
            
            val reader = com.google.zxing.MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            studentId = result.text
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: try to decode only the top half of the image
            try {
                val width = bitmap.width
                val height = bitmap.height / 2
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                
                val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                
                val hints = java.util.EnumMap<com.google.zxing.DecodeHintType, Any>(com.google.zxing.DecodeHintType::class.java)
                hints[com.google.zxing.DecodeHintType.TRY_HARDER] = true
                
                val reader = com.google.zxing.MultiFormatReader()
                val result = reader.decode(binaryBitmap, hints)
                studentId = result.text
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
        } else {
            // Read Roll No Bubbles
            val rStartX = 150f
            val rStartY = 120f
            val rSpacingX = 42f
            val rSpacingY = 28f
            
            var rollNoStr = ""
            for (col in 0 until 7) {
                var bestDigit = -1
                var maxDarkness = 0f
                var secondMaxDarkness = 0f
                
                for (row in 0..9) {
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 28f + row * rSpacingY
                    val actual = mapVirtualToActual(cx, cy)
                    val darkness = sampleDarkness(bitmap, matrix, cx, cy, actual.first, actual.second, 8f)
                    
                    canvas.drawCircle(actual.first, actual.second, 10f, paintBlue)
                    
                    if (darkness > maxDarkness) {
                        secondMaxDarkness = maxDarkness
                        maxDarkness = darkness
                        bestDigit = row
                    } else if (darkness > secondMaxDarkness) {
                        secondMaxDarkness = darkness
                    }
                }
                
                if (maxDarkness > 0.25f && (maxDarkness - secondMaxDarkness) > 0.10f && bestDigit != -1) {
                    rollNoStr += bestDigit.toString()
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 28f + bestDigit * rSpacingY
                    val actual = mapVirtualToActual(cx, cy)
                    canvas.drawCircle(actual.first, actual.second, 12f, paintRed)
                } else {
                    rollNoStr += "?"
                }
            }
            studentId = rollNoStr
        }
    

        // Read Answers
        val ansBaseX = 316f
        val ansBaseY = 599.5f
        val ansColStrideX = 130f
        val ansSpacingX = 21f
        val ansSpacingY = 39f
        val ansBubbleRadius = 10f
        val questionsPerColumn = 20

        val answers = mutableListOf<Int>()
        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()

        for (q in 0 until numQuestions) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn
            val qStartX = ansBaseX + col * ansColStrideX
            val qStartY = ansBaseY + row * ansSpacingY

            var maxDarkness = 0f
            var secondMaxDarkness = 0f
            var bestOpt = -1

            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()
            for (opt in 0 until numOptions) {
                val vx = qStartX + opt * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                currentOptionCoords.add(actual)
                val darkness = sampleDarkness(bitmap, matrix, vx, vy, actual.first, actual.second, ansBubbleRadius)
                
                if (darkness > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = darkness
                    bestOpt = opt
                } else if (darkness > secondMaxDarkness) {
                    secondMaxDarkness = darkness
                }
            }
            allOptionCoords.add(currentOptionCoords)

            // A valid mark should be significantly darker than the empty bubbles and mostly filled
            val studentAns = if (maxDarkness > 0.25f && (maxDarkness - secondMaxDarkness) > 0.10f && bestOpt != -1) bestOpt else -1
            
            if (studentAns != -1) {
                val vx = qStartX + studentAns * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                canvas.drawCircle(actual.first, actual.second, ansBubbleRadius * 1.5f, paintRed)
            }
            
            answers.add(studentAns)
        }

        return ScanResult(studentId, paperSet, answers, annotatedBitmap, allOptionCoords)
    }

    private fun findCenterOfMass(bitmap: Bitmap, startX: Int, startY: Int, endX: Int, endY: Int): Pair<Float, Float>? {
        var sumX = 0L
        var sumY = 0L
        var count = 0
        
        // Find local background
        var bgSum = 0
        var bgCount = 0
        for (x in startX until endX step 2) {
            for (y in startY until endY step 2) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                bgSum += (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                bgCount++
            }
        }
        val bgAvg = if (bgCount > 0) bgSum / bgCount else 200
        val threshold = Math.min(140, bgAvg - 40)

        for (x in startX until endX) {
            for (y in startY until endY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Convert to grayscale
                val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                
                if (gray < threshold) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        return if (count > 20) { // lower threshold since area might be small
            Pair(sumX.toFloat() / count, sumY.toFloat() / count)
        } else {
            null
        }
    }

    private fun sampleDarkness(bitmap: Bitmap, matrix: Matrix, virtualX: Float, virtualY: Float, actualX: Float, actualY: Float, virtualRadius: Float): Float {
        // Map radius to image coordinates approximately
        val ptsRadius = floatArrayOf(virtualX + virtualRadius, virtualY)
        matrix.mapPoints(ptsRadius)
        val ptsCenter = floatArrayOf(virtualX, virtualY)
        matrix.mapPoints(ptsCenter)
        val rx = ptsRadius[0]
        val ry = ptsRadius[1]
        val radius = Math.hypot((rx - ptsCenter[0]).toDouble(), (ry - ptsCenter[1]).toDouble()).toInt()
        
        val actualRadius = (radius * 0.75f).toInt().coerceAtLeast(2) // Sample inner core avoiding borders

        val cx = actualX.toInt().coerceIn(0, bitmap.width - 1)
        val cy = actualY.toInt().coerceIn(0, bitmap.height - 1)

        if (cx - actualRadius < 0 || cy - actualRadius < 0 || cx + actualRadius >= bitmap.width || cy + actualRadius >= bitmap.height) {
            return 0f
        }

        var darkCount = 0
        var totalCount = 0

        // Calculate local background brightness
        var bgSum = 0
        var bgCount = 0
        val bgRadius = (radius * 1.5f).toInt().coerceAtLeast(actualRadius + 2)
        val bgMinRadius = (radius * 1.1f).toInt().coerceAtLeast(actualRadius + 1)
        
        val startX = (cx - bgRadius).coerceAtLeast(0)
        val startY = (cy - bgRadius).coerceAtLeast(0)
        val endX = (cx + bgRadius).coerceAtMost(bitmap.width - 1)
        val endY = (cy + bgRadius).coerceAtMost(bitmap.height - 1)

        for (dx in startX - cx..endX - cx) {
            for (dy in startY - cy..endY - cy) {
                val distSq = dx * dx + dy * dy
                if (distSq in (bgMinRadius * bgMinRadius)..bgRadius * bgRadius) {
                    val px = cx + dx
                    val py = cy + dy
                    if (px in 0 until bitmap.width && py in 0 until bitmap.height) {
                        val pixel = bitmap.getPixel(px, py)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        bgSum += (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        bgCount++
                    }
                }
            }
        }
        
        // If we found a background average, use it to make an adaptive threshold
        val bgAvg = if (bgCount > 0) bgSum / bgCount else 200
        val threshold = Math.min(160, bgAvg - 40) // Must be significantly darker than local background

        for (dx in -actualRadius..actualRadius) {
            for (dy in -actualRadius..actualRadius) {
                if (dx * dx + dy * dy <= actualRadius * actualRadius) {
                    val px = (cx + dx).coerceIn(0, bitmap.width - 1)
                    val py = (cy + dy).coerceIn(0, bitmap.height - 1)
                    val pixel = bitmap.getPixel(px, py)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    
                    if (gray < threshold) {
                        darkCount++
                    }
                    totalCount++
                }
            }
        }

        return if (totalCount == 0) 0f else darkCount.toFloat() / totalCount
    }

    private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        
        val searchWidth = width / 4
        val searchHeight = height / 4
        
        val startX = if (isLeft) 0 else width - searchWidth
        val startY = if (isTop) 0 else height - searchHeight
        val endX = if (isLeft) searchWidth else width
        val endY = if (isTop) searchHeight else height
        
        var bgSum = 0
        var bgCount = 0
        val step = Math.max(1, width / 100)
        for (x in startX until endX step step) {
            for (y in startY until endY step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                bgSum += (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                bgCount++
            }
        }
        val bgAvg = if (bgCount > 0) bgSum / bgCount else 200
        val threshold = Math.min(130, bgAvg - 40)
        
        val windowSize = width / 25
        
        var bestScore = Float.MAX_VALUE
        var bestX = if (isLeft) width * 0.05f else width * 0.95f
        var bestY = if (isTop) height * 0.05f else height * 0.95f
        var found = false
        
        for (x in startX until endX - windowSize step step) {
            for (y in startY until endY - windowSize step step) {
                var darkCount = 0
                for (wx in x until x + windowSize step step) {
                    for (wy in y until y + windowSize step step) {
                        val pixel = bitmap.getPixel(wx, wy)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        if (gray < threshold) darkCount++
                    }
                }
                
                val total = (windowSize/step) * (windowSize/step)
                if (total > 0 && darkCount.toFloat() / total > 0.4f) {
                    val cx = x + windowSize / 2f
                    val cy = y + windowSize / 2f
                    
                    val distCornerX = if (isLeft) cx else (width - cx)
                    val distCornerY = if (isTop) cy else (height - cy)
                    
                    val penaltyX = if (distCornerX < width * 0.015f) 500f else 0f
                    val penaltyY = if (distCornerY < height * 0.015f) 500f else 0f
                    
                    val score = distCornerX + distCornerY + penaltyX + penaltyY
                    
                    if (score < bestScore) {
                        bestScore = score
                        bestX = cx
                        bestY = cy
                        found = true
                    }
                }
            }
        }
        
        if (found) {
            var sumX = 0L
            var sumY = 0L
            var count = 0
            for (wx in (bestX - windowSize).toInt().coerceAtLeast(0) until (bestX + windowSize).toInt().coerceAtMost(width - 1)) {
                for (wy in (bestY - windowSize).toInt().coerceAtLeast(0) until (bestY + windowSize).toInt().coerceAtMost(height - 1)) {
                    val pixel = bitmap.getPixel(wx, wy)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    if (gray < threshold + 15) {
                        sumX += wx
                        sumY += wy
                        count++
                    }
                }
            }
            if (count > 0) {
                return Pair(sumX.toFloat() / count, sumY.toFloat() / count)
            }
        }
        
        return Pair(bestX, bestY)
    }
}
