package com.example.iot_test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * 简易柱状图（数据看板的“近 7 天答题量”）
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var labels: List<String> = emptyList()
    private var values: List<Int> = emptyList()

    private val density = resources.displayMetrics.scaledDensity

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B82F6")
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EDF2FF")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111827")
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun setData(labels: List<String>, values: List<Int>) {
        this.labels = labels
        this.values = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = values.size
        if (count == 0) return

        val w = width.toFloat()
        val h = height.toFloat()
        val bottomPad = 22f * density
        val topPad = 18f * density
        val chartH = h - bottomPad - topPad
        if (chartH <= 0f) return

        val maxValue = max(values.maxOrNull() ?: 0, 1)
        val slot = w / count
        val barWidth = slot * 0.56f
        val radius = barWidth / 2f

        for (i in 0 until count) {
            val left = i * slot + (slot - barWidth) / 2f
            val right = left + barWidth

            canvas.drawRoundRect(RectF(left, topPad, right, h - bottomPad), radius, radius, trackPaint)

            val value = values.getOrElse(i) { 0 }
            if (value > 0) {
                val barH = chartH * value / maxValue.toFloat()
                val top = h - bottomPad - barH
                canvas.drawRoundRect(RectF(left, top, right, h - bottomPad), radius, radius, barPaint)
                canvas.drawText(value.toString(), (left + right) / 2f, top - 5f * density, valuePaint)
            }

            val label = labels.getOrElse(i) { "" }
            canvas.drawText(label, (left + right) / 2f, h - 6f * density, labelPaint)
        }
    }
}
