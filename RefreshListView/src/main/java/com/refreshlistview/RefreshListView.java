package com.atguigu.refreshlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import java.text.SimpleDateFormat;
import java.util.Date;

public class RefreshListView extends ListView {

    /**
     * 下拉刷新和顶部轮播图（还没有加载进来）
     */
    private LinearLayout headerView;

    /**
     * 下拉刷新控件
     */
    private View ll_pull_down_refresh;

    private ImageView iv_header_arrow;

    private ProgressBar pb_header_state;

    private TextView tv_header_state;

    private TextView tv_header_time;
    /**
     * 下拉刷新空间的高
     */
    private int pullDownRefreshHeight;
    /**
     * 顶部轮播图部分
     */
    private View topnewsview;
    /**
     * ListView在屏幕上Y轴的坐标
     */
    private int listViewOnScreenY = -1;

    /**
     * 下拉刷新状态
     */
    public static final int PULL_DOWN_REFRESH = 1;

    /**
     * 手松刷新
     */
    public static final int RELEASE_REFRESH = 2;


    /**
     * 正在刷新
     */
    public static final int REFRESHING = 3;


    /**
     * 当前的状态
     */
    private int currentState = PULL_DOWN_REFRESH;

    private Animation upAnim;

    private Animation downAnim;

    /**
     * 加载更多的视图
     */
    private View refresh_footer;
    /**
     * 加载更多控件的高
     */
    private int footViewHeight;
    /**
     * 是否已经加载更多
     */
    private boolean isLoadMore = false;

    public RefreshListView(Context context) {
        this(context, null);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initHeaderView(context);
        initAnimation();
        initFootView(context);
    }

    /**
     * 初始化加载更多的布局
     *
     * @param context
     */
    private void initFootView(Context context) {
        refresh_footer = View.inflate(context, R.layout.refresh_footer, null);

        refresh_footer.measure(0, 0);
        footViewHeight = refresh_footer.getMeasuredHeight();

        refresh_footer.setPadding(0, -footViewHeight, 0, 0);


        //添加到ListView的脚部
        addFooterView(refresh_footer);


        //监听ListVew的滚动
        setOnScrollListener(new MyOnScrollListener());

    }

    class MyOnScrollListener implements OnScrollListener{

        /**
         * 当ListView滚动状态发生变化的时候回调这个方法
         * 静止->滑动
         * 滑动-->惯性滚动
         * 惯性滚动-静止
         * @param view
         * @param scrollState
         */
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

            if(scrollState == SCROLL_STATE_IDLE || scrollState ==SCROLL_STATE_FLING){

                if(getLastVisiblePosition() ==getCount()-1){

                    //1.把加载更多UI显示出来
                    refresh_footer.setPadding(10,10,10,10);

                    //2.设置状态
                    isLoadMore = true;

                    //3.调用接口
                    if(onRefreshListener != null){
                        onRefreshListener.onLoadMore();
                    }

                }

            }

        }

        /**
         * 当滚动额时候回调这个方法
         * @param view
         * @param firstVisibleItem
         * @param visibleItemCount
         * @param totalItemCount
         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }
    }


    /**
     * 初始化动画
     */
    private void initAnimation() {
        upAnim = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        upAnim.setDuration(500);
        upAnim.setFillAfter(true);

        downAnim = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        downAnim.setDuration(500);
        downAnim.setFillAfter(true);
    }

    /**
     * 初始化下拉刷新
     *
     * @param context
     */
    private void initHeaderView(Context context) {
        headerView = (LinearLayout) View.inflate(context, R.layout.refresh_header, null);
        ll_pull_down_refresh = headerView.findViewById(R.id.ll_pull_down_refresh);
        iv_header_arrow = (ImageView) headerView.findViewById(R.id.iv_header_arrow);
        pb_header_state = (ProgressBar) headerView.findViewById(R.id.pb_header_state);
        tv_header_state = (TextView) headerView.findViewById(R.id.tv_header_state);
        tv_header_time = (TextView) headerView.findViewById(R.id.tv_header_time);


        //显示下拉刷新空间

//        View.setPading(0,-控件的高，0,0);//控件完成隐藏
//        View.setPading(0,0，0,0);//完全显示
//        View.setPading(0,控件的高，0,0);//2倍高显示下拉刷新控件

        ll_pull_down_refresh.measure(0, 0);
        pullDownRefreshHeight = ll_pull_down_refresh.getMeasuredHeight();//
        ll_pull_down_refresh.setPadding(0, -pullDownRefreshHeight, 0, 0);


        //添加到头部
        this.addHeaderView(headerView);

    }


    private float startY;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //1.记录起始坐标
                startY = ev.getY();
                break;

            case MotionEvent.ACTION_MOVE:

                //如果正在刷新，就不让下拉
                if (currentState == REFRESHING) {
                    break;
                }


