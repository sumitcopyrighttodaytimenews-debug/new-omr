
package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object OmrScanner {
    data class ScanResult(
        val studentId: String,
        val paperSet: String,
        val answers: List<Int>,
        val annotatedBitmap: Bitmap,
        val optionCoords: List<List<Pair<Float, Float>>> = emptyList()
    )

    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, templateType: String = "Standard"): ScanResult {
        // 1. Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // 2. Preprocess
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        
        // 3. Find Corner Markers
        val corners = findCornersOpenCV(gray)
        
        val w = 1000.0
        val h = 1000.0
        
        val srcPoints = if (corners != null) {
            corners
        } else {
            // Absolute fallback: Assume image is perfectly cropped A4 page, 
            // guess the marker positions based on expected layout percentages.
            listOf(
                Point(bitmap.width * 0.05, bitmap.height * 0.35),
                Point(bitmap.width * 0.95, bitmap.height * 0.35),
                Point(bitmap.width * 0.05, bitmap.height * 0.96),
                Point(bitmap.width * 0.95, bitmap.height * 0.96)
            )
        }

        // 4. Perspective Transform
        val srcMat = MatOfPoint2f(*srcPoints.toTypedArray())
        val dstMat = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(w, 0.0),
            Point(0.0, h),
            Point(w, h)
        )
        
        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        
        val warped = Mat()
        Imgproc.warpPerspective(mat, warped, perspectiveTransform, Size(w, h))
        
        val warpedGray = Mat()
        Imgproc.warpPerspective(gray, warpedGray, perspectiveTransform, Size(w, h))
        
        // Apply Thresholding on warped image for bubble detection
        val warpedBlurred = Mat()
        Imgproc.GaussianBlur(warpedGray, warpedBlurred, Size(5.0, 5.0), 0.0)
        
        val warpedThresh = Mat()
        // Adaptive thresholding to handle uneven lighting
        Imgproc.adaptiveThreshold(warpedBlurred, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        // Draw debug overlay on warped image
        val warpedAnnotated = warped.clone()
        val colorRed = Scalar(255.0, 0.0, 0.0, 255.0)
        val colorGreen = Scalar(0.0, 255.0, 0.0, 255.0)
        val colorBlue = Scalar(0.0, 0.0, 255.0, 255.0)
        
        // 5. Bubble Mapping & Fill Detection
        
        // Read Set
        val setStartX = 120.0
        val setStartY = 50.0
        val setSpacingY = 94.736
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        
        var bestSetRow = -1
        var maxSetDarkness = 0.0
        var secondMaxSetDarkness = 0.0
        
        val bubbleRadius = 14.0
        
        for (i in setSets.indices) {
            val cx = setStartX
            val cy = setStartY + i * setSpacingY
            
            val fillPercentage = getFillPercentage(warpedThresh, cx, cy, bubbleRadius)
            
            // Draw ROI for debug
            Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorBlue, 2)
            
            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }
        
        // Configurable threshold for considering a bubble "filled"
        val fillThreshold = 0.35 // 35% of pixels in the ROI are dark
        val marginThreshold = 0.15 // Difference between top and second top
        
        val paperSet = if (maxSetDarkness > fillThreshold) {
            if (secondMaxSetDarkness > fillThreshold || (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                val cx = setStartX
                val cy = setStartY + bestSetRow * setSpacingY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorGreen, -1)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }
        
        // Student ID (Assuming it's filled in bubbles or we can just return a placeholder for now since we focused on SET and Answers)
        // If templateType == "Standard", we can try QR/Barcode on the warped image, but ZXing needs Bitmap.
        // For now, let's keep it simple.
        var studentId = "?"
        
        // Read Answers
        val ansBaseX = 230.0
        val ansBaseY = 50.0
        val ansColStrideX = 160.0
        val ansSpacingX = 30.0
        val ansSpacingY = 47.368
        val ansBubbleRadius = 11.0
        
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
                
                // Store mapped coordinates for UI overlay
                currentOptionCoords.add(Pair(cx.toFloat(), cy.toFloat()))
                
                val fillPercentage = getFillPercentage(warpedThresh, cx, cy, ansBubbleRadius)
                
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), colorBlue, 2)
                
                if (fillPercentage > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = fillPercentage
                    bestOpt = opt
                } else if (fillPercentage > secondMaxDarkness) {
                    secondMaxDarkness = fillPercentage
                }
            }
            
            allOptionCoords.add(currentOptionCoords)
            
            var studentAns = -1 // NOT_ATTEMPTED
            
            if (maxDarkness > fillThreshold) {
                if (secondMaxDarkness > fillThreshold || (maxDarkness - secondMaxDarkness) < marginThreshold) {
                    studentAns = -2 // MULTIPLE_MARKED
                } else {
                    studentAns = bestOpt
                }
            }
            
            if (studentAns >= 0) {
                val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), colorRed, -1)
            } else if (studentAns == -2) {
                // Draw yellow circle for multiple marked
                val cx = qStartX + bestOpt * ansSpacingX
                val cy = qStartY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), Scalar(255.0, 255.0, 0.0, 255.0), 2)
            }
            
            answers.add(studentAns)
        }
        
        // Convert back to Bitmap
        val finalBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedAnnotated, finalBitmap)
        
        return ScanResult(studentId, paperSet, answers, finalBitmap, allOptionCoords)
    }

    private fun getFillPercentage(threshInv: Mat, cx: Double, cy: Double, radius: Double): Double {
        val r = (radius * 0.8).toInt() // Slightly smaller ROI to avoid bubble border
        
        val x = (cx - r).toInt().coerceAtLeast(0)
        val y = (cy - r).toInt().coerceAtLeast(0)
        val w = (r * 2).coerceAtMost(threshInv.width() - x)
        val h = (r * 2).coerceAtMost(threshInv.height() - y)
        
        if (w <= 0 || h <= 0) return 0.0
        
        val roi = threshInv.submat(Rect(x, y, w, h))
        val nonZero = Core.countNonZero(roi)
        val totalPixels = w * h
        
        return nonZero.toDouble() / totalPixels
    }

