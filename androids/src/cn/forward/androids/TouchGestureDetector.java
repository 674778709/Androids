package cn.forward.androids;

import android.content.Context;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.lang.reflect.Field;

import cn.forward.androids.utils.ReflectUtil;

/**
 * 集成GestureDetector、ScaleGestureDetector，方便识别常用手势（点击、双击、长按、缩放等），并对特定场景下的手势识别进行优化
 */
public class TouchGestureDetector {

    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleGestureDetector;
    private final IOnTouchGestureListener mOnTouchGestureListener;
    private boolean mIsScrollAfterScaled = true; // 在一串事件序列中，缩放onScale后继续识别onScroll手势

    public TouchGestureDetector(Context context, final IOnTouchGestureListener listener) {
        mOnTouchGestureListener = new OnTouchGestureListenerProxy(listener);
        mGestureDetector = new GestureDetector(context, mOnTouchGestureListener);
        mGestureDetector.setOnDoubleTapListener(mOnTouchGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mOnTouchGestureListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mScaleGestureDetector.setQuickScaleEnabled(false);
        }
    }

    /**
     * 缩放过程中识别为缩放手势的双指最小距离值
     *
     * @param minSpan
     */
    public void setScaleMinSpan(int minSpan) {
        Field field = ReflectUtil.getField(ScaleGestureDetector.class, "mMinSpan");
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        try {
            field.set(mScaleGestureDetector, minSpan);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 识别为缩放手势的双指滑动最小距离值
     *
     * @param spanSLop
     */
    public void setScaleSpanSlop(int spanSLop) {
        //mSpanSlop
        Field field = ReflectUtil.getField(ScaleGestureDetector.class, "mSpanSlop");
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        try {
            field.set(mScaleGestureDetector, spanSLop);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否开启长按识别。如果取消长按识别，则长按后移动手指会触发onScroll事件
     *
     * @param isLongpressEnabled
     */
    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        mGestureDetector.setIsLongpressEnabled(isLongpressEnabled);
    }

    /**
     * 是否开启长按识别
     *
     * @return
     */
    public boolean isLongpressEnabled() {
        return mGestureDetector.isLongpressEnabled();
    }

    /**
     * 在一串事件序列中，缩放onScale后是否继续识别onScroll手势
     *
     * @param scrollAfterScaled
     */
    public void setIsScrollAfterScaled(boolean scrollAfterScaled) {
        mIsScrollAfterScaled = scrollAfterScaled;
    }

    public boolean isScrollAfterScaled() {
        return mIsScrollAfterScaled;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            mOnTouchGestureListener.onUpOrCancel(event);
        }
        boolean ret = mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            ret |= mGestureDetector.onTouchEvent(event);
        }
        return ret;
    }

    /**
     * 代理
     */
    private class OnTouchGestureListenerProxy implements IOnTouchGestureListener {

        private IOnTouchGestureListener mListener;
        private boolean mHasScaled = false; // 当前触摸序列中是否已经识别了缩放手势，如果是的话，则后续不会触发onScroll

        public OnTouchGestureListenerProxy(IOnTouchGestureListener listener) {
            this.mListener = listener;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            // 触摸序列的开始，初始化
            mHasScaled = false;
            return mListener.onDown(e);
        }

        @Override
        public void onUpOrCancel(MotionEvent e) {
            mListener.onUpOrCancel(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return mListener.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mListener.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mIsScrollAfterScaled && mHasScaled) { // 当前触摸序列中已经识别了缩放手势,后续不会触发onScroll
                return false;
            }
            return mListener.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            mListener.onShowPress(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return mListener.onSingleTapUp(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return mListener.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return mListener.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return mListener.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return mListener.onScale(detector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mHasScaled = true;
            return mListener.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mListener.onScaleEnd(detector);
        }
    }

    /**
     * 识别手势的回调接口
     */
    public static interface IOnTouchGestureListener extends GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {
        public void onUpOrCancel(MotionEvent e);
    }

    public static abstract class OnTouchGestureListener implements IOnTouchGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onUpOrCancel(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }
}

