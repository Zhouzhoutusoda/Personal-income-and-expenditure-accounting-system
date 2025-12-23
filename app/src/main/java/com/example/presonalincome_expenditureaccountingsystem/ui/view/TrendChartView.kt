package com.example.presonalincome_expenditureaccountingsystem.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * 趋势图自定义 View
 * 
 * 显示收支趋势的折线图
 */
class TrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 趋势数据
    data class TrendData(
        val label: String,      // X轴标签（日期）
        val expense: Double,    // 支出
        val income: Double      // 收入
    )

    private val expenseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val incomeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val expenseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val incomeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val expensePath = Path()
    private val expenseFillPath = Path()
    private val incomePath = Path()
    private val incomeFillPath = Path()

    private var dataList: List<TrendData> = emptyList()
    private var animationProgress = 0f
    private var animator: ValueAnimator? = null

    private val padding = 40f
    private val bottomPadding = 60f

    /**
     * 设置趋势数据
     */
    fun setData(data: List<TrendData>) {
        dataList = data
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataList.isEmpty() || dataList.size < 2) {
            drawEmptyState(canvas)
            return
        }

        val chartWidth = width - padding * 2
        val chartHeight = height - padding - bottomPadding

        // 计算最大值
        val maxValue = max(
            dataList.maxOfOrNull { max(it.expense, it.income) } ?: 0.0,
            1.0
        )

        // 绘制网格线
        drawGrid(canvas, chartHeight.toInt())

        // 计算点的位置
        val pointSpacing = chartWidth / (dataList.size - 1)
        
        // 清空路径
        expensePath.reset()
        expenseFillPath.reset()
        incomePath.reset()
        incomeFillPath.reset()

        // 计算并绘制折线
        dataList.forEachIndexed { index, data ->
            val x = padding + index * pointSpacing
            val expenseY = padding + chartHeight - (data.expense / maxValue * chartHeight).toFloat() * animationProgress
            val incomeY = padding + chartHeight - (data.income / maxValue * chartHeight).toFloat() * animationProgress

            if (index == 0) {
                expensePath.moveTo(x, expenseY)
                expenseFillPath.moveTo(x, padding + chartHeight)
                expenseFillPath.lineTo(x, expenseY)
                
                incomePath.moveTo(x, incomeY)
                incomeFillPath.moveTo(x, padding + chartHeight)
                incomeFillPath.lineTo(x, incomeY)
            } else {
                expensePath.lineTo(x, expenseY)
                expenseFillPath.lineTo(x, expenseY)
                
                incomePath.lineTo(x, incomeY)
                incomeFillPath.lineTo(x, incomeY)
            }

            if (index == dataList.size - 1) {
                expenseFillPath.lineTo(x, padding + chartHeight)
                expenseFillPath.close()
                
                incomeFillPath.lineTo(x, padding + chartHeight)
                incomeFillPath.close()
            }
        }

        // 绘制填充渐变
        expenseFillPaint.shader = LinearGradient(
            0f, padding, 0f, padding + chartHeight,
            Color.parseColor("#33F44336"),
            Color.parseColor("#00F44336"),
            Shader.TileMode.CLAMP
        )
        incomeFillPaint.shader = LinearGradient(
            0f, padding, 0f, padding + chartHeight,
            Color.parseColor("#334CAF50"),
            Color.parseColor("#004CAF50"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(expenseFillPath, expenseFillPaint)
        canvas.drawPath(incomeFillPath, incomeFillPaint)

        // 绘制折线
        canvas.drawPath(expensePath, expenseLinePaint)
        canvas.drawPath(incomePath, incomeLinePaint)

        // 绘制数据点
        dataList.forEachIndexed { index, data ->
            val x = padding + index * pointSpacing
            val expenseY = padding + chartHeight - (data.expense / maxValue * chartHeight).toFloat() * animationProgress
            val incomeY = padding + chartHeight - (data.income / maxValue * chartHeight).toFloat() * animationProgress

            // 支出点
            dotPaint.color = Color.WHITE
            canvas.drawCircle(x, expenseY, 8f, dotPaint)
            dotPaint.color = Color.parseColor("#F44336")
            canvas.drawCircle(x, expenseY, 5f, dotPaint)

            // 收入点
            dotPaint.color = Color.WHITE
            canvas.drawCircle(x, incomeY, 8f, dotPaint)
            dotPaint.color = Color.parseColor("#4CAF50")
            canvas.drawCircle(x, incomeY, 5f, dotPaint)
        }

        // 绘制X轴标签（只显示部分）
        val labelStep = if (dataList.size > 10) dataList.size / 5 else 1
        dataList.forEachIndexed { index, data ->
            if (index % labelStep == 0 || index == dataList.size - 1) {
                val x = padding + index * pointSpacing
                canvas.drawText(data.label, x, height - 20f, labelPaint)
            }
        }
    }

    private fun drawGrid(canvas: Canvas, chartHeight: Int) {
        val gridCount = 4
        val gridSpacing = chartHeight / gridCount.toFloat()

        for (i in 0..gridCount) {
            val y = padding + i * gridSpacing
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        labelPaint.textSize = 36f
        canvas.drawText("暂无趋势数据", width / 2f, height / 2f, labelPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}