private fun findMarkerInRegion(gray: Mat, xStart: Int, xEnd: Int, yStart: Int, yEnd: Int): Point? {
        val windowSize = (gray.width() * 0.022).toInt() // ~2.2% of width
        val step = Math.max(1, windowSize / 4)
        
        var minMean = 255.0
        var bestCx = -1
        var bestCy = -1
        
        for (y in yStart until (yEnd - windowSize) step step) {
            for (x in xStart until (xEnd - windowSize) step step) {
                val roi = gray.submat(Rect(x, y, windowSize, windowSize))
                val mean = Core.mean(roi).`val`[0]
                if (mean < minMean) {
                    minMean = mean
                    bestCx = x + windowSize / 2
                    bestCy = y + windowSize / 2
                }
            }
        }
        
        if (minMean < 120) {
            return Point(bestCx.toDouble(), bestCy.toDouble())
        }
        return null
    }

    private fun findCornersOpenCV(gray: Mat): List<Point>? {
        val w = gray.width()
        val h = gray.height()
        
        // Expected regions for the 4 markers
        // TL: Left 1-12%, Top 28-42%
        val tl = findMarkerInRegion(gray, (w * 0.01).toInt(), (w * 0.12).toInt(), (h * 0.28).toInt(), (h * 0.42).toInt())
        // TR: Right 88-99%, Top 28-42%
        val tr = findMarkerInRegion(gray, (w * 0.88).toInt(), (w * 0.99).toInt(), (h * 0.28).toInt(), (h * 0.42).toInt())
        // BL: Left 1-12%, Bottom 85-98%
        val bl = findMarkerInRegion(gray, (w * 0.01).toInt(), (w * 0.12).toInt(), (h * 0.85).toInt(), (h * 0.98).toInt())
        // BR: Right 88-99%, Bottom 85-98%
        val br = findMarkerInRegion(gray, (w * 0.88).toInt(), (w * 0.99).toInt(), (h * 0.85).toInt(), (h * 0.98).toInt())
        
        if (tl != null && tr != null && bl != null && br != null) {
            return listOf(tl, tr, bl, br)
        }
        return null
    }
}
