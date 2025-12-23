package com.example.presonalincome_expenditureaccountingsystem.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 饼图自定义 View
 * 
 * 显示分类占比的环形饼图
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 饼图数据
    data class PieData(
        val name: String,
        val value: Double,
        val color: Int,
        val percentage: Float = 0f
    )

    private val piePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        textAlign = Paint.Align.CENTER
        textSize = 48f
    }

    private val centerSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575")
        textAlign = Paint.Align.CENTER
        textSize = 32f
    }

    private val pieRect = RectF()
    
    private var dataList: List<PieData> = emptyList()
    private var animationProgress = 0f
    private var animator: ValueAnimator? = null
    
    // 环形宽度比例
    private val ringWidthRatio = 0.25f
    
    // 中心文字
    private var centerTitle = ""
    private var centerSubtitle = ""

    /**
     * 设置饼图数据
     */
    fun setData(data: List<PieData>, title: String = "", subtitle: String = "") {
        // 计算百分比
        val total = data.sumOf { it.value }
        dataList = if (total > 0) {
            data.map { it.copy(percentage = (it.value / total * 100).toFloat()) }
        } else {
            emptyList()
        }
        
        centerTitle = title
        centerSubtitle = subtitle
        
        // 启动动画
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
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

        if (dataList.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * 0.8f
        val innerRadius = radius * (1 - ringWidthRatio)

        pieRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 绘制饼图
        var startAngle = -90f
        for (data in dataList) {
            val sweepAngle = data.percentage / 100f * 360f * animationProgress
            
            piePaint.color = data.color
            canvas.drawArc(pieRect, startAngle, sweepAngle, true, piePaint)
            
            startAngle += data.percentage / 100f * 360f * animationProgress
        }

        // 绘制中心空白圆形（形成环形效果）
        piePaint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, innerRadius, piePaint)

        // 绘制中心文字
        if (centerTitle.isNotEmpty()) {
            centerTextPaint.textSize = radius * 0.25f
            canvas.drawText(centerTitle, centerX, centerY, centerTextPaint)
        }
        
        if (centerSubtitle.isNotEmpty()) {
            centerSubTextPaint.textSize = radius * 0.15f
            canvas.drawText(centerSubtitle, centerX, centerY + radius * 0.2f, centerSubTextPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * 0.6f

        // 绘制灰色圆环
        piePaint.color = Color.parseColor("#EEEEEE")
        piePaint.style = Paint.Style.STROKE
        piePaint.strokeWidth = radius * ringWidthRatio
        canvas.drawCircle(centerX, centerY, radius - piePaint.strokeWidth / 2, piePaint)
        piePaint.style = Paint.Style.FILL

        // 绘制中心文字
        centerSubTextPaint.textSize = 36f
        canvas.drawText("暂无数据", centerX, centerY, centerSubTextPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}

