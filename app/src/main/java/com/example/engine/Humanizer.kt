package com.example.engine

import android.graphics.Path
import android.graphics.PointF
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object Humanizer {
    private val random = Random(System.currentTimeMillis())

    /**
     * Applies subtle 2D Gaussian-like jitter to coordinates, mimicking human finger landing variation.
     */
    fun randomizePoint(x: Float, y: Float, radius: Int = 12): PointF {
        if (radius <= 0) return PointF(x, y)
        // Approximate normal distribution using Box-Muller-like average of uniforms
        val offsetFactorX = (random.nextFloat() + random.nextFloat() - 1f) * radius
        val offsetFactorY = (random.nextFloat() + random.nextFloat() - 1f) * radius
        return PointF(max(0f, x + offsetFactorX), max(0f, y + offsetFactorY))
    }

    /**
     * Human touch duration usually ranges between 60ms and 130ms with slight variance.
     */
    fun randomizeTapDuration(baseDuration: Long = 85L, variancePercent: Int = 15): Long {
        val variance = (baseDuration * (variancePercent / 100f)).toLong()
        val delta = if (variance > 0) random.nextLong(-variance, variance + 1) else 0
        return max(45L, baseDuration + delta)
    }

    /**
     * Randomizes delay between actions to avoid predictable robot intervals.
     */
    fun randomizeDelay(baseDelay: Long, variancePercent: Int = 20): Long {
        if (baseDelay <= 0) return 0L
        val variance = (baseDelay * (variancePercent / 100f)).toLong()
        val delta = if (variance > 0) random.nextLong(-variance, variance + 1) else 0
        return max(20L, baseDelay + delta)
    }

    /**
     * Creates a realistic curved gesture Path with a slight arc (Bézier curve),
     * rather than a perfectly artificial straight line.
     */
    fun createHumanizedSwipePath(startX: Float, startY: Float, endX: Float, endY: Float): Path {
        val path = Path()
        val start = randomizePoint(startX, startY, 6)
        val end = randomizePoint(endX, endY, 6)
        path.moveTo(start.x, start.y)

        // Midpoint with small perpendicular curve deflection
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f
        val dx = end.x - start.x
        val dy = end.y - start.y

        // Random lateral deviation (arc)
        val arcDeviation = (random.nextFloat() - 0.5f) * 40f
        val controlX = midX - (dy / 50f) * arcDeviation
        val controlY = midY + (dx / 50f) * arcDeviation

        path.quadTo(controlX, controlY, end.x, end.y)
        return path
    }
}
