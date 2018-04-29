package com.sunqi.test.cm.customsurfaceviewdemo.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.sunqi.test.cm.customsurfaceviewdemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yzs on 2017/7/13.
 * 圆形进度控件，在SurfaceView中绘制
 */

public class MultiCircleProgressView extends SurfaceView implements SurfaceHolder.Callback  {

    // ================ 公共数据 ============= //
    /** 顶部作为计数起点, 右边是0，左边是-180或者180，底部是90 */
    private static final int START_POINT_TOP = -90;
    private static final int MAX_PROGRESS_DEFAULT = 100;
    private static final int MIN_PROGRESS_DEFAULT = 1;
    private static final int PERCENT_BASE = 100;
    private static final float TOTAL_ANGLE = 360f;

    /** 圆环进度的颜色 */
    private int mRoundProgressColor = 0xff04d3ff;
    /** 圆心的x坐标 */
    float mCenterX = 0;
    /** 圆心的y坐标 */
    float mCenterY = 0;
    /** 定义画笔 */
    private Paint mProgressPaint;
    /** 定义监听事件列表 */
    private List<IProgressStateChangeListener> mProgressStateChangeListeners;
    /** SurfaceView的绘制线程 */
    private DrawThread mDrawThread;
    private SurfaceHolder mSurfaceHolder;
    private DisplayMetrics mMetrics;
    /** 最大帧数 (1000 / 20) */
    private static final int DRAW_INTERVAL = 20;
    private static final int CLEAR_COLOR = 0xff0583f7;
    private float mProgressCenterY;
    /** 文本的边界1 */
    private Rect mTempTextBoundsOne = new Rect();
    /** 文本的边界2 */
    private Rect mTempTextBoundsTwo = new Rect();

