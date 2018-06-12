package com.dreamer.magictransformview.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import com.dreamer.magictransformview.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ysx on 2017/11/29.
 */

public class MagicTransformView extends View {

    private static final String TAG = "MagicTransformView";
    private static final int MSG_UPDATE_ANIM = 0;

    private Context mContext;

    private int mLeftRightInterval;
    private int mWidth, mHeight;
    private int mRadius;
    private int mTextSize;
    private int mTextMargin;
    private int mAnimCurrentPage = 0;
    private int mAnimMaxPage = 6;
    private int mAnimDuration = 400;
    private float mProgressPercent;

    // 评论、下载文本的宽度
    private float mLeftTextWidth, mRightTextWidth;
    // 评论、下载内容的宽度
    private float mLeftContentWidth, mRightContentWidth;

    private Bitmap mWriteBitmap, mDownloadBitmap;
    private List<Bitmap> mBitmaps;

    private Paint mPaint, mRightPaint, mTextLeftPaint, mTextRightPaint;
    private Path mLeftPath, mRightPath;
    private RectF mLeftRectF, mRightRectF;
    private LinearGradient mProgressGradient;

    private String mTextLeft, mTextRight;

    private AnimatorSet mAnimatorSet;
    private MagicTransformListener mListener;

    public static final int LEFT_EXPAND = 0;
    public static final int RIGHT_EXPAND = 1;
    public static final int NOTHING_EXPAND = 2;
    @IntDef({LEFT_EXPAND, RIGHT_EXPAND, NOTHING_EXPAND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MagicTransformViewState{}

    @MagicTransformViewState int expandState = RIGHT_EXPAND;

    public static final int NOT_DOWNLOAD = 0;
    public static final int DOWNLOADING = 1;
    public static final int PAUSE_DOWNLOAD = 2;
    public static final int INSTALL_APP = 3;
    public static final int OPEN_APP = 4;
    @IntDef({NOT_DOWNLOAD, DOWNLOADING, PAUSE_DOWNLOAD, INSTALL_APP, OPEN_APP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DownloadState{}

    @DownloadState int downloadState = NOT_DOWNLOAD;


    public MagicTransformView(Context context) {
        this(context, null);
    }

    public MagicTransformView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MagicTransformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MagicTransformView);

        try {
            mLeftRightInterval = ta.getDimensionPixelSize(R.styleable.MagicTransformView_leftRightInterval, dip2px(context, 20));
            mTextLeft = ta.getString(R.styleable.MagicTransformView_textLeft);
            mTextRight = ta.getString(R.styleable.MagicTransformView_textRight);

            mBitmaps = new ArrayList<>();
            int resourceId = ta.getResourceId(R.styleable.MagicTransformView_rightSrc, 0);
            TypedArray typedArray = getResources().obtainTypedArray(resourceId);
            for (int i = 0; i < typedArray.length(); i++) {
                mBitmaps.add(BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(i, 0)));
            }
            mTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12.0f, getResources().getDisplayMetrics());
            mTextMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.0f, getResources().getDisplayMetrics());
        } finally {
            ta.recycle();
        }

        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mWriteBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_write_comment);
        if (!mBitmaps.isEmpty()) {
            mDownloadBitmap = mBitmaps.get(mAnimCurrentPage);
        } else {
            mDownloadBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.small_download_gif_1);
        }

        mRadius = dip2px(mContext, 15);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.yellow));

        mRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRightPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mRightPaint.setColor(ContextCompat.getColor(mContext, R.color.yellow));

        mTextLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextLeftPaint.setColor(ContextCompat.getColor(mContext, R.color.black_text));
        mTextLeftPaint.setTextSize(mTextSize);

        mTextRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextRightPaint.setColor(ContextCompat.getColor(mContext, R.color.black_text));
        mTextRightPaint.setTextSize(mTextSize);

        mLeftPath = new Path();
        mRightPath = new Path();

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setDuration(mAnimDuration);
        mAnimatorSet.setInterpolator(new AccelerateInterpolator());

        // 测量文本宽度
        mLeftTextWidth = mTextLeftPaint.measureText(mTextLeft);
        mRightTextWidth = mTextRightPaint.measureText(mTextRight);

        mLeftContentWidth = mLeftTextWidth + mWriteBitmap.getWidth();
        mRightContentWidth = mRightTextWidth;

        mWidth = getResources().getDisplayMetrics().widthPixels;
        mHeight = dip2px(mContext, 49);

        mLeftRectF = new RectF(getPaddingLeft(),
                getPaddingTop(),
                2 * mRadius + getPaddingLeft(),
                mHeight - getPaddingBottom());

        // 初始化下载按钮的所占有的空间
        mRightRectF = new RectF(mLeftRectF.right + mLeftRightInterval,
                getPaddingTop(),
                mWidth - getPaddingRight(),
                mHeight - getPaddingBottom());
    }

    public void setMagicTransformListener(MagicTransformListener listener) {
        mListener = listener;
    }

    public int getDownloadState() {
        return downloadState;
    }

    /**
     * 设置下载状态
     * @param downloadState
     */
    public void setDownloadState(int downloadState) {
        this.downloadState = downloadState;
    }

    public void setRightText(String text) {
        mTextRight = text;
        mRightTextWidth = mTextRightPaint.measureText(mTextRight);
        mRightContentWidth = mRightTextWidth;
        invalidate();
    }

    /**
     * 设置下载进度
     * @param percent 进度值，取值范围（0.0~1.0）
     */
    public void setDownloadProgress(float percent) {
        mProgressPercent = percent;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(Color.WHITE);
        // ------------------------------------------- 左边内容 --------------------------------------------------------//
        // 画背景
        mLeftPath.addRoundRect(mLeftRectF, mRadius, mRadius, Path.Direction.CW);
        canvas.drawPath(mLeftPath, mPaint);

        float leftBitmapPosX, leftTextPosX;
        if (mLeftRectF.width() > mLeftContentWidth) {
            leftBitmapPosX = mLeftRectF.centerX() - mLeftContentWidth / 2 - mTextMargin;
            leftTextPosX = mLeftRectF.centerX() - mLeftContentWidth / 2 + mWriteBitmap.getWidth();
            // 写文字
            canvas.drawText(mTextLeft, leftTextPosX,
                    mLeftRectF.centerY() + getTextHeight(mTextLeftPaint), mTextLeftPaint);
        } else {
            leftBitmapPosX = mLeftRectF.centerX() - mWriteBitmap.getWidth() / 2;
            leftTextPosX = 0;
        }
        // 画评论图标
        canvas.drawBitmap(mWriteBitmap, leftBitmapPosX,
                mLeftRectF.centerY() - mWriteBitmap.getHeight() / 2, null);

        // ----------------------------------------- 右边内容 ---------------------------------------------------------//

        // 画背景
        if (expandState == RIGHT_EXPAND && (downloadState == DOWNLOADING ||
                downloadState == PAUSE_DOWNLOAD)) {
            mProgressGradient = new LinearGradient(mRightRectF.left,
                    0,
                    mRightRectF.right,
                    0,
                    new int[]{ContextCompat.getColor(mContext, R.color.yellow),
                            ContextCompat.getColor(mContext, R.color.progress_yellow)},
                    new float[]{mProgressPercent, mProgressPercent + 0.001f},
                    Shader.TileMode.CLAMP);
            mRightPaint.setShader(mProgressGradient);
        } else {
            mRightPaint.setShader(null);
            mRightPaint.setColor(ContextCompat.getColor(mContext, R.color.yellow));
        }
        mRightPath.addRoundRect(mRightRectF, mRadius, mRadius, Path.Direction.CW);
        canvas.drawPath(mRightPath, mRightPaint);
        // 画进度

        float rightBitmapPosX, rightTextPosX;
        if (mRightRectF.width() > mRightContentWidth) {
            rightBitmapPosX = mRightRectF.centerX() - mRightContentWidth / 2 - mTextMargin;
            if (downloadState == NOT_DOWNLOAD) {
                rightTextPosX = mRightRectF.centerX() - mRightContentWidth / 2 + mDownloadBitmap.getWidth();
            } else {
                rightTextPosX = mRightRectF.centerX() - mRightContentWidth / 2;
            }
            // 写文字
            canvas.drawText(mTextRight, rightTextPosX,
                    mRightRectF.centerY() + getTextHeight(mTextRightPaint), mTextRightPaint);
        } else {
            rightBitmapPosX = mRightRectF.centerX() - mDownloadBitmap.getWidth() / 2;
            rightTextPosX = 0;
        }

        // 画下载图标
        if (downloadState == NOT_DOWNLOAD || expandState == LEFT_EXPAND) {
            if (expandState == LEFT_EXPAND) {
                rightBitmapPosX = mRightRectF.centerX() - mDownloadBitmap.getWidth() / 2;
            }
            canvas.drawBitmap(mDownloadBitmap, rightBitmapPosX,
                    mRightRectF.centerY() - mDownloadBitmap.getHeight() / 2, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (mAnimatorSet.isRunning()) return true;

                if (mLeftRectF.contains(x, y)) {
                    //Log.d(TAG, "评论点击");
                    if (expandState == RIGHT_EXPAND) {
                        if (mListener != null)
                            mListener.onSwitchPage(1);
                        animExpandLeft();
                    }

                    if (mListener != null && expandState == LEFT_EXPAND) {
                        mListener.onLeftViewClick();
                    }

                } else if (mRightRectF.contains(x , y)) {
                   // Log.d(TAG, "下载点击");
                    if (expandState == LEFT_EXPAND) {
                        if (mListener != null)
                            mListener.onSwitchPage(0);
                        animExpandRight();
                    }
                    if (mListener != null && expandState == RIGHT_EXPAND) {
                        mListener.onRightViewClick(downloadState);
                    }
                }
                break;
        }
        return true;
    }

    /**
     * 展开评论按钮
     */
    private void animExpandLeft() {
        // 展开评论按钮
        final float leftTargetDistance = mWidth - getPaddingRight() - mLeftRightInterval - 2 * mRadius;
        ValueAnimator valueAnimatorLeft = ValueAnimator.ofFloat(mLeftRectF.right, leftTargetDistance);
        valueAnimatorLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float right = (float) animation.getAnimatedValue();
                mLeftRectF.right = right;
                mTextLeftPaint.setAlpha((int) (255 * (right / leftTargetDistance)));
                invalidate();
            }
        });

        final float rightTargetDistance = mWidth - getPaddingRight() - 2 * mRadius;
        // 收缩下载按钮
        ValueAnimator valueAnimatorRight = ValueAnimator.ofFloat(mRightRectF.left, rightTargetDistance);
        valueAnimatorRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRightPath.reset();
                mRightRectF.left = (float) animation.getAnimatedValue();
                mTextRightPaint.setAlpha((int) (255 - 255 * (mRightRectF.left / rightTargetDistance)));
            }
        });

        mAnimatorSet.playTogether(valueAnimatorLeft, valueAnimatorRight);
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                resetExpandState();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                expandState = LEFT_EXPAND;
                // 开启动画
                if (downloadState == DOWNLOADING) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_ANIM);
                }
            }
        });
        mAnimatorSet.start();
    }

    /**
     * 展开下载按钮
     */
    private void animExpandRight() {
        // 收缩评论按钮
        final float leftTargetDistance = mLeftRectF.left + 2 * mRadius;
        ValueAnimator valueAnimatorLeft = ValueAnimator.ofFloat(mLeftRectF.right, leftTargetDistance);
        valueAnimatorLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLeftPath.reset();
                float right = (float) animation.getAnimatedValue();
                mLeftRectF.right = right;
                mTextLeftPaint.setAlpha((int) (255 - 255 * (leftTargetDistance / right)));
            }
        });

        // 展开下载按钮
        final float rightTargetDistance = getPaddingLeft() + mLeftRightInterval + 2 * mRadius;
        ValueAnimator valueAnimatorRight = ValueAnimator.ofFloat(mRightRectF.left, rightTargetDistance);
        valueAnimatorRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRightRectF.left = (float) animation.getAnimatedValue();
                mTextRightPaint.setAlpha((int) (255 * rightTargetDistance / mRightRectF.left));
                invalidate();
            }
        });

        mAnimatorSet.playTogether(valueAnimatorLeft, valueAnimatorRight);
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                resetExpandState();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                expandState = RIGHT_EXPAND;
            }
        });
        mAnimatorSet.start();
    }

    /**
     * 和ViewPager联动
     * @param position
     */
    public void switchView(int position) {
        if (mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel();
        }

        if (position == 0) {
            animExpandRight();
        } else {
            animExpandLeft();
        }
    }

    /**
     * 重置按钮的状态
     */
    private void resetExpandState() {
        resetDownloadAnim();
        expandState = NOTHING_EXPAND;
    }

    public void resetDownloadAnim() {
        mHandler.removeMessages(MSG_UPDATE_ANIM);
        mAnimCurrentPage = 0;
        mDownloadBitmap = mBitmaps.get(mAnimCurrentPage);
    }

    /**
     * 测量文本高度
     * @param paint
     * @return
     */
    private float getTextHeight(Paint paint) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        //metrics.ascent为负数
        float dy = -(metrics.descent + metrics.ascent) / 2;
        return dy;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(MSG_UPDATE_ANIM);
    }

    private int dip2px(Context context, int dipValue) {
        float scale =
                context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mHandler.removeMessages(MSG_UPDATE_ANIM);

            if (msg.what == MSG_UPDATE_ANIM && expandState == LEFT_EXPAND) {
                mAnimCurrentPage ++;
                if (mAnimCurrentPage >= mAnimMaxPage) {
                    mAnimCurrentPage = 0;
                }
                mDownloadBitmap = mBitmaps.get(mAnimCurrentPage);
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIM, 150);
                invalidate();
            }
        }
    };

    public interface MagicTransformListener {

        void onSwitchPage(int position);

        void onLeftViewClick();

        void onRightViewClick(@DownloadState int state);
    }
}
