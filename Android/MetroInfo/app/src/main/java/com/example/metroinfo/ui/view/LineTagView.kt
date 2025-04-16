package com.example.metroinfo.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.metroinfo.util.LineColors
import kotlin.math.min

class LineTagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val textBounds = Rect()
    private var lineNumber: Int = 1
    private var cornerRadius = 0f

    init {
        paint.textAlign = Paint.Align.LEFT
    }

    fun setLineNumber(number: Int) {
        lineNumber = number
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 200
        val desiredHeight = 80

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
        cornerRadius = height / 4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // 绘制背景
        paint.color = LineColors.getLineColor(lineNumber)
        rect.set(0f, 0f, width, height)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // 绘制线路号码
        paint.color = android.graphics.Color.WHITE
        paint.textSize = height * 0.6f
        val numberText = lineNumber.toString()
        paint.getTextBounds(numberText, 0, numberText.length, textBounds)
        val numberX = height * 0.2f
        val numberY = (height + textBounds.height()) / 2f

        canvas.drawText(numberText, numberX, numberY, paint)

        // 绘制"号线"文字
        paint.textSize = height * 0.35f
        val lineText = "号线"
        paint.getTextBounds(lineText, 0, lineText.length, textBounds)
        val lineX = numberX + paint.measureText(numberText) + height * 0.1f
        val lineY = (height + textBounds.height()) / 2f

        canvas.drawText(lineText, lineX, lineY, paint)

        // 绘制英文 "Line X"
        val englishText = "Line $lineNumber"
        paint.textSize = height * 0.25f
        paint.getTextBounds(englishText, 0, englishText.length, textBounds)
        val englishX = lineX
        val englishY = height * 0.85f

        canvas.drawText(englishText, englishX, englishY, paint)
    }
} 