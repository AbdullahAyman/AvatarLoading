package tps.com.avatarpro.viewComponents

import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import tps.com.avatarpro.R

class AvatarView : ImageView {

    /**
     * Progress values
     */
    var max = 100f
    var progress = 0f
        set(progress) {
            if (progress < 0) {
                field = 0f
            } else if (progress > 100) {
                field = 100f
            } else {
                field = progress
            }
            invalidate()
        }

    var currentProgress = 0f

    /**
     * Progress ring sizes
     */
    var backgroundRingSize = 40f
    var progressRingSize = backgroundRingSize
    var isProgressRingOutline = false

    /**
     * Default progress colors
     */
    var backgroundRingColor = DEFAULT_BG_COLOR
    var progressRingColor = DEFAULT_RING_COLOR
    var progressGradient: IntArray? = null
    var isJoinGradient: Boolean = false
    var gradientFactor: Float = 0.toFloat()

    /**
     * Default progress ring cap
     */
    var progressRingCorner: Paint.Cap = Paint.Cap.BUTT
        private set

    /*
     * Animator
     */
    /* *************************
     * GETTERS & SETTERS
     * *************************/

    /**
     * Get an instance of the current [ObjectAnimator]
     * <br></br>You can e.g. add Listeners to it
     *
     * @return [ObjectAnimator]
     */
    var animator: ObjectAnimator? = null
        private set

    /*
     * Default interpolator
     */
    private val mDefaultInterpolator = OvershootInterpolator()

    /*
     * Default sizes
     */
    private var mViewHeight = 0
    private var mViewWidth = 0

    /*
     * Default padding
     */
    private var mPaddingTop: Int = 0
    private var mPaddingBottom: Int = 0
    private var mPaddingLeft: Int = 0
    private var mPaddingRight: Int = 0

    /*
     * Paints
     */
    private var mProgressRingPaint: Paint? = null
    private var mBackgroundRingPaint: Paint? = null

    /*
     * Bounds of the ring
     */
    private var mRingBounds: RectF? = null
    private var mOffsetRingSize: Float = 0.toFloat()

    /*
     * Masks for clipping the current drawable in a circle
     */
    private var mMaskPaint: Paint? = null
    private var mOriginalBitmap: Bitmap? = null
    private var mCacheCanvas: Canvas? = null

    private val sweepAngle: Float
        get() = 360f / max * progress

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
            context: Context, attrs: AttributeSet?, defStyleAttr: Int,
            defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Parse attributes
     *
     * @param attrs        AttributeSet
     * @param defStyleAttr int
     * @param defStyleRes  int
     */
    private fun init(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.AvatarView, defStyleAttr, defStyleRes
        )

        max = a.getFloat(
                R.styleable.AvatarView_max, max
        )
        progress = a.getFloat(
                R.styleable.AvatarView_progress, progress
        )
        if (!a.hasValue(R.styleable.AvatarView_backgroundRingSize)) {
            if (a.hasValue(R.styleable.AvatarView_progressRingSize)) {
                progressRingSize = a.getDimension(
                        R.styleable.AvatarView_progressRingSize, progressRingSize
                )
                backgroundRingSize = progressRingSize
            }
        } else {
            backgroundRingSize = a.getDimension(
                    R.styleable.AvatarView_backgroundRingSize, backgroundRingSize
            )
            progressRingSize = a.getDimension(
                    R.styleable.AvatarView_progressRingSize, progressRingSize
            )
        }
        isProgressRingOutline = a.getBoolean(R.styleable.AvatarView_progressRingOutline, false)
        backgroundRingColor = a.getColor(
                R.styleable.AvatarView_backgroundRingColor, backgroundRingColor
        )
        progressRingColor = a.getColor(
                R.styleable.AvatarView_progressRingColor, DEFAULT_RING_COLOR
        )

        try {
            if (a.hasValue(R.styleable.AvatarView_progressGradient)) {
                val gradient: IntArray
                var i = -1
                try {
                    val resourceId = a
                            .getResourceId(R.styleable.AvatarView_progressGradient, 0)
                    if (isInEditMode) {
                        val gradientRes = resources.getStringArray(resourceId)
                        gradient = IntArray(gradientRes.size)
                        i = 0
                        for (color in gradientRes) {
                            gradient[i] = Color.parseColor(color)
                            i++
                        }
                    } else {
                        if (a.resources.getResourceTypeName(resourceId) != "array") {
                            throw IllegalArgumentException("Resource is not an array")
                        }
                        val ta = a.resources.obtainTypedArray(resourceId)
                        val len = ta.length()
                        gradient = IntArray(len)
                        i = 0
                        for (c in 0 until len) {
                            val colorString = ta.getString(c)
                            if (colorString != null) {
                                gradient[i] = Color.parseColor(colorString)
                                i++
                            } else {
                                throw IllegalArgumentException()
                            }
                        }
                        ta.recycle()
                    }
                } catch (e: IllegalArgumentException) {
                    if (i == -1) {
                        throw e
                    }
                    throw IllegalArgumentException("Unknown Color at position $i")
                }

                progressGradient = gradient

                isJoinGradient = a.getBoolean(R.styleable.AvatarView_joinGradient, false)

                gradientFactor = a.getFloat(R.styleable.AvatarView_gradientFactor, 1f)
            }
        } catch (e: Exception) {
            if (!isInEditMode) {
                throw e
            }
        }

