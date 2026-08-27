package com.openlight.notes.core.refinement

import com.openlight.notes.core.ink.Stroke
import kotlin.math.*

/**
 * Handwriting refinement (AD-11).
 * Deterministic geometry on our own stroke points.
 */
object HandwritingRefinement {

    /**
     * Tier 1: Straighten — auto-level slanted lines.
     * Cluster strokes into text lines, fit baseline, rotate by −slope.
     */
    fun straighten(strokes: List<Stroke>, threshold: Float = 50f): List<Stroke> {
        if (strokes.isEmpty()) return strokes

        // Cluster strokes into lines by vertical extent
        val lines = clusterIntoLines(strokes, threshold)

        // For each line, fit baseline and rotate
        return lines.flatMap { lineStrokes ->
            val baseline = fitBaseline(lineStrokes)
            val angle = atan2(baseline[1], baseline[0])
            rotateStrokes(lineStrokes, -angle, baseline[2], baseline[3])
        }
    }

    /**
     * Tier 2: Tidy — smooth, simplify, size-normalize.
     * Ramer–Douglas–Peucker + Catmull-Rom + x-height normalization.
     */
    fun tidy(strokes: List<Stroke>, epsilon: Float = 2f): List<Stroke> {
        return strokes.map { stroke ->
            val simplified = rdpSimplify(stroke.points, epsilon)
            val smoothed = catmullRom(simplify, 3)
            stroke.copy(points = smoothed)
        }
    }

    /**
     * Cluster strokes into text lines by vertical extent + time adjacency.
     */
    private fun clusterIntoLines(strokes: List<Stroke>, threshold: Float): List<List<Stroke>> {
        if (strokes.isEmpty()) return emptyList()

        val sorted = strokes.sortedBy { stroke ->
            stroke.points.minOfOrNull { it[1] } ?: 0f
        }

        val lines = mutableListOf<MutableList<Stroke>>()
        var currentLine = mutableListOf(sorted[0])
        var currentMin = sorted[0].points.minOfOrNull { it[1] } ?: 0f
        var currentMax = sorted[0].points.maxOfOrNull { it[1] } ?: 0f

        for (i in 1 until sorted.size) {
            val stroke = sorted[i]
            val strokeMin = stroke.points.minOfOrNull { it[1] } ?: 0f
            val strokeMax = stroke.points.maxOfOrNull { it[1] } ?: 0f

            if (strokeMin < currentMax + threshold && strokeMax > currentMin - threshold) {
                currentLine.add(stroke)
                currentMin = min(currentMin, strokeMin)
                currentMax = max(currentMax, strokeMax)
            } else {
                lines.add(currentLine)
                currentLine = mutableListOf(stroke)
                currentMin = strokeMin
                currentMax = strokeMax
            }
        }
        lines.add(currentLine)
        return lines
    }

    /**
     * Fit baseline by least-squares regression over stroke anchor points.
     * Returns [slope, intercept, centroidX, centroidY].
     */
    private fun fitBaseline(strokes: List<Stroke>): FloatArray {
        val anchors = strokes.mapNotNull { stroke ->
            if (stroke.points.isEmpty()) null
            else {
                val x = stroke.points.map { it[0] }.average().toFloat()
                val y = stroke.points.map { it[1] }.average().toFloat()
                x to y
            }
        }

        if (anchors.isEmpty()) return floatArrayOf(0f, 0f, 0f, 0f)

        val n = anchors.size
        val sumX = anchors.sumOf { it.first.toDouble() }
        val sumY = anchors.sumOf { it.second.toDouble() }
        val sumXY = anchors.sumOf { it.first.toDouble() * it.second.toDouble() }
        val sumXX = anchors.sumOf { it.first.toDouble() * it.first.toDouble() }

        val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        val cx = (sumX / n).toFloat()
        val cy = (sumY / n).toFloat()

        return floatArrayOf(slope.toFloat(), intercept.toFloat(), cx, cy)
    }

    /**
     * Rotate strokes about centroid by given angle.
     */
    private fun rotateStrokes(strokes: List<Stroke>, angle: Float, cx: Float, cy: Float): List<Stroke> {
        val cos = cos(angle)
        val sin = sin(angle)

        return strokes.map { stroke ->
            val rotatedPoints = stroke.points.map { point ->
                val x = point[0] - cx
                val y = point[1] - cy
                floatArrayOf(
                    x * cos - y * sin + cx,
                    x * sin + y * cos + cy,
                    point[2], point[3], point[4], point[5]
                )
            }
            stroke.copy(points = rotatedPoints)
        }
    }

    /**
     * Ramer–Douglas–Peucker simplification.
     */
    private fun rdpSimplify(points: List<FloatArray>, epsilon: Float): List<FloatArray> {
        if (points.size < 3) return points

        // Find the point with maximum distance from line between first and last
        var maxDist = 0f
        var maxIdx = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIdx = i
            }
        }

        return if (maxDist > epsilon) {
            val left = rdpSimplify(points.subList(0, maxIdx + 1), epsilon)
            val right = rdpSimplify(points.subList(maxIdx, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(point: FloatArray, lineStart: FloatArray, lineEnd: FloatArray): Float {
        val dx = lineEnd[0] - lineStart[0]
        val dy = lineEnd[1] - lineStart[1]
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0f) return sqrt((point[0] - lineStart[0]).pow(2) + (point[1] - lineStart[1]).pow(2))
        val t = ((point[0] - lineStart[0]) * dx + (point[1] - lineStart[1]) * dy) / (len * len)
        val projX = lineStart[0] + t * dx
        val projY = lineStart[1] + t * dy
        return sqrt((point[0] - projX).pow(2) + (point[1] - projY).pow(2))
    }

    /**
     * Catmull-Rom spline interpolation for smoothing.
     */
    private fun catmullRom(points: List<FloatArray>, subdivisions: Int): List<FloatArray> {
        if (points.size < 4) return points

        val result = mutableListOf<FloatArray>()
        result.add(points.first())

        for (i in 1 until points.size - 2) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[i + 2]

            for (j in 1..subdivisions) {
                val t = j.toFloat() / subdivisions
                val t2 = t * t
                val t3 = t2 * t

                val x = 0.5f * (
                    2 * p1[0] +
                    (-p0[0] + p2[0]) * t +
                    (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 +
                    (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3
                )
                val y = 0.5f * (
                    2 * p1[1] +
                    (-p0[1] + p2[1]) * t +
                    (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 +
                    (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3
                )

                result.add(floatArrayOf(x, y, p1[2], p1[3], p1[4], p1[5]))
            }
        }

        result.add(points.last())
        return result
    }

    /**
     * X-height normalization: scale strokes to match median line height.
     */
    fun normalizeHeight(strokes: List<Stroke>, targetHeight: Float = 50f): List<Stroke> {
        if (strokes.isEmpty()) return strokes

        val heights = strokes.mapNotNull { stroke ->
            val ys = stroke.points.map { it[1] }
            if (ys.isEmpty()) null else ys.max() - ys.min()
        }.sorted()

        if (heights.isEmpty()) return strokes

        val medianHeight = heights[heights.size / 2]
        if (medianHeight == 0f) return strokes

        val scale = targetHeight / medianHeight
        return strokes.map { stroke ->
            val scaledPoints = stroke.points.map { point ->
                floatArrayOf(point[0], point[1] * scale, point[2], point[3], point[4], point[5])
            }
            stroke.copy(points = scaledPoints)
        }
    }
}
