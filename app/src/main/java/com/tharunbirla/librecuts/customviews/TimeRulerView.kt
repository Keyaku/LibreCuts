package com.tharunbirla.librecuts.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Locale

class TimeRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var videoDurationMs: Long = 0L

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#49474F") // outlineVariant
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CAC4D0") // toolTextInactive
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CAC4D0") // toolTextInactive
        textSize = 24f // approx 10sp
        textAlign = Paint.Align.CENTER
    }

    init {
        // Convert 10sp to pixels for text size
        val density = resources.displayMetrics.density
        textPaint.textSize = 10f * density
    }

    fun setVideoDuration(durationMs: Long) {
        this.videoDurationMs = durationMs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (videoDurationMs <= 0 || width <= 0) return

        val durationSec = videoDurationMs / 1000f
        if (durationSec <= 0f) return

        val stepMs = when {
            durationSec <= 15 -> 2000L    // Every 2 seconds
            durationSec <= 30 -> 5000L    // Every 5 seconds
            durationSec <= 60 -> 10000L   // Every 10 seconds
            durationSec <= 120 -> 15000L  // Every 15 seconds
            durationSec <= 300 -> 30000L  // Every 30 seconds
            else -> 60000L                // Every 1 minute
        }

        val minorStepMs = stepMs / 5
        val msPerPixel = videoDurationMs.toFloat() / width

        // Draw top horizontal baseline
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, tickPaint)

        // Draw minor and major ticks
        var currentMs = 0L
        while (currentMs <= videoDurationMs) {
            val x = currentMs / msPerPixel

            if (currentMs % stepMs == 0L) {
                // Major tick
                canvas.drawLine(x, 0f, x, height * 0.4f, majorTickPaint)

                // Format time string (e.g., 00:02 or 01:30)
                val minutes = (currentMs / 60000).toInt()
                val seconds = ((currentMs % 60000) / 1000).toInt()
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                // Draw label below tick
                val textY = height * 0.85f
                canvas.drawText(timeStr, x, textY, textPaint)
            } else {
                // Minor tick
                canvas.drawLine(x, 0f, x, height * 0.2f, tickPaint)
            }

            currentMs += minorStepMs
        }
    }
}
