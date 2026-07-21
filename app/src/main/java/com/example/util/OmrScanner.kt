package com.example.util

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
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
    private const val TAG = "OmrScanner"
    
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
            // Absolute fallback: Assume image is perfectly cropped A4 page
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
        
        // Draw the srcPoints on the original mat for debugging if they were found
        val colorCorner = Scalar(255.0, 0.0, 255.0, 255.0) // Magenta
        for (pt in srcPoints) {
            Imgproc.circle(mat, pt, 20, colorCorner, -1)
        }
        
        val warpedGray = Mat()
        Imgproc.warpPerspective(gray, warpedGray, perspectiveTransform, Size(w, h))
        
        // Apply Thresholding on warped image for bubble detection
        val warpedBlurred = Mat()
        Imgproc.GaussianBlur(warpedGray, warpedBlurred, Size(5.0, 5.0), 0.0)
        
        val warpedThresh = Mat()
        Imgproc.adaptiveThreshold(warpedBlurred, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        // Draw debug overlay on warped image
        val warpedAnnotated = warped.clone()
        val colorRed = Scalar(255.0, 0.0, 0.0, 255.0)
        val colorGreen = Scalar(0.0, 255.0, 0.0, 255.0)
        val colorBlue = Scalar(0.0, 0.0, 255.0, 255.0)
        
        // --- BUBBLE SNAPPING ---
        val bubbleCenters = mutableListOf<Point>()
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(warpedThresh.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 80 && area < 1200) {
                 val contour2f = MatOfPoint2f(*contour.toArray())
                 val perimeter = Imgproc.arcLength(contour2f, true)
                 if (perimeter > 0) {
                     val circularity = 4 * Math.PI * area / (perimeter * perimeter)
                     if (circularity > 0.65) {
                         val rect = Imgproc.boundingRect(contour)
                         val cx = rect.x + rect.width / 2.0
                         val cy = rect.y + rect.height / 2.0
                         bubbleCenters.add(Point(cx, cy))
                     }
                 }
            }
        }

        fun snapToNearest(cx: Double, cy: Double, centers: List<Point>, maxDist: Double): Point {
            var bestPt = Point(cx, cy)
            var minDist = maxDist
            for (pt in centers) {
                val dist = Math.hypot(pt.x - cx, pt.y - cy)
                if (dist < minDist) {
                    minDist = dist
                    bestPt = pt
                }
            }
            return bestPt
        }
        // ------------------------
        
        // --- TIMING MARKS ---
        val (leftMarks, rightMarks) = detectTimingMarks(warpedThresh)
        var topY = 50.0
        var bottomY = 950.0
        var rowStep = 47.368
        
        if (leftMarks.isNotEmpty()) {
            topY = leftMarks.first().y
            bottomY = leftMarks.last().y
            if (leftMarks.size > 1) {
                rowStep = (bottomY - topY) / Math.max(1, leftMarks.size - 1).toDouble()
            }
        }
        
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
            var cx = setStartX
            var cy = setStartY + i * setSpacingY
            
            val snapped = snapToNearest(cx, cy, bubbleCenters, 30.0)
            cx = snapped.x
            cy = snapped.y
            
            val fillPercentage = getFillPercentage(warpedThresh, cx, cy, bubbleRadius)
            
            Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorBlue, 2)
            
            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }
        
        val fillThreshold = 0.35
        val marginThreshold = 0.20
        
        val paperSet = if (maxSetDarkness > fillThreshold) {
            if (secondMaxSetDarkness > maxSetDarkness * 0.75 || (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                var cx = setStartX
                var cy = setStartY + bestSetRow * setSpacingY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 30.0)
                Imgproc.circle(warpedAnnotated, snapped, bubbleRadius.toInt(), colorGreen, -1)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }
        
        // Read Answers
        val answerLeft = 230.0
        val answerRight = 870.0
        val colWidth = (answerRight - answerLeft) / 4.0
        val ansColStrideX = 160.0
        val ansBaseX = 230.0
        val ansSpacingX = 30.0
        val ansBubbleRadius = 14.0
        
        val questionsPerColumn = 20
        val answers = mutableListOf<Int>()
        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()
        
        for (q in 0 until numQuestions) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn
            
            val qStartX = ansBaseX + col * ansColStrideX
            
            // Safer logic for dynamic grid Y axis
            val finalQStartY = if (leftMarks.size in 18..22) {
                topY + row * rowStep
            } else {
                50.0 + row * 47.368
            }
            
            var maxDarkness = 0.0
            var secondMaxDarkness = 0.0
            var bestOpt = -1
            
            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()
            
            for (opt in 0 until numOptions) {
                var cx = qStartX + opt * ansSpacingX
                var cy = finalQStartY
                
                val snapped = snapToNearest(cx, cy, bubbleCenters, 14.0)
                cx = snapped.x
                cy = snapped.y
                
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
                if (secondMaxDarkness > maxDarkness * 0.75 || (maxDarkness - secondMaxDarkness) < marginThreshold) {
                    studentAns = -2 // MULTIPLE_MARKED
                } else {
                    studentAns = bestOpt
                }
            }
            
            if (studentAns >= 0) {
                var cx = qStartX + studentAns * ansSpacingX
                var cy = finalQStartY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 14.0)
                Imgproc.circle(warpedAnnotated, snapped, ansBubbleRadius.toInt(), colorRed, -1)
            } else if (studentAns == -2) {
                var cx = qStartX + bestOpt * ansSpacingX
                var cy = finalQStartY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 14.0)
                Imgproc.circle(warpedAnnotated, snapped, ansBubbleRadius.toInt(), Scalar(255.0, 255.0, 0.0, 255.0), 2)
            }
            
            answers.add(studentAns)
        }
        
        // Convert back to Bitmap
        val finalBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedAnnotated, finalBitmap)
        
        // Student ID QR Code (safely handle OOM and errors)
        var studentId = "?"
        try {
            val qrId = readQr(finalBitmap)
            if (qrId != null) {
                studentId = qrId
            }
        } catch (e: Exception) {
            Log.e(TAG, "QR code read failed", e)
        }
        
        // Clean up Mats to prevent OOM / crash
        mat.release()
        gray.release()
        warped.release()
        warpedGray.release()
        warpedBlurred.release()
        warpedThresh.release()
        warpedAnnotated.release()
        
        return ScanResult(studentId, paperSet, answers, finalBitmap, allOptionCoords)
    }

    private fun detectTimingMarks(thresh: Mat): Pair<List<Point>, List<Point>> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        
        val threshClone = thresh.clone()
        Imgproc.findContours(
            threshClone,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        threshClone.release()
        hierarchy.release()
        
        val leftMarks = mutableListOf<Point>()
        val rightMarks = mutableListOf<Point>()
        
        val width = thresh.width()
        
        for (c in contours) {
            val rect = Imgproc.boundingRect(c)
            val area = rect.width * rect.height
            
            if (area < 50 || area > 1500) continue
            
            val ratio = rect.width.toDouble() / rect.height
            if (ratio < 1.5) continue
            
            val center = Point(
                rect.x + rect.width / 2.0,
                rect.y + rect.height / 2.0
            )
            
            if (center.x < width * 0.15)
                leftMarks.add(center)
            
            if (center.x > width * 0.85)
                rightMarks.add(center)
        }
        
        leftMarks.sortBy { it.y }
        rightMarks.sortBy { it.y }
        
        return Pair(leftMarks, rightMarks)
    }

    private fun getFillPercentage(threshInv: Mat, cx: Double, cy: Double, radius: Double): Double {
        val r = (radius * 0.6).toInt() 
        
        val x = (cx - r).toInt()
        val y = (cy - r).toInt()
        val w = (r * 2)
        val h = (r * 2)
        
        // CRASH FIX: Ensure rect is fully within Mat boundaries
        if (x < 0 || y < 0 || x + w > threshInv.width() || y + h > threshInv.height() || w <= 0 || h <= 0) {
            return 0.0
        }
        
        val roi = threshInv.submat(Rect(x, y, w, h))
        
        val mask = Mat.zeros(roi.size(), CvType.CV_8U)
        Imgproc.circle(
            mask,
            Point(roi.width() / 2.0, roi.height() / 2.0),
            r,
            Scalar(255.0),
            -1
        )
        
        val maskedRoi = roi.clone()
        Core.bitwise_and(maskedRoi, mask, maskedRoi)
        
        val filled = Core.countNonZero(maskedRoi)
        val total = Core.countNonZero(mask)
        
        // Release Mats
        roi.release()
        mask.release()
        maskedRoi.release()
        
        return if (total > 0) filled.toDouble() / total else 0.0
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

    private fun findCornersOpenCV(gray: Mat): List<Point>? {
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(7.0, 7.0), 0.0)
        
        val thresh = Mat()
        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 51, 10.0)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val squareCenters = mutableListOf<Point>()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 150 && area < 10000) {
                val rect = Imgproc.boundingRect(contour)
                val aspectRatio = rect.width.toDouble() / rect.height
                val extent = area / (rect.width * rect.height)
                
                if (aspectRatio > 0.6 && aspectRatio < 1.4 && extent > 0.5) {
                    val center = Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0)
                    
                    val mask = Mat.zeros(gray.size(), CvType.CV_8U)
                    Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                    val mean = Core.mean(gray, mask).`val`[0]
                    mask.release()
                    if (mean < 130) {
                        squareCenters.add(center)
                    }
                }
            }
        }
        
        blurred.release()
        thresh.release()
        hierarchy.release()
        
        if (squareCenters.size >= 4) {
            val w = gray.width().toDouble()
            val h = gray.height().toDouble()
            
            var tl = squareCenters[0]
            var tr = squareCenters[0]
            var bl = squareCenters[0]
            var br = squareCenters[0]
            
            var minTl = Double.MAX_VALUE
            var minTr = Double.MAX_VALUE
            var minBl = Double.MAX_VALUE
            var minBr = Double.MAX_VALUE
            
            for (pt in squareCenters) {
                val valTl = pt.x + pt.y
                val valTr = (w - pt.x) + pt.y
                val valBl = pt.x + (h - pt.y)
                val valBr = (w - pt.x) + (h - pt.y)
                
                if (valTl < minTl) { minTl = valTl; tl = pt }
                if (valTr < minTr) { minTr = valTr; tr = pt }
                if (valBl < minBl) { minBl = valBl; bl = pt }
                if (valBr < minBr) { minBr = valBr; br = pt }
            }
            
            val areaQuad = 0.5 * Math.abs(
                tl.x*tr.y - tl.y*tr.x + 
                tr.x*br.y - tr.y*br.x + 
                br.x*bl.y - br.y*bl.x + 
                bl.x*tl.y - bl.y*tl.x
            )
            
            if (areaQuad > w * h * 0.1) {
                return listOf(tl, tr, bl, br)
            }
        }
        
        var maxAreaContour = 0.0
        var bestApprox = MatOfPoint2f()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > gray.width() * gray.height() * 0.1) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
                
                if (approx.total() == 4L && area > maxAreaContour) {
                    maxAreaContour = area
                    approx.copyTo(bestApprox)
                }
            }
        }
        
        if (bestApprox.total() == 4L) {
             val points = bestApprox.toList()
             val sortedByY = points.sortedBy { it.y }
             val top = sortedByY.take(2).sortedBy { it.x }
             val bottom = sortedByY.drop(2).sortedBy { it.x }
             return listOf(top[0], top[1], bottom[0], bottom[1])
        }
        
        return null
    }
}
