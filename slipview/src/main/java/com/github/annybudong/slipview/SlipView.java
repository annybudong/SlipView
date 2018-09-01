package com.github.annybudong.slipview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

public class SlipView extends LinearLayout {

    public interface OnScrollListener {

        void onScrollStart();

        void onScrollEnd();
    }

    private Scroller scroller;
    private VelocityTracker velocityTracker;

    private int lastX;          //最近一次MotionEvent的x坐标
    private int lastY;          //最近一次MotionEvent的y坐标
    private int menuDistance;   //菜单宽度
    private int touchSlop;      //超过此距离，认为手指正在滑动

    private boolean inited = false;                         //是否已初始化
    private boolean scrollable = true;                      //是否允许滚动
    private boolean isScrolling = false;                    //是否正在滚动
    private boolean hasConsumeDownEventByChild = true;      //child是否消费了down事件

    private OnScrollListener onScrollListener;

    public SlipView(Context context) {
        super(context);
        init(context);
    }

    public SlipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (!inited) {
            scroller = new Scroller(context);
            inited = true;
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getOrientation() != HORIZONTAL) {
            throw new AssertionError("The orientation of SlipView must be HORIZONTAL! Please check your layout file.");
        }

        if (getChildAt(0).getLayoutParams().width != LayoutParams.MATCH_PARENT) {
            throw new AssertionError("The width of first child in SlipView must be MATCH_PARENT! Please check your layout file.");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //onMeasure会调用多次，所以这段代码也会执行多次，但是最后一次计算结果必然是正确的。
        int childCount = getChildCount();
        int sumDistance = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            sumDistance += child.getMeasuredWidth();
        }

        /**
         * sumDistance为SlipView在水平方向的总长度，getMeasuredWidth()为SlipView
         * 显示的长度，两者相减则是隐藏在右侧的菜单的长度。
         */
        menuDistance = sumDistance - getMeasuredWidth();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!scrollable) {
            return super.onInterceptTouchEvent(event);
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        trackEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = x - lastX;
                int deltaY = y - lastY;

                if (!isScrolling && Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY) ) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }

                lastX = x;
                break;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //如果禁止滚动，或者正在滚动，那么不要自己处理event事件了。
        if (!scrollable || isScrolling) {
            return super.onInterceptTouchEvent(event);
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        trackEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                /**
                 * 如果down事件走到了SlipView的onTouchEvent，说明SlipView的子View并没有消费down事件，其子View的clickable为false
                 * 而且没有setOnClickListener设置过点击监听。由于子View没有消费down事件，那么后续的move事件不会传递到子View，所以
                 * SlipView的onInterceptTouchEvent就不会再进行拦截了（down事件进来后，move事件就不会来了，所以根本不会走到我们代码
                 * 拦截那里）。
                 *
                 * 由于不会走到我们在onInterceptTouchEvent拦截代码那里，所以我们要在onTouchEvent进行拦截，拦截之后要求parent不要拦截
                 * 后续的事件，把后续的事件都发到SlipView。
                 */
                lastX = x;
                lastY = y;
                hasConsumeDownEventByChild = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = lastX - x;
                int deltaY = lastY - y;

                //如果子View没有消费down事件，则在此处继续拦截并请求SlipView的父类不要拦截，把事件统统传递到SlipView上来进行滑动。
                if (!hasConsumeDownEventByChild && Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY) ) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                int targetScrollX = getScrollX() + deltaX;
                if (targetScrollX <= 0) {
                    scrollTo(0, 0);
                } else if (targetScrollX > menuDistance) {
                    scrollTo(menuDistance, 0);
                } else {
                    scrollBy(deltaX, 0);
                }
                lastX = x;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                int dx;
                hasConsumeDownEventByChild = true;
                int xVelocity = getXVelocity();          //通过加速度方向来判断手指抬起时正在朝哪边滚动
                if (xVelocity < 0) {
                    dx = menuDistance - getScrollX();
                } else if (xVelocity > 0) {
                    dx = -getScrollX();
                } else if (getScrollX() >= 80){
                    dx = menuDistance - getScrollX();
                } else {
                    dx = -getScrollX();
                }

                if (onScrollListener != null) {
                    isScrolling = true;
                    onScrollListener.onScrollStart();
                }
                scroller.startScroll(getScrollX(), 0, dx, 0);
                invalidate();

                clearTracker();
                break;
        }

        return true;
    }

    private void trackEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }

    private int getXVelocity() {
        velocityTracker.computeCurrentVelocity(1000);
        return (int) velocityTracker.getXVelocity();
    }

    private void clearTracker() {
        velocityTracker.recycle();
        velocityTracker = null;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();
        } else if (onScrollListener != null && isScrolling) {
            isScrolling = false;
            onScrollListener.onScrollEnd();
        }
    }

    /**
     * 关闭侧滑菜单（没有动画）
     *
     * @param duration 单位ms
     */
    public void closeMenu(int duration) {
        if (duration == 0) {
            scrollTo(0, 0);
        } else {
            int x = getScrollX();
            scroller.startScroll(x, 0, -x, 0, duration);
            invalidate();
        }
    }

    /**
     * 是否允许侧滑，默认为true
     * @param scrollable ture-允许 false-禁止
     */
    public void enableScroll(boolean scrollable) {
        this.scrollable = scrollable;
    }

    /**
     * 侧滑菜单是否正在显示
     * @return
     */
    public boolean isMenuShowing() {
        return getScrollX() > 0 ? true : false;
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.onScrollListener = listener;
    }
}
