package com.tharunbirla.librecuts.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TrackTrimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var videoDurationMs: Long = 0L
    var startTimeMs: Long = 0L
    var endTimeMs: Long = 0L

    var onTrimChanged: ((Long, Long) -> Unit)? = null

    var trackColor: Int = Color.parseColor("#4285F4") // Default blue
    var trackLabel: String? = null
    var isSelectedTrack: Boolean = false
    var trackIcon: android.graphics.drawable.Drawable? = null
    var trackThumbnail: android.graphics.Bitmap? = null
    var isAudioTrack: Boolean = false
    var onTrackClicked: (() -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF") // Semi-transparent white
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private val rectF = RectF()
    private val thumbnailRectF = RectF()
    private val handleWidth = 24f

    private enum class DragTarget { NONE, LEFT, RIGHT, CENTER }
    private var dragTarget = DragTarget.NONE
    private var lastTouchX = 0f
    private var downTouchX = 0f

    fun setRange(videoDurationMs: Long, startTimeMs: Long, endTimeMs: Long) {
        this.videoDurationMs = videoDurationMs
        this.startTimeMs = startTimeMs.coerceIn(0L, videoDurationMs)
        this.endTimeMs = endTimeMs.coerceIn(this.startTimeMs, videoDurationMs)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (videoDurationMs <= 0 || width <= 0) return

        val msPerPixel = videoDurationMs.toFloat() / width
        val startX = startTimeMs / msPerPixel
        val endX = endTimeMs / msPerPixel

        rectF.set(startX, 0f, endX, height.toFloat())
        
        // Draw track fill
        trackPaint.color = trackColor
        trackPaint.alpha = 200
        canvas.drawRoundRect(rectF, 12f, 12f, trackPaint)

        // Draw Audio Wave Background
        if (isAudioTrack) {
            canvas.save()
            canvas.clipRect(rectF)
            val centerY = height / 2f
            val maxAmplitude = height * 0.35f
            var x = startX + handleWidth + 4f
            val waveSpacing = 12f
            var timeOffset = 0f
            while (x < endX - handleWidth) {
                // Procedural wave using sine and pseudo-random
                val amplitude = maxAmplitude * (0.3f + 0.7f * Math.abs(Math.sin((x + timeOffset) * 0.05).toFloat()))
                canvas.drawLine(x, centerY - amplitude, x, centerY + amplitude, wavePaint)
                x += waveSpacing
                timeOffset += 1f
            }
            canvas.restore()
        }
        
        // Draw track border
        if (isSelectedTrack) {
            borderPaint.color = Color.WHITE
            borderPaint.strokeWidth = 6f
        } else {
            borderPaint.color = Color.parseColor("#88FFFFFF")
            borderPaint.strokeWidth = 3f
        }
        canvas.drawRoundRect(rectF, 12f, 12f, borderPaint)

        // Draw Icon, Thumbnail, and Label if available
        var textStartX = startX + handleWidth + 16f
        val iconSize = 36
        val iconTop = (height - iconSize) / 2

        if (trackIcon != null) {
            trackIcon?.setBounds(textStartX.toInt(), iconTop, (textStartX + iconSize).toInt(), iconTop + iconSize)
            trackIcon?.setTint(Color.WHITE)
            trackIcon?.draw(canvas)
            textStartX += iconSize + 12f
        }

        if (trackThumbnail != null) {
            val thumbWidth = (iconSize * 1.5f)
            thumbnailRectF.set(textStartX, iconTop.toFloat(), textStartX + thumbWidth, iconTop.toFloat() + iconSize)
            canvas.drawBitmap(trackThumbnail!!, null, thumbnailRectF, trackPaint)
            textStartX += thumbWidth + 12f
        }

        if (trackLabel != null) {
            val padding = 16f
            val maxTextWidth = (endX - startX) - (textStartX - startX) - handleWidth - padding
            if (maxTextWidth > 20f) {
                val textPaintObj = android.text.TextPaint(textPaint)
                val textToDraw = android.text.TextUtils.ellipsize(
                    trackLabel,
                    textPaintObj,
                    maxTextWidth,
                    android.text.TextUtils.TruncateAt.END
                ).toString()
                
                val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(textToDraw, textStartX, textY, textPaint)
            }
        }

        // Draw left handle
        canvas.drawRect(startX, 0f, startX + handleWidth, height.toFloat(), handlePaint)
        // Draw right handle
        canvas.drawRect(endX - handleWidth, 0f, endX, height.toFloat(), handlePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (videoDurationMs <= 0) return false

        val msPerPixel = videoDurationMs.toFloat() / width
        val startX = startTimeMs / msPerPixel
        val endX = endTimeMs / msPerPixel

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                downTouchX = event.x
                dragTarget = when {
                    event.x >= startX - 30f && event.x <= startX + handleWidth + 30f -> DragTarget.LEFT
                    event.x >= endX - handleWidth - 30f && event.x <= endX + 30f -> DragTarget.RIGHT
                    event.x > startX + handleWidth && event.x < endX - handleWidth -> DragTarget.CENTER
                    else -> DragTarget.NONE
                }
                if (dragTarget != DragTarget.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                return dragTarget != DragTarget.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dtMs = (dx * msPerPixel).toLong()

                when (dragTarget) {
                    DragTarget.LEFT -> {
                        startTimeMs = (startTimeMs + dtMs).coerceIn(0L, endTimeMs - 100L) // 100ms min duration
                    }
                    DragTarget.RIGHT -> {
                        endTimeMs = (endTimeMs + dtMs).coerceIn(startTimeMs + 100L, videoDurationMs)
                    }
                    DragTarget.CENTER -> {
                        val duration = endTimeMs - startTimeMs
                        startTimeMs = (startTimeMs + dtMs).coerceIn(0L, videoDurationMs - duration)
                        endTimeMs = startTimeMs + duration
                    }
                    DragTarget.NONE -> {}
                }
                
                lastTouchX = event.x
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragTarget != DragTarget.NONE) {
                    val wasDrag = Math.abs(event.x - downTouchX) > 10f
                    if (!wasDrag && dragTarget == DragTarget.CENTER && event.action == MotionEvent.ACTION_UP) {
                        onTrackClicked?.invoke()
                    } else if (wasDrag) {
                        onTrimChanged?.invoke(startTimeMs, endTimeMs)
                    }
                    dragTarget = DragTarget.NONE
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
