# DailySnipetCodeAndroid

## Note Android news version 
### Broadcast Receiver
** Android 9: **
- Beginning with Android 9 (API level 28), The NETWORK_STATE_CHANGED_ACTION broadcast doesn't receive information about the *user's location* or personally identifiable data.

In addition, if your app is installed on a device running Android 9 or higher, system broadcasts from Wi-Fi don't contain SSIDs, BSSIDs, connection information, or scan results. To get this information, call getConnectionInfo() instead.

** Android 8: **
- Begging with Android 8 , the system imposes additional restrictions on manifest declared receiver (-- nếu tagget version từ android version 8, hệ thống sẽ hạn chế(không cho phép) việc khai báo Broadcast receiver  trong manifest. Require regist and listent Broadcast Receiver trong Context() và phải unregist khi không còn sử dung để tránh Memory leak).



## SnipetCode

### 1.AdapterRetrofit with RXJava
```kotlin
fun <T> Call<T>.waiting(): Single<Response<T>> = Single.create {single ->

    enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
            single.onError(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            single.onSuccess(response)
        }

    })
}
```

### 2. CustomRecyclerViewLikeViewPager
```java
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class RecyclerViewPager extends RecyclerView
{
    private float mTriggerOffset = 0.25f;
    private float mFlingFactor = 0.15f;
    private float mMillisecondsPerInch = 100f;
    private float mTouchSpan;
    private List<OnPageChangedListener> mOnPageChangedListeners;
    private int mSmoothScrollTargetPosition = -1;
    private int mPositionBeforeScroll = -1;
    private boolean mSinglePageFling = true;
    private boolean mNeedAdjust;
    private int mFirstLeftWhenDragging;
    private int mFirstTopWhenDragging;
    private View mCurView;
    private int mMaxLeftWhenDragging = Integer.MIN_VALUE;
    private int mMinLeftWhenDragging = Integer.MAX_VALUE;
    private int mMaxTopWhenDragging = Integer.MIN_VALUE;
    private int mMinTopWhenDragging = Integer.MAX_VALUE;
    private int mPositionOnTouchDown = -1;
    private boolean mHasCalledOnPageChanged = true;
    private boolean reverseLayout = false;

    public RecyclerViewPager(Context context)
    {
        this(context, null);
    }

    public RecyclerViewPager(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public RecyclerViewPager(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void setLayoutManager(LayoutManager layout)
    {
        super.setLayoutManager(layout);
        if (layout instanceof LinearLayoutManager)
            reverseLayout = ((LinearLayoutManager) layout).getReverseLayout();
    }

    @Override
    public boolean fling(int velocityX, int velocityY)
    {
        boolean flinging = super.fling((int) (velocityX * mFlingFactor), (int) (velocityY * mFlingFactor));
        if (flinging)
            if (getLayoutManager().canScrollHorizontally())
                adjustPositionX(velocityX);
            else
                adjustPositionY(velocityY);
        return flinging;
    }

    @Override
    public void smoothScrollToPosition(int position)
    {
        if (mPositionBeforeScroll < 0)
        {
            mPositionBeforeScroll = getCurrentPosition();
        }
        mSmoothScrollTargetPosition = position;
        if (getLayoutManager() != null && getLayoutManager() instanceof LinearLayoutManager)
        {
            // exclude item decoration
            LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(getContext())
            {
                @Override
                public PointF computeScrollVectorForPosition(int targetPosition)
                {
                    if (getLayoutManager() == null)
                        return null;
                    return ((LinearLayoutManager) getLayoutManager()).computeScrollVectorForPosition(targetPosition);
                }

                @Override
                protected void onTargetFound(View targetView, RecyclerView.State state, Action action)
                {
                    if (getLayoutManager() == null)
                        return;
                    int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
                    int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
                    if (dx > 0)
                        dx = dx - getLayoutManager().getLeftDecorationWidth(targetView);
                    else
                        dx = dx + getLayoutManager().getRightDecorationWidth(targetView);
                    if (dy > 0)
                        dy = dy - getLayoutManager().getTopDecorationHeight(targetView);
                    else
                        dy = dy + getLayoutManager().getBottomDecorationHeight(targetView);
                    final int distance = (int) Math.sqrt(dx * dx + dy * dy);
                    final int time = calculateTimeForDeceleration(distance);
                    if (time > 0)
                        action.update(-dx, -dy, time, mDecelerateInterpolator);
                }

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics)
                {
                    return mMillisecondsPerInch / displayMetrics.densityDpi;
                }

                @Override
                protected void onStop()
                {
                    super.onStop();
                    if (mOnPageChangedListeners != null)
                    {
                        for (OnPageChangedListener onPageChangedListener : mOnPageChangedListeners)
                        {
                            if (onPageChangedListener != null)
                            {
                                onPageChangedListener.OnPageChanged(mPositionBeforeScroll, mSmoothScrollTargetPosition);
                            }
                        }
                    }
                    mHasCalledOnPageChanged = true;
                }
            };

            linearSmoothScroller.setTargetPosition(position);
            if (position == RecyclerView.NO_POSITION)
                return;
            getLayoutManager().startSmoothScroll(linearSmoothScroller);
        } else
            super.smoothScrollToPosition(position);
    }

    @Override
    public void scrollToPosition(int position)
    {
        mPositionBeforeScroll = getCurrentPosition();
        mSmoothScrollTargetPosition = position;
        super.scrollToPosition(position);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if (Build.VERSION.SDK_INT < 16)
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                else
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (mSmoothScrollTargetPosition >= 0 && mSmoothScrollTargetPosition < getItemCount() && mOnPageChangedListeners != null)
                    for (OnPageChangedListener onPageChangedListener : mOnPageChangedListeners)
                    {
                        if (onPageChangedListener != null)
                            onPageChangedListener.OnPageChanged(mPositionBeforeScroll, getCurrentPosition());
                    }
            }
        });
    }

    private int getItemCount()
    {
        return getAdapter().getItemCount();
    }

    /**
     * get item position in center of viewpager
     */
    public int getCurrentPosition()
    {
        int curPosition;
        if (getLayoutManager().canScrollHorizontally())
            curPosition = ViewUtils.getCenterXChildPosition(this);
        else
            curPosition = ViewUtils.getCenterYChildPosition(this);
        if (curPosition < 0)
            curPosition = mSmoothScrollTargetPosition;
        return curPosition;
    }


    private boolean isLeftToRightMode()
    {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR;
    }

    /***
     * adjust position before Touch event complete and fling action start.
     */
    protected void adjustPositionX(int velocityX)
    {

        if (reverseLayout) velocityX *= -1;
        if (!isLeftToRightMode()) velocityX *= -1;

        int childCount = getChildCount();
        if (childCount > 0)
        {
            int curPosition = ViewUtils.getCenterXChildPosition(this);
            int childWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            int flingCount = getFlingCount(velocityX, childWidth);
            int targetPosition = curPosition + flingCount;
            if (mSinglePageFling)
            {
                flingCount = Math.max(-1, Math.min(1, flingCount));
                targetPosition = flingCount == 0 ? curPosition : mPositionOnTouchDown + flingCount;
            }
            targetPosition = Math.max(targetPosition, 0);
            targetPosition = Math.min(targetPosition, getItemCount() - 1);
            if (targetPosition == curPosition
                    && (!mSinglePageFling || mPositionOnTouchDown == curPosition))
            {
                View centerXChild = ViewUtils.getCenterXChild(this);
                if (centerXChild != null)
                {
                    if (mTouchSpan > centerXChild.getWidth() * mTriggerOffset * mTriggerOffset && targetPosition != 0)
                    {
                        if (!reverseLayout) targetPosition--;
                        else targetPosition++;
                    } else if (mTouchSpan < centerXChild.getWidth() * -mTriggerOffset && targetPosition != getItemCount() - 1)
                    {
                        if (!reverseLayout) targetPosition++;
                        else targetPosition--;
                    }
                }
            }
            smoothScrollToPosition(safeTargetPosition(targetPosition, getItemCount()));
        }
    }

    public void addOnPageChangedListener(OnPageChangedListener listener)
    {
        if (mOnPageChangedListeners == null)
        {
            mOnPageChangedListeners = new ArrayList<>();
        }
        mOnPageChangedListeners.add(listener);
    }

    /***
     * adjust position before Touch event complete and fling action start.
     */
    protected void adjustPositionY(int velocityY)
    {
        if (reverseLayout) velocityY *= -1;

        int childCount = getChildCount();
        if (childCount > 0)
        {
            int curPosition = ViewUtils.getCenterYChildPosition(this);
            int childHeight = getHeight() - getPaddingTop() - getPaddingBottom();
            int flingCount = getFlingCount(velocityY, childHeight);
            int targetPosition = curPosition + flingCount;
            if (mSinglePageFling)
            {
                flingCount = Math.max(-1, Math.min(1, flingCount));
                targetPosition = flingCount == 0 ? curPosition : mPositionOnTouchDown + flingCount;
            }

            targetPosition = Math.max(targetPosition, 0);
            targetPosition = Math.min(targetPosition, getItemCount() - 1);
            if (targetPosition == curPosition
                    && (!mSinglePageFling || mPositionOnTouchDown == curPosition))
            {
                View centerYChild = ViewUtils.getCenterYChild(this);
                if (centerYChild != null)
                {
                    if (mTouchSpan > centerYChild.getHeight() * mTriggerOffset && targetPosition != 0)
                    {
                        if (!reverseLayout) targetPosition--;
                        else targetPosition++;
                    } else if (mTouchSpan < centerYChild.getHeight() * -mTriggerOffset && targetPosition != getItemCount() - 1)
                    {
                        if (!reverseLayout) targetPosition++;
                        else targetPosition--;
                    }
                }
            }
            smoothScrollToPosition(safeTargetPosition(targetPosition, getItemCount()));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && getLayoutManager() != null)
        {
            mPositionOnTouchDown = getLayoutManager().canScrollHorizontally()
                    ? ViewUtils.getCenterXChildPosition(this)
                    : ViewUtils.getCenterYChildPosition(this);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        // recording the max/min value in touch track
        if (e.getAction() == MotionEvent.ACTION_MOVE && mCurView != null)
        {
            mMaxLeftWhenDragging = Math.max(mCurView.getLeft(), mMaxLeftWhenDragging);
            mMaxTopWhenDragging = Math.max(mCurView.getTop(), mMaxTopWhenDragging);
            mMinLeftWhenDragging = Math.min(mCurView.getLeft(), mMinLeftWhenDragging);
            mMinTopWhenDragging = Math.min(mCurView.getTop(), mMinTopWhenDragging);
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void onScrollStateChanged(int state)
    {
        super.onScrollStateChanged(state);
        if (state == SCROLL_STATE_DRAGGING)
        {
            mNeedAdjust = true;
            mCurView = getLayoutManager().canScrollHorizontally()
                    ? ViewUtils.getCenterXChild(this) :
                    ViewUtils.getCenterYChild(this);
            if (mCurView != null)
            {
                if (mHasCalledOnPageChanged)
                {
                    // While rvp is scrolling, mPositionBeforeScroll will be previous value.
                    mPositionBeforeScroll = getChildLayoutPosition(mCurView);
                    mHasCalledOnPageChanged = false;
                }
                mFirstLeftWhenDragging = mCurView.getLeft();
                mFirstTopWhenDragging = mCurView.getTop();
            } else
                mPositionBeforeScroll = -1;
            mTouchSpan = 0;
        } else if (state == SCROLL_STATE_SETTLING)
        {
            mNeedAdjust = false;
            if (mCurView != null)
                if (getLayoutManager().canScrollHorizontally())
                    mTouchSpan = mCurView.getLeft() - mFirstLeftWhenDragging;
                else
                    mTouchSpan = mCurView.getTop() - mFirstTopWhenDragging;
            else
                mTouchSpan = 0;
            mCurView = null;
        } else if (state == SCROLL_STATE_IDLE)
        {
            if (mNeedAdjust)
            {
                int targetPosition = getLayoutManager().canScrollHorizontally()
                        ? ViewUtils.getCenterXChildPosition(this) :
                        ViewUtils.getCenterYChildPosition(this);
                if (mCurView != null)
                {
                    targetPosition = getChildAdapterPosition(mCurView);
                    if (getLayoutManager().canScrollHorizontally())
                    {
                        boolean leftToRight = isLeftToRightMode();
                        int spanX = mCurView.getLeft() - mFirstLeftWhenDragging;
                        if (spanX > mCurView.getWidth() * mTriggerOffset && mCurView.getLeft() >= mMaxLeftWhenDragging)
                            if (!reverseLayout)
                                targetPosition = leftToRight ? (targetPosition - 1) : (targetPosition + 1);
                            else
                                targetPosition = leftToRight ? (targetPosition + 1) : (targetPosition - 1);
                        else if (spanX < mCurView.getWidth() * -mTriggerOffset && mCurView.getLeft() <= mMinLeftWhenDragging)
                            if (!reverseLayout)
                                targetPosition = leftToRight ? (targetPosition + 1) : (targetPosition - 1);
                            else
                                targetPosition = leftToRight ? (targetPosition - 1) : (targetPosition + 1);
                    } else
                    {
                        int spanY = mCurView.getTop() - mFirstTopWhenDragging;
                        if (spanY > mCurView.getHeight() * mTriggerOffset && mCurView.getTop() >= mMaxTopWhenDragging)
                            if (!reverseLayout)
                                targetPosition--;
                            else
                                targetPosition++;
                        else if (spanY < mCurView.getHeight() * -mTriggerOffset && mCurView.getTop() <= mMinTopWhenDragging)
                            if (!reverseLayout)
                                targetPosition++;
                            else
                                targetPosition--;
                    }
                }
                smoothScrollToPosition(safeTargetPosition(targetPosition, getItemCount()));
                mCurView = null;
            } else if (mSmoothScrollTargetPosition != mPositionBeforeScroll)
                mPositionBeforeScroll = mSmoothScrollTargetPosition;
            // reset
            mMaxLeftWhenDragging = Integer.MIN_VALUE;
            mMinLeftWhenDragging = Integer.MAX_VALUE;
            mMaxTopWhenDragging = Integer.MIN_VALUE;
            mMinTopWhenDragging = Integer.MAX_VALUE;
        }
    }

    private int getFlingCount(int velocity, int cellSize)
    {
        if (velocity == 0)
            return 0;
        int sign = velocity > 0 ? 1 : -1;
        return (int) (sign * Math.ceil((velocity * sign * mFlingFactor / cellSize) - mTriggerOffset));
    }

    private int safeTargetPosition(int position, int count)
    {
        if (position < 0)
            return 0;
        if (position >= count)
            return count - 1;
        return position;
    }

    public interface OnPageChangedListener
    {
        /**
         * Fires when viewpager changes it's page
         *
         * @param oldPosition old position
         * @param newPosition new position
         */
        void OnPageChanged(int oldPosition, int newPosition);
    }
}
 No newline at end of file
```
**ViewUtilClass**
```java
import android.support.v7.widget.RecyclerView;
import android.view.View;

class ViewUtils
{

    /**
     * Get center child in X Axes
     */
    static View getCenterXChild(RecyclerView recyclerView)
    {
        int childCount = recyclerView.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                View child = recyclerView.getChildAt(i);
                if (isChildInCenterX(recyclerView, child))
                {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Get position of center child in X Axes
     */
    static int getCenterXChildPosition(RecyclerView recyclerView)
    {
        int childCount = recyclerView.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                View child = recyclerView.getChildAt(i);
                if (isChildInCenterX(recyclerView, child))
                {
                    return recyclerView.getChildAdapterPosition(child);
                }
            }
        }
        return childCount;
    }

    /**
     * Get center child in Y Axes
     */
    static View getCenterYChild(RecyclerView recyclerView)
    {
        int childCount = recyclerView.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                View child = recyclerView.getChildAt(i);
                if (isChildInCenterY(recyclerView, child))
                {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Get position of center child in Y Axes
     */
    static int getCenterYChildPosition(RecyclerView recyclerView)
    {
        int childCount = recyclerView.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                View child = recyclerView.getChildAt(i);
                if (isChildInCenterY(recyclerView, child))
                {
                    return recyclerView.getChildAdapterPosition(child);
                }
            }
        }
        return childCount;
    }

    private static boolean isChildInCenterX(RecyclerView recyclerView, View view)
    {
        int childCount = recyclerView.getChildCount();
        int[] lvLocationOnScreen = new int[2];
        int[] vLocationOnScreen = new int[2];
        recyclerView.getLocationOnScreen(lvLocationOnScreen);
        int middleX = lvLocationOnScreen[0] + recyclerView.getWidth() / 2;
        if (childCount > 0)
        {
            view.getLocationOnScreen(vLocationOnScreen);
            if (vLocationOnScreen[0] <= middleX && vLocationOnScreen[0] + view.getWidth() >= middleX)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isChildInCenterY(RecyclerView recyclerView, View view)
    {
        int childCount = recyclerView.getChildCount();
        int[] lvLocationOnScreen = new int[2];
        int[] vLocationOnScreen = new int[2];
        recyclerView.getLocationOnScreen(lvLocationOnScreen);
        int middleY = lvLocationOnScreen[1] + recyclerView.getHeight() / 2;
        if (childCount > 0)
        {
            view.getLocationOnScreen(vLocationOnScreen);
            if (vLocationOnScreen[1] <= middleY && vLocationOnScreen[1] + view.getHeight() >= middleY)
            {
                return true;
            }
        }
        return false;
    }
}
 No newline at end of file
```