        setProgressRingCorner(
                a.getInt(
                        R.styleable.AvatarView_progressRingCorner, Paint.Cap.BUTT.ordinal
                )
        )

        a.recycle()

        setupAnimator()
    }

    /**
     * Measure to square the view
     *
     * @param widthMeasureSpec  int
     * @param heightMeasureSpec int
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Process complexity measurements
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Squared size
        val size: Int

        // Get getMeasuredWidth() and getMeasuredHeight().
        val width = measuredWidth
        val height = measuredHeight

        // Remove padding to avoid bad size ratio calculation
        val widthWithoutPadding = width - paddingLeft - paddingRight
        val heightWithoutPadding = height - paddingTop - paddingBottom

        // Depending on the size ratio, calculate the final size without padding
        if (widthWithoutPadding > heightWithoutPadding) {
            size = heightWithoutPadding
        } else {
            size = widthWithoutPadding
        }

        // Report back the measured size.
        // Add pending padding
        setMeasuredDimension(
                size + paddingLeft + paddingRight,
                size + paddingTop + paddingBottom
        )
    }

    /**
     * This method is called after measuring the dimensions of MATCH_PARENT and WRAP_CONTENT Save
     * these dimensions to setup the bounds and paints
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Save current view dimensions
        mViewWidth = w
        mViewHeight = h

        // Apply ring as outline
        if (isProgressRingOutline) {
            setPadding(
                    java.lang.Float.valueOf(backgroundRingSize + paddingLeft).toInt(),
                    java.lang.Float.valueOf(backgroundRingSize + paddingTop).toInt(),
                    java.lang.Float.valueOf(backgroundRingSize + paddingRight).toInt(),
                    java.lang.Float.valueOf(backgroundRingSize + paddingBottom).toInt()
            )
        }

        setupBounds()
        setupBackgroundRingPaint()
        setupProgressRingPaint()

        requestLayout()
        invalidate()
    }

    /**
     * Set the common bounds of the rings
     */
    private fun setupBounds() {
        // Min value for squared size
        val minValue = Math.min(mViewWidth, mViewHeight)

        // Calculate the Offset if needed
        val xOffset = mViewWidth - minValue
        val yOffset = mViewHeight - minValue

        // Apply ring as outline
        var outline = 0
        if (isProgressRingOutline) {
            outline = java.lang.Float.valueOf(-backgroundRingSize).toInt()
        }

        // Save padding plus offset
        mPaddingTop = outline + this.paddingTop + yOffset / 2
        mPaddingBottom = outline + this.paddingBottom + yOffset / 2
        mPaddingLeft = outline + this.paddingLeft + xOffset / 2
        mPaddingRight = outline + this.paddingRight + xOffset / 2

        // Bigger ring size
        val biggerRingSize = if (backgroundRingSize > progressRingSize)
            backgroundRingSize
        else
            progressRingSize

        // Save the half of the progress ring
        mOffsetRingSize = biggerRingSize / 2

        val width = width
        val height = height

        // Create the ring bounds Rect
        mRingBounds = RectF(
                mPaddingLeft + mOffsetRingSize,
                mPaddingTop + mOffsetRingSize,
                width.toFloat() - mPaddingRight.toFloat() - mOffsetRingSize,
                height.toFloat() - mPaddingBottom.toFloat() - mOffsetRingSize
        )
    }

    private fun setupMask() {
        mOriginalBitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888
        )
        val shader = BitmapShader(
                mOriginalBitmap!!,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
        )
        mMaskPaint = Paint()
        mMaskPaint!!.isAntiAlias = true
        mMaskPaint!!.shader = shader
    }

    private fun setupProgressRingPaint() {
        mProgressRingPaint = Paint()
        mProgressRingPaint!!.isAntiAlias = true
        mProgressRingPaint!!.strokeCap = progressRingCorner
        mProgressRingPaint!!.style = Paint.Style.STROKE
        mProgressRingPaint!!.strokeWidth = progressRingSize
        mProgressRingPaint!!.color = progressRingColor

        if (progressGradient != null) {
            var colors: IntArray = progressGradient!!
            val positions: FloatArray
            if (isJoinGradient) {
                colors = IntArray(progressGradient!!.size + 1)
                positions = FloatArray(colors.size)
                var i = 0
                positions[i] = i.toFloat()
                for (color in progressGradient!!) {
                    colors[i] = color
                    if (i == progressGradient!!.size - 1) {
                        positions[i] = (ANGLE_360 - progressRingSize * gradientFactor) / ANGLE_360
                    } else if (i > 0) {
                        positions[i] = i.toFloat() / colors.size.toFloat()
                    }
                    i++
                }
                colors[i] = colors[0]
                positions[i] = 1f
            }

            val gradient = SweepGradient(
                    mRingBounds!!.centerX(),
                    mRingBounds!!.centerY(),
                    colors, null
            )

            mProgressRingPaint!!.shader = gradient
            val matrix = Matrix()
            mProgressRingPaint!!.shader.setLocalMatrix(matrix)
            matrix.postTranslate(-mRingBounds!!.centerX(), -mRingBounds!!.centerY())
            matrix.postRotate((-ANGLE_90).toFloat())
            matrix.postTranslate(mRingBounds!!.centerX(), mRingBounds!!.centerY())
            mProgressRingPaint!!.shader.setLocalMatrix(matrix)
            mProgressRingPaint!!.color = progressGradient!![0]
        }
    }

    private fun setupBackgroundRingPaint() {
        mBackgroundRingPaint = Paint()
        mBackgroundRingPaint!!.color = backgroundRingColor
        mBackgroundRingPaint!!.isAntiAlias = true
        mBackgroundRingPaint!!.style = Paint.Style.STROKE
        mBackgroundRingPaint!!.strokeWidth = backgroundRingSize
    }

    private fun setupAnimator() {
        animator = ObjectAnimator.ofFloat(
                this, "progress", this.progress, this.progress
        )
        animator!!.duration = ANIMATION_DURATION.toLong()
        animator!!.interpolator = mDefaultInterpolator
        animator!!.startDelay = ANIMATION_DELAY.toLong()
        animator!!.addUpdateListener { animation ->
            currentProgress = animation.animatedValue as Float
            progress = currentProgress
        }
    }

    /**
     * It will start animating the progress ring to the progress value set
     * <br></br>Default animation duration is 1200 milliseconds
     * <br></br>It starts with a default delay of 500 milliseconds
     * <br></br>YmProgressRingCornerou can get an instance of the animator with the method [ ][AvatarView.getAnimator] and Override these values
     *
     * @see ObjectAnimator
     */
    fun startAnimation() {
        val finalProgress = this.progress
        this.progress = this.currentProgress
        animator!!.setFloatValues(this.currentProgress, finalProgress)
        animator!!.start()
    }

    override fun onDraw(canvas: Canvas) {
        // Setup the mask at first
        if (mMaskPaint == null) {
            setupMask()
        }

        // Cache the canvas
        if (mCacheCanvas == null) {
            mCacheCanvas = Canvas(mOriginalBitmap!!)
        }

        // ImageView
        super.onDraw(mCacheCanvas)

        // Crop ImageView resource to a circle
        canvas.drawCircle(
                mRingBounds!!.centerX(),
                mRingBounds!!.centerY(),
                mRingBounds!!.width() / 2 - backgroundRingSize / 2,
                mMaskPaint!!
        )

        // Draw the background ring
        if (backgroundRingSize > 0) {
            canvas.drawArc(mRingBounds!!, ANGLE_360.toFloat(), ANGLE_360.toFloat(), false, mBackgroundRingPaint!!)
        }
        // Draw the progress ring
        if (progressRingSize > 0) {
            canvas.drawArc(mRingBounds!!, (-ANGLE_90).toFloat(), sweepAngle, false, mProgressRingPaint!!)
        }
    }

    fun setProgressRingCorner(progressRingCorner: Int) {
        this.progressRingCorner = getCap(progressRingCorner)
    }

    private fun getCap(id: Int): Paint.Cap {
        for (value in Paint.Cap.values()) {
            if (id == value.ordinal) {
                return value
            }
        }
        return Paint.Cap.BUTT
    }

    companion object {

        private val ANIMATION_DURATION = 1200
        private val ANIMATION_DELAY = 100
        private val ANGLE_360 = 360
        private val ANGLE_90 = 90

        private val DEFAULT_BG_COLOR = -0x557c2f37
        private val DEFAULT_RING_COLOR = -0xff6978
    }
}