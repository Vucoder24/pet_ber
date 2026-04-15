package com.nvv.petber.ui.mention

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class MentionSpan(
    val petId: String,
    val petName: String
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val oldColor = paint.color
        val oldFakeBold = paint.isFakeBoldText

        paint.color = 0xFF1DA1F2.toInt() // màu IG
        paint.isFakeBoldText = true

        canvas.drawText(text, start, end, x, y.toFloat(), paint)

        paint.color = oldColor
        paint.isFakeBoldText = oldFakeBold
    }
}