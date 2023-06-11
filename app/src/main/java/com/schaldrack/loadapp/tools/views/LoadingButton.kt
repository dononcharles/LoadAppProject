package com.schaldrack.loadapp.tools.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.schaldrack.loadapp.R
import com.schaldrack.loadapp.data.models.ButtonState
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var widthSize = 0
    private var heightSize = 0

    private var buttonText = ""
    private var buttonTextColor = 0
    private var buttonBackgroundColor = 0
    private var progress = 0f

    private var valueAnimator = ValueAnimator()

    private var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _, new ->
        when (new) {
            ButtonState.Loading -> {
                buttonBackgroundColor = ContextCompat.getColor(context, R.color.colorPrimary)
                setButtonName("Loading...")

                valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2000
                    addUpdateListener {
                        setProgress(it.animatedValue as Float)
                    }
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    disableViewWhileAnimation()
                    start()
                }
                isEnabled = false // avoid click while loading
            }

            ButtonState.Completed -> {
                backgroundPaint.color = ContextCompat.getColor(context, R.color.colorPrimary)
                isEnabled = true
                buttonBackgroundColor = ContextCompat.getColor(context, R.color.colorPrimary)
                setButtonName("Download")
                valueAnimator.cancel()
            }

            ButtonState.Clicked -> {
                println("ButtonState.Clicked")
            }
        }

        reDraw()
    }

    private fun ValueAnimator.disableViewWhileAnimation() {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                isEnabled = false
            }

            override fun onAnimationEnd(animation: Animator) {
                isEnabled = true
            }
        })
    }

    private fun reDraw() {
        // Force call onDraw() method to redraw the view
        invalidate()
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
    }

    private val backgroundColorWhenDownloading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
    }

    private val arcPaintWhenDownloading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
    }

    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        textSize = 55.0f
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }

    // Create instance of Rect, To Avoid object allocations during draw/layout operations
    private val textRect = Rect()

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            buttonText = getString(R.styleable.LoadingButton_buttonText).toString()
            buttonTextColor = getColor(R.styleable.LoadingButton_buttonTextColor, 0)
            buttonBackgroundColor = getColor(R.styleable.LoadingButton_buttonBackgroundColor, 0)
        }
    }

    override fun performClick(): Boolean {
        if (super.performClick()) return true
        reDraw()
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // get width and height of the view
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        canvas?.drawColor(buttonBackgroundColor)

        // draw the button text
        buttonTextPaint.getTextBounds(buttonText, 0, buttonText.length, textRect)

        // center X axis
        val centerX = width / 2
        val centerY = height / 2 - textRect.centerY()

        canvas?.drawRoundRect(0f, 0f, width, height, 0f, 0f, backgroundPaint)

        // Draw Arc when button state is loading
        if (buttonState == ButtonState.Loading) {
            val arcWidth = width * progress
            canvas?.drawRoundRect(0f, 0f, arcWidth, height, 0f, 0f, backgroundColorWhenDownloading)

            val padding = 20f
            val arcSize = 100f
            val spaceFromStart = width - textRect.width() - padding / 2

            canvas?.drawArc(
                spaceFromStart,
                padding,
                spaceFromStart + arcSize,
                height - padding,
                0f,
                360 * progress,
                true,
                arcPaintWhenDownloading,
            )
        }

        // Draw Button Text on Top of all canvas
        canvas?.drawText(buttonText, centerX, centerY, buttonTextPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minW: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minW, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(MeasureSpec.getSize(w), heightMeasureSpec, 0)
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

    private fun setButtonName(text: String) {
        buttonText = text
        reDraw()
    }

    fun setLoadingButtonState(state: ButtonState) {
        buttonState = state
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        reDraw()
    }
}
