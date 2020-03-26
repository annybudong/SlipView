package com.github.annybudong.slipview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class SlipView extends ViewGroup {
    public interface OnScrollListener {

        void onScrollStart();

        void onScrollEnd();
    }

    private Scroller scroller;
    private VelocityTracker velocityTracker;

    private int lastX;          //最近一次MotionEvent的x坐标
    private int lastY;          //最近一次MotionEvent的y坐标
    private int menuWidth;      //菜单宽度
    private int touchSlop;      //超过此距离，认为手指正在滑动

    private boolean scrollable = true;                      //是否允许滚动
    private boolean isScrolling = false;                    //是否正在滚动
    private boolean hasConsumeDownEventByChild = true;      //child是否消费了down事件

    private SlipView.OnScrollListener onScrollListener;

    public SlipView(Context context) {
        this(context, null);
    }

    public SlipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scroller = new Scroller(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * @param touchSlop 滚动阈值，水平方向滚动超过此值才会被判断用户在水平滚动，谨慎设置，建议取值在2-8之间
     */
    public void setTouchSlop(int touchSlop) {
        this.touchSlop = touchSlop;
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() <= 1) {
            throw new AssertionError("The child-count of SlipView must be greater than 1!");
        }

        View firstChild = getChildAt(0);
        MarginLayoutParams params = (MarginLayoutParams) firstChild.getLayoutParams();
        if (params.width != LayoutParams.MATCH_PARENT) {
            throw new AssertionError("The width of the first child in SlipView must be MATCH_PARENT!");
        }

        if (firstChild.getVisibility() == View.GONE) {
            throw new AssertionError("The visibility of the first child of SlipView can not be GONE!");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);      //MeasureSpec.EXACTLY对应match_parent和100dp
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);    //MeasureSpec.AT_MOST对应wrap_content

        if (widthMode == MeasureSpec.AT_MOST) {
            //SlipView的宽度不应该是wrap_content，否则侧滑菜单可能无法隐藏
            throw new AssertionError("The width of SlipView can not be wrap_content!");
        }

        int height = 0;
        menuWidth = 0;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            if (i == 0) {
                int dstWidth = child.getMeasuredWidth() - lp.leftMargin - lp.rightMargin;
                int childWidthSpec = MeasureSpec.makeMeasureSpec(dstWidth, MeasureSpec.EXACTLY);
                int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
                child.measure(childWidthSpec, childHeightSpec);
            }
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            height = Math.max(height, childHeight);
            if (i > 0) {
                menuWidth += childWidth;
            }
        }

        setMeasuredDimension(widthSize, heightMode == MeasureSpec.EXACTLY ? heightSize : height + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int lc = left + lp.leftMargin;
            int tc = top + lp.topMargin;
            int rc = lc + child.getMeasuredWidth();
            int bc = tc + child.getMeasuredHeight();
            child.layout(lc, tc, rc, bc);

            left += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        }
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

                lastY = y;
                lastX = x;
                break;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //如果禁止滚动，或者正在滚动，那么不要自己处理event事件了。
        if (!scrollable || isScrolling) {
            return super.onTouchEvent(event);
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
                } else if (targetScrollX > menuWidth) {
                    scrollTo(menuWidth, 0);
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
                    dx = menuWidth - getScrollX();
                } else if (xVelocity > 0) {
                    dx = -getScrollX();
                } else if (getScrollX() >= 80){
                    dx = menuWidth - getScrollX();
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

    public void setOnScrollListener(SlipView.OnScrollListener listener) {
        this.onScrollListener = listener;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }
}