    public MultiCircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mProgressStateChangeListeners = new ArrayList<IProgressStateChangeListener>();
        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mMetrics = getResources().getDisplayMetrics();
        mProgressCenterY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, mMetrics);
        mLogoMarginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mMetrics);
        mLogoMarginTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mMetrics);
        mLogoSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, mMetrics);
        mTitleLeftX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 47, mMetrics);
        mTitleCenterY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, mMetrics);
        mTitleTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, mMetrics);
        mOuterRoundWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, mMetrics);
        mInnerRoundWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, mMetrics);
        mOuterRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, mMetrics);
        mInnerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, mMetrics);
        mSpaceTextAndSign = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, mMetrics);
        mOuterHeadCircleWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, mMetrics);
        mPercentTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 45, mMetrics);
        mPercentSignSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, mMetrics);
        mTxtCourseSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, mMetrics);
        mCoursePosY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 355, mMetrics);
        mResultTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, mMetrics);
        mSubResultTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, mMetrics);
        mResultPosY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 382, mMetrics);
        mSubResultPosY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 415, mMetrics);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mTitleText = getResources().getString(R.string.app_name);
    }


    /** 剩下需要快速完成的角度 */
    private float mNeedQuickCompleteAngle = 0;
    /** 开始快速完成时的角度起始值 */
    private float mQuickStartAngle = 0;
    /** 是否需要快速完成 */
    private boolean isNeedCompleteQuickly = false;
    public void completeQuickly() {
        smoothScrollToAngle(TOTAL_ANGLE);
    }

    private float mSmoothScrollTotalAngle = 0;
    /**
     * 平滑旋转到指定的角度（0~360度）
     * @param angle 0~360度
     */
    private void smoothScrollToAngle(float angle) {
        isNeedCompleteQuickly = true;
        mQuickStartAngle = 0;
        mSmoothScrollTotalAngle = angle;
    }

    private int mSmoothScrollToProgress = 0;
    /**
     * 平滑旋转到指定的进度（0~100）
     * @param progress
     */
    public void smoothScrollToProgress(int progress) {
        if (progress <= MIN_PROGRESS_DEFAULT || progress >= MAX_PROGRESS_DEFAULT) {
            return;
        }
        mSmoothScrollToProgress = progress;
        smoothScrollToAngle(progress * TOTAL_ANGLE / PERCENT_BASE);
    }

    private void start() {
        if (mDrawThread == null) {
            mDrawThread = new DrawThread(mSurfaceHolder, getContext());
        }
        try {
            if (!mDrawThread.isRunning) {
                mDrawThread.isRunning = true;
                mDrawThread.start();
            }
        } catch (Exception ignore) {}
    }

    private void stop() {
        mDrawThread.isRunning = false;
        try {
            mDrawThread.join();
            mDrawThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
        isNeedCompleteQuickly = false;
    }

    class DrawThread extends Thread {
        SurfaceHolder surfaceHolder;
        Context context;
        boolean isRunning;

        public DrawThread(SurfaceHolder surfaceHolder, Context context) {
            this.surfaceHolder = surfaceHolder;
            this.context = context;
        }

        @Override
        public void run() {
            long timeStartPerDraw;
            long deltaTime;
            while (isRunning) {
                Canvas canvas = null;
                timeStartPerDraw = System.currentTimeMillis();
                try {
                    synchronized (surfaceHolder) {
                        Surface surface = surfaceHolder.getSurface();
                        if (surface != null && surface.isValid()) {
                            canvas = surfaceHolder.lockCanvas(null);
                        }
                        if (canvas != null) {
                            doDraw(canvas);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (surfaceHolder != null && canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        } catch (Exception ignore) {}
                    }
                }
                deltaTime = System.currentTimeMillis() - timeStartPerDraw;
                if (deltaTime < DRAW_INTERVAL) {
                    try {
                        // 控制帧数
                        Thread.sleep(DRAW_INTERVAL - deltaTime);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private long mTimerStartMillis = 0;
    private long mDeltaTime = 0;
    private float mStep1 = 180f / 10000f; // 前10秒每秒走5% 即18度
    private float mStep2 = 175f / 20000f; // 10秒到30秒走175度
    private void calculateAngle() {
        if (mTimerStartMillis == 0) {
            mTimerStartMillis = System.currentTimeMillis();
        }
        if (isNeedCompleteQuickly) {
            if (mQuickStartAngle == 0) {
                mTimerStartMillis = System.currentTimeMillis();
                mQuickStartAngle = getAngle();
                mNeedQuickCompleteAngle = mSmoothScrollTotalAngle - mQuickStartAngle;
                if (mNeedQuickCompleteAngle < 0) {
                    mNeedQuickCompleteAngle = 0;
                }
            }
            mDeltaTime = System.currentTimeMillis() - mTimerStartMillis;
            if (mDeltaTime <= 2000) {    // 快速结束时2秒走完剩下的
                setAngle(mQuickStartAngle + mNeedQuickCompleteAngle / 2000f * mDeltaTime);
            } else {
                setAngle(mSmoothScrollTotalAngle);
            }
        } else {
            mDeltaTime = System.currentTimeMillis() - mTimerStartMillis;
            if (mDeltaTime <= 10000) {   // 前10秒每秒走5% 即18度，总共走掉180度
                setAngle(mStep1 * mDeltaTime);
            } else if (mDeltaTime <= 30000f) { // 10秒到30秒走175度
                setAngle(180f + (mStep2 * (mDeltaTime - 10000)));
            }
        }
    }

    private long mProgressAlphaTimeStart = 0;
    private long mProgressDeltaTime = 0;
    private float mProgressAlphaStep = 255f / 500f;
    private boolean isProgressHideAnim = false;

    private void calculateProgressAnimData() {
        if (!isProgressHideAnim) {
            return;
        }
        if (mProgressAlphaTimeStart == 0) {
            mProgressAlphaTimeStart = System.currentTimeMillis();
        }
        mProgressDeltaTime = System.currentTimeMillis() - mProgressAlphaTimeStart;
        if (mProgressDeltaTime > 500) {
            mProgressAlpha = 0;
            isProgressHideAnim = false;
            for (IProgressStateChangeListener listener : mProgressStateChangeListeners) {
                if (listener != null) {
                    listener.onFinished();
                }
            }
            return;
        }
        mProgressAlpha = 255 - (int) (mProgressAlphaStep * mProgressDeltaTime);
    }

    private void doDraw(Canvas canvas) {
        calculatePreValue();
        canvas.drawColor(CLEAR_COLOR);
        drawTopLogoAndTitle(canvas);
        drawProgressPart(canvas);
        drawCurTextCourse(canvas);
        drawResultText(canvas);
        drawBottomTipText(canvas);
    }

    private boolean isProgressVisible = true;
    private int mProgressAlpha = 255;
    private void drawProgressPart(Canvas canvas) {
        if (!isProgressVisible) {
            return;
        }
        calculateProgressAnimData();
        drawOuterGradientProgress(canvas);
        calculateAngle();
        drawInnerProgress(canvas);
        drawPercentText(canvas);
    }

    private void calculatePreValue() {
        if (mCenterX == 0) {
            mCenterX = getWidth() / 2;                         // 获取圆心的x坐标
        }
        if (mCenterY == 0) {
            mCenterY = mProgressCenterY;
        }
        if (mInnerArcLimitRect.isEmpty()) {
            mInnerArcLimitRect.set(mCenterX - mInnerRadius, mCenterY - mInnerRadius, mCenterX + mInnerRadius, mCenterY + mInnerRadius);
        }
    }


    // ================ 顶部标题栏数据 ============= //
    private float mLogoMarginLeft = 0;
    private float mLogoMarginTop = 0;
    private float mLogoSize = 0;
    private float mTitleCenterY = 0;
    private float mTitleLeftX = 0;
    private float mTitleTextSize = 0;
    private String mTitleText;
    private Bitmap mLogoBmp;

    private void drawTopLogoAndTitle(Canvas canvas) {
        // 绘制logo
        if (mLogoBmp == null) {
            Bitmap temp = BitmapFactory.decodeResource(getResources(), R.drawable.accessibility_super_cms_logo);
            float ratio = mLogoSize / temp.getWidth();
            Matrix matrix = new Matrix();
            matrix.postScale(ratio, ratio);
            mLogoBmp = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }
        if (mLogoBmp != null) {
            canvas.drawBitmap(mLogoBmp, mLogoMarginLeft, mLogoMarginTop, mProgressPaint);
        }
        // 绘制标题
        mProgressPaint.setStrokeWidth(0);
        mProgressPaint.setStyle(Paint.Style.FILL);
        mProgressPaint.setColor(mPercentTextColor);
        mProgressPaint.setTextSize(mTitleTextSize);
        mProgressPaint.setTextAlign(Paint.Align.LEFT);
        mProgressPaint.setTypeface(Typeface.DEFAULT);                        // 设置字体
        Paint.FontMetrics fontMetrics = mProgressPaint.getFontMetrics();
        mFontBaseline = mTitleCenterY - (fontMetrics.top + fontMetrics.bottom) / 2;
        canvas.drawText(mTitleText, mTitleLeftX , mFontBaseline, mProgressPaint);
    }

    // ================ 外环进度数据 ============= //
    /** 顶部作为计数起点 270度, 计算圆上的任意点坐标时顺时针为正，右边是0 */
    private static final float HEAD_CIRCLE_START_ANGLE = 270f;
    /** 进度条每次移动的角度 */
    private static int mOuterProgressStep = 6;
    /** 修改这个颜色数组就会出现不一样的渐变圆弧 */
    private int[] mColors = {
            0x0004d3ff, 0x0004d3ff, 0x4004d3ff, 0x8004d3ff, 0xff04d3ff
    };
    /** 外环渐变处理器 */
    private SweepGradient mOuterSweepGradient;
    /** 外环用于旋转的矩阵 Matrix */
    private Matrix mOuterMatrix = new Matrix();
    /** 外圆环的宽度 */
    private float mOuterRoundWidth;
    /** 外圆环头部的圆圈半径 */
    private float mOuterHeadCircleWidth;
    /** 外环的半径 */
    private float mOuterRadius = 0;
    /** 外环角度旋转总进度*/
    private float mOuterAngleProgressTotal = 0;
    /** 外环头部圆选择角度 */
    private float mOuterHeadCircleAngleTotal = 0;
    private double mOuterHeadCircleAngleTotalMath = 0;
    private void drawOuterGradientProgress(final Canvas canvas) {
        mProgressPaint.setStrokeWidth(mOuterRoundWidth);         // 设置圆环的宽度
        mProgressPaint.setColor(mRoundProgressColor);       // 设置进度的颜色
        mProgressPaint.setAlpha(mProgressAlpha);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        // 定义一个梯度渲染，由于梯度渲染是从三点钟方向开始，所以再让他逆时针旋转90°，从0点开始
        if (mOuterSweepGradient == null) {
            mOuterSweepGradient = new SweepGradient(mCenterX, mCenterY, mColors, null);
        }
        mOuterMatrix.setRotate((START_POINT_TOP + mOuterAngleProgressTotal), mCenterX, mCenterY);
        mOuterSweepGradient.setLocalMatrix(mOuterMatrix);
        mProgressPaint.setShader(mOuterSweepGradient);
        canvas.drawCircle(mCenterX, mCenterY, mOuterRadius, mProgressPaint); // 画出圆环
        drawOuterArcHeadCircle(canvas);
        mOuterAngleProgressTotal += mOuterProgressStep;
        if (mOuterAngleProgressTotal > TOTAL_ANGLE) {
            mOuterAngleProgressTotal -= TOTAL_ANGLE;
        }
    }

    private void drawOuterArcHeadCircle(final Canvas canvas) {
        mProgressPaint.setShader(null);
        mProgressPaint.setStrokeWidth(0);
        mProgressPaint.setStyle(Paint.Style.FILL);
        // 一开始从顶部开始旋转
        mOuterHeadCircleAngleTotal = (HEAD_CIRCLE_START_ANGLE + mOuterAngleProgressTotal);
        if (mOuterHeadCircleAngleTotal - TOTAL_ANGLE > 0) {
            mOuterHeadCircleAngleTotal -= TOTAL_ANGLE;
        }
        // 根据旋转角度计算圆上当前位置点坐标，再以当前位置左边点位圆心画一个圆
        mOuterHeadCircleAngleTotalMath = mOuterHeadCircleAngleTotal * Math.PI / 180f;
        canvas.drawCircle((float) (mCenterX + mOuterRadius * Math.cos(mOuterHeadCircleAngleTotalMath)),
                (float) (mCenterY + mOuterRadius * Math.sin(mOuterHeadCircleAngleTotalMath)),
                mOuterHeadCircleWidth, mProgressPaint);
    }

    // ================ 内环进度数据 ============= //
    /** 内环的半径 */
    private float mInnerRadius = 0;
    /** 内圆环的宽度 */
    private float mInnerRoundWidth;
    /** 用于定义的圆弧的形状和大小的界限 */
    private RectF mInnerArcLimitRect = new RectF();
    /** 内环总弧长 */
    private float mInnerArcAngle = 0;
    private void drawInnerProgress(final Canvas canvas) {
        mProgressPaint.setStrokeWidth(mInnerRoundWidth);         // 设置圆环的宽度
        mProgressPaint.setColor(mRoundProgressColor);       // 设置进度的颜色
        mProgressPaint.setAlpha(mProgressAlpha);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setShader(null);
        canvas.drawArc(mInnerArcLimitRect, START_POINT_TOP, mInnerArcAngle, false, mProgressPaint);
    }

    // ================ 中间百分比数据 ============= //
    /** 当前进度 */
    private int mCurProgress = 0;
    private String mCurProgressStr = "";
    private String mPercentSignStr = "%";
    /** 中间进度百分比的字符串的颜色 */
    private int mPercentTextColor = 0xffffffff;
    /** 中间进度百分比的字符串的字体大小 */
    private float mPercentTextSize;
    /** 中间进度百分号字体大小 */
    private float mPercentSignSize;
    /** 百分比文本与%符号之间的间距 */
    private float mSpaceTextAndSign;
    /** 字体绘制的基线 */
    private float mFontBaseline;
    private void drawPercentText(final Canvas canvas) {
        mProgressPaint.setStrokeWidth(0);
        mProgressPaint.setStyle(Paint.Style.FILL);
        mProgressPaint.setColor(mPercentTextColor);
        mProgressPaint.setAlpha(mProgressAlpha);
        mProgressPaint.setTextSize(mPercentTextSize);
        mProgressPaint.setTextAlign(Paint.Align.CENTER);
        mProgressPaint.setTypeface(Typeface.DEFAULT_BOLD);                        // 设置字体

        mProgressPaint.getTextBounds(mCurProgressStr, 0, mCurProgressStr.length(), mTempTextBoundsOne);
        Paint.FontMetrics fontMetrics = mProgressPaint.getFontMetrics();
        mFontBaseline = mCenterY - (fontMetrics.top + fontMetrics.bottom) / 2;

        mProgressPaint.setTextSize(mPercentSignSize);
        mProgressPaint.getTextBounds(mPercentSignStr, 0, mPercentSignStr.length(), mTempTextBoundsTwo);

        if (mCurProgress != 0) {
            mProgressPaint.setTextSize(mPercentTextSize);
            canvas.drawText(mCurProgressStr, mCenterX - (mSpaceTextAndSign + mTempTextBoundsTwo.width()) / 2 ,
                    mFontBaseline, mProgressPaint); // 画出进度百分比

            mProgressPaint.setTextSize(mPercentSignSize);
            canvas.drawText(mPercentSignStr, mCenterX + (mSpaceTextAndSign + mTempTextBoundsOne.width()) / 2,
                    mFontBaseline, mProgressPaint); // 画出进度百分f符号
        }
    }

    private float mTxtCourseSize = 0;
    private int mTextCourseColor = 0xffb8d3fc;
    private String mCurCourseStr = "";
    private String mCourseOmissionStr = "...";
    private String mRealCourseOmissionStr = "";
    private float mCoursePosY = 0;
    private boolean isCourseVisible = true;
    private boolean isCourseShowAnim = false;

    private void drawCurTextCourse(Canvas canvas) {
        if (!isCourseVisible) {
            return;
        }
        calculateCourseAnimData();
        startOmissionAnim();
        mProgressPaint.setShader(null);
        mProgressPaint.setStrokeWidth(0);
        mProgressPaint.setColor(mTextCourseColor);
        mProgressPaint.setAlpha(mCourseAlpha);
        mProgressPaint.setTextSize(mTxtCourseSize);
        mProgressPaint.setTextAlign(Paint.Align.LEFT);
        mProgressPaint.setTypeface(Typeface.DEFAULT);                        // 设置字体

        mProgressPaint.getTextBounds(mCurCourseStr, 0, mCurCourseStr.length(), mTempTextBoundsOne);
        Paint.FontMetrics fontMetrics = mProgressPaint.getFontMetrics();
        mFontBaseline = mCoursePosY - (fontMetrics.top + fontMetrics.bottom) / 2;
        mProgressPaint.getTextBounds(mCourseOmissionStr, 0, mCourseOmissionStr.length(), mTempTextBoundsTwo);

        canvas.drawText(mCurCourseStr, mCenterX - (mTempTextBoundsOne.width() / 2 + mTempTextBoundsTwo.width()) ,
                mFontBaseline, mProgressPaint); // 话当前权限执行过程提示文本
        canvas.drawText(mRealCourseOmissionStr, mCenterX + mSpaceTextAndSign + (mTempTextBoundsOne.width() / 2 - mTempTextBoundsTwo.width()),
                mFontBaseline, mProgressPaint); // 画...
    }

    private float mCourseAlphaStep = 255f / 300f;
    private int mCourseAlpha = 0;
    private long mCourseAnimTimeStart = 0;
    private long mCourseAnimDeltaTime = 0;
    private void calculateCourseAnimData() {
        if (isCourseShowAnim) {
            if (mCourseAnimTimeStart == 0) {
                mCourseAnimTimeStart = System.currentTimeMillis();
                mCourseAlpha = 0;
            }
            mCourseAnimDeltaTime = System.currentTimeMillis() - mCourseAnimTimeStart;
            if (mCourseAnimDeltaTime > 300) {
                isCourseShowAnim = false;
                mCourseAlpha = 255;
                return;
            }
            mCourseAlpha =(int) (mCourseAlphaStep * mCourseAnimDeltaTime);
        }
    }

    private long mOmissionTimeStart = 0;
    private long mOmissionDeltaTime = 0;
    private static final String ONE_DOT = ".";
    private static final String TWO_DOT = "..";
    private static final String THREE_DOT = "...";
    private void startOmissionAnim() {
        if (mOmissionTimeStart == 0) {
            mOmissionTimeStart = System.currentTimeMillis();
        }
        mOmissionDeltaTime = System.currentTimeMillis() - mOmissionTimeStart;
        if (mOmissionDeltaTime < 400) {
            mRealCourseOmissionStr = ONE_DOT;
        } else if (mOmissionDeltaTime < 800) {
            mRealCourseOmissionStr = TWO_DOT;
        } else if (mOmissionDeltaTime < 1200) {
            mRealCourseOmissionStr = THREE_DOT;
        } else {
            mOmissionTimeStart = System.currentTimeMillis();
        }
    }

    private float mResultTextSize = 0;
    private float mSubResultTextSize = 0;
    private int mResultTextColor = 0xffffffff;
    private int mSubResultTextColor = 0xffb8d3fc;
    private String mResultStr = "";
    private String mSubResultStr = "";
    private float mResultPosY = 0;
    private float mSubResultPosY = 0;
    private boolean isResultTextVisible = false;
    private boolean isSubResultTextVisible = false;
    private boolean isShowResultAnim = false;
    private boolean isShowSubResultAnim = false;

    private void drawResultText(Canvas canvas) {
        if (isResultTextVisible) {
            calculateResultAnimData();
            mProgressPaint.setColor(mResultTextColor);
            mProgressPaint.setAlpha(mResultAlpha);
            mProgressPaint.setTextSize(mResultTextSize);
            mProgressPaint.setTextAlign(Paint.Align.CENTER);
            mProgressPaint.setTypeface(Typeface.DEFAULT);                        // 设置字体
            mProgressPaint.getTextBounds(mResultStr, 0, mResultStr.length(), mTempTextBoundsOne);
            Paint.FontMetrics fontMetrics = mProgressPaint.getFontMetrics();
            mFontBaseline = mResultPosY - (fontMetrics.top + fontMetrics.bottom) / 2;
            canvas.drawText(mResultStr, mCenterX, mFontBaseline, mProgressPaint); // 画结果文本
        }
        if (isSubResultTextVisible) {
            calculateSubResultAnimData();
            mProgressPaint.setColor(mSubResultTextColor);
            mProgressPaint.setAlpha(mSubResultAlpha);
            mProgressPaint.setTextSize(mSubResultTextSize);
            mProgressPaint.setTextAlign(Paint.Align.CENTER);
            mProgressPaint.setTypeface(Typeface.DEFAULT);
            mProgressPaint.getTextBounds(mSubResultStr, 0, mSubResultStr.length(), mTempTextBoundsOne);
            Paint.FontMetrics fontMetrics = mProgressPaint.getFontMetrics();
            mFontBaseline = mSubResultPosY - (fontMetrics.top + fontMetrics.bottom) / 2;
            canvas.drawText(mSubResultStr, mCenterX, mFontBaseline, mProgressPaint); // 画子结果文本
        }
    }

    private float mResultAlphaStep = 255f / 300f;
    private int mResultAlpha = 0;
    private long mResultAnimTimeStart = 0;
    private long mResultAnimDeltaTime = 0;
    private void calculateResultAnimData() {
        if (isShowResultAnim) {
            if (mResultAnimTimeStart == 0) {
                mResultAnimTimeStart = System.currentTimeMillis();
                mResultAlpha = 0;
            }
            mResultAnimDeltaTime = System.currentTimeMillis() - mResultAnimTimeStart;
            if (mResultAnimDeltaTime > 300) {
                isShowResultAnim = false;
                mResultAlpha = 255;
                return;
            }
            mResultAlpha = (int) (mResultAlphaStep * mResultAnimDeltaTime);
        }
    }

    private float mSubResultAlphaStep = 255f / 300f;
    private int mSubResultAlpha = 0;
    private long mSubResultAnimTimeStart = 0;
    private long mSubResultAnimDeltaTime = 0;
    private void calculateSubResultAnimData() {
        if (isShowSubResultAnim) {
            if (mSubResultAnimTimeStart == 0) {
                mSubResultAnimTimeStart = System.currentTimeMillis();
                mSubResultAlpha = 0;
            }
            mSubResultAnimDeltaTime = System.currentTimeMillis() - mSubResultAnimTimeStart;
            if (mSubResultAnimDeltaTime > 300) {
                isShowSubResultAnim = false;
                mSubResultAlpha = 255;
                return;
            }
            mSubResultAlpha = (int) (mSubResultAlphaStep * mSubResultAnimDeltaTime);
        }
    }

    private float mBottomTipTextSize = 0;
    private float mBottomTipPosY = 0;
    private String mBottomTipStr = "";
    private boolean isBottomTipVisible = true;
    private boolean isBottomTipShowAnim = false;

    private void drawBottomTipText(Canvas canvas) {
        if (!isBottomTipVisible) {
            return;
        }
        calculateBottomTipAnimData();
        if (mBottomTipTextSize == 0) {
            mBottomTipTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, mMetrics);
            mBottomTipPosY = getHeight() - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 78, mMetrics);
        }
        mProgressPaint.setColor(Color.WHITE);
        mProgressPaint.setAlpha(mTipAlpha);
        mProgressPaint.setTextSize(mBottomTipTextSize);
        mProgressPaint.setTextAlign(Paint.Align.CENTER);
        mProgressPaint.setTypeface(Typeface.DEFAULT);
        mProgressPaint.getTextBounds(mBottomTipStr, 0, mBottomTipStr.length(), mTempTextBoundsOne);
        Paint.FontMetrics fontMetrics = mProgressPaint.getFontMetrics();
        mFontBaseline = mBottomTipPosY - (fontMetrics.top + fontMetrics.bottom) / 2;
        canvas.drawText(mBottomTipStr, mCenterX, mFontBaseline, mProgressPaint); // 画结果文本
    }

    private float mTipAlphaStep = 255f / 300f;
    private int mTipAlpha = 0;
    private long mTipAnimTimeStart = 0;
    private long mTipAnimDeltaTime = 0;
    private void calculateBottomTipAnimData() {
        if (isBottomTipShowAnim) {
            if (mTipAnimTimeStart == 0) {
                mTipAnimTimeStart = System.currentTimeMillis();
                mTipAlpha = 0;
            }
            mTipAnimDeltaTime = System.currentTimeMillis() - mTipAnimTimeStart;
            if (mTipAnimDeltaTime > 300) {
                isBottomTipShowAnim = false;
                mTipAlpha = 255;
                return;
            }
            mTipAlpha = (int) (mTipAlphaStep * mTipAnimDeltaTime);
        }
    }

    /**
     * progress的范围 0~360
     * @param angle
     */
    public void setAngle(float angle) {
        if (angle - TOTAL_ANGLE > 0) {
            angle = TOTAL_ANGLE;
        } else if (angle < 0) {
            angle = 0;
        }
        mInnerArcAngle = angle;
        int progress = (int) (mInnerArcAngle / TOTAL_ANGLE * PERCENT_BASE);
        if (progress == mCurProgress) { // 相同进度不重复设置，避免notify重复通知
            return;
        }
        if (progress < MIN_PROGRESS_DEFAULT) {
            progress = MIN_PROGRESS_DEFAULT;
        } else if (progress > MAX_PROGRESS_DEFAULT) {
            progress = MAX_PROGRESS_DEFAULT;
        }
        mCurProgress = progress;
        mCurProgressStr = "" + mCurProgress;
        notifyProgressStateChangeListeners();
    }

    public float getAngle() {
        return mInnerArcAngle;
    }

    public interface IProgressStateChangeListener {
        /** 进度执行到100%时回调 */
        void onFinished();
        /** 执行到外部指定的进度回调 */
        void onSmoothScrollFinish();
    }

    public boolean isCompleted() {
        return mCurProgress == MAX_PROGRESS_DEFAULT;
    }

    private void notifyProgressStateChangeListeners() {
        if (mProgressStateChangeListeners == null) {
            return;
        }
        if (isCompleted()) {
            isProgressHideAnim = true;
            return;
        }
        if (mCurProgress == mSmoothScrollToProgress) {
            for (IProgressStateChangeListener listener : mProgressStateChangeListeners) {
                if (listener != null) {
                    listener.onSmoothScrollFinish();
                }
            }
        }
    }

    public void addProgressStateListener(IProgressStateChangeListener listener) {
        mProgressStateChangeListeners.add(listener);
    }

    public void onDestroy() {
        if (mProgressStateChangeListeners != null) {
            mProgressStateChangeListeners.clear();
            mProgressStateChangeListeners = null;
        }
    }

    public void setTextCourseStr(String textCourseStr) {
        mCurCourseStr = textCourseStr;
    }

    public void setResultStr(String resultStr) {
        mResultStr = resultStr;
    }

    public void setSubResultStr(String subResultStr) {
        mSubResultStr = subResultStr;
    }

    public void setBottomTipStr(String bottomTipStr) {
        mBottomTipStr = bottomTipStr;
    }

    public void setProgressVisible(boolean progressVisible) {
        isProgressVisible = progressVisible;
    }

    public void setResultVisible(boolean resultVisible) {
        if (resultVisible && !isResultTextVisible) {
            isShowResultAnim = true;
        }
        isResultTextVisible = resultVisible;
    }

    public void setSubResultVisible(boolean subResultVisible) {
        if (subResultVisible && !isSubResultTextVisible) {
            isShowSubResultAnim = true;
        }
        isSubResultTextVisible = subResultVisible;
    }

    public void setBottomTipVisible(boolean bottomTipVisible) {
        if (bottomTipVisible && !isBottomTipVisible) {
            isBottomTipShowAnim = true;
        }
        isBottomTipVisible = bottomTipVisible;
    }

    public void setCourseVisible(boolean courseVisible) {
        if (courseVisible && !isCourseVisible) {
            isCourseShowAnim = true;
        }
        isCourseVisible = courseVisible;
    }
}