                //判断顶部轮播图是否完全显示
                boolean isDisplayTopNews = isDisplayTopNews();

                if (!isDisplayTopNews) {
                    //下拉刷新不需要执行
                    break;
                }

                //2.来到新的坐标
                float endY = ev.getY();
                //3.计算偏移量
                float distanceY = endY - startY;


                if (distanceY > 0) {

                    int padingTop = (int) (-pullDownRefreshHeight + distanceY);

                    if (padingTop > 0 && currentState != RELEASE_REFRESH) {

                        currentState = RELEASE_REFRESH;
                        //更新UI
                        refreshHeaderState();
                    } else if (padingTop < 0 && currentState != PULL_DOWN_REFRESH) {
                        currentState = PULL_DOWN_REFRESH;
                        //更新UI
                        refreshHeaderState();
                    }

                    ll_pull_down_refresh.setPadding(0, padingTop, 0, 0);//控件完成隐藏
                }
                break;

            case MotionEvent.ACTION_UP:
                //重新赋值
                startY = 0;
                if (currentState == PULL_DOWN_REFRESH) {
                    //隐藏
                    ll_pull_down_refresh.setPadding(0, -pullDownRefreshHeight, 0, 0);
                } else if (currentState == RELEASE_REFRESH) {

                    currentState = REFRESHING;
                    refreshHeaderState();
                    ll_pull_down_refresh.setPadding(0, 0, 0, 0);

                    //调用接口
                    if (onRefreshListener != null) {
                        onRefreshListener.onPullDownRefresh();
                    }
                }

                break;
        }
        return super.onTouchEvent(ev);
    }

    private void refreshHeaderState() {
        switch (currentState) {
            case PULL_DOWN_REFRESH://下拉刷新
                tv_header_state.setText("下拉刷新...");
                iv_header_arrow.startAnimation(downAnim);
                pb_header_state.setVisibility(View.GONE);
                iv_header_arrow.setVisibility(VISIBLE);

                break;
            case RELEASE_REFRESH://手松刷新
                tv_header_state.setText("手松刷新...");
                iv_header_arrow.startAnimation(upAnim);
                pb_header_state.setVisibility(View.GONE);
                iv_header_arrow.setVisibility(VISIBLE);

                break;

            case REFRESHING://正在刷新

                tv_header_state.setText("正在刷新...");
                iv_header_arrow.clearAnimation();
                pb_header_state.setVisibility(View.VISIBLE);
                iv_header_arrow.setVisibility(GONE);

                break;
        }
    }

    /**
     * 判断顶部轮播图是否完全显示
     * 当ListView在屏幕上的Y轴坐标小于或者等于顶部轮播图在屏幕上Y轴坐标的时候，那么顶部轮播完全显示
     *
     * @return
     */
    private boolean isDisplayTopNews() {

        if(topnewsview != null){
            int[] location = new int[2];
            //1.计算ListView在屏幕上的Y轴的坐标
            if (listViewOnScreenY == -1) {
                getLocationOnScreen(location);
                listViewOnScreenY = location[1];
            }

            //2.计算顶部轮播图部分在屏幕上的Y轴的坐标
            topnewsview.getLocationOnScreen(location);

            int topNewsOnScreenY = location[1];

            return listViewOnScreenY <= topNewsOnScreenY;
        }

        return true;

    }

    /**
     * 添加顶部轮播图到 headerView
     *
     * @param topnewsview
     */
    public void addTopNews(View topnewsview) {
        this.topnewsview = topnewsview;

        if (topnewsview != null) {

            headerView.addView(topnewsview);
        }

    }

    /**
     * 还原状态
     *
     * @param success
     */
    public void onFinishRefrsh(boolean success) {
        if(isLoadMore){
            //加载更多
            isLoadMore = false;
            refresh_footer.setPadding(0,-footViewHeight,0,0);
        }else{

            //下拉刷新
            currentState = PULL_DOWN_REFRESH;
            ll_pull_down_refresh.setPadding(0, -pullDownRefreshHeight, 0, 0);
            pb_header_state.setVisibility(GONE);
            tv_header_state.setText("下拉刷新...");

            if (success) {
                tv_header_time.setText("上次更新时间:" + getSystemTime());
            }
        }


    }

    /**
     * 得到系统时间
     *
     * @return
     */
    private String getSystemTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }


    /**
     * 自定义下拉刷新接口
     */
    public interface OnRefreshListener {

        /**
         * 当下拉刷新的时候回调这个方法
         */
        public void onPullDownRefresh();

        /**
         当加载更多数据的时候回调这个方法
         */
        public void onLoadMore();

    }


    private OnRefreshListener onRefreshListener;


    /**
     * 设置监听页面的刷新
     */
    public void setOnRefreshListener(OnRefreshListener l) {

        this.onRefreshListener = l;
    }
}
