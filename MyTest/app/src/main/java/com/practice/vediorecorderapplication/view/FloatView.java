package com.practice.vediorecorderapplication.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;

import com.practice.vediorecorderapplication.R;

public class FloatView {
    private static WindowManager mWindowManager;
    private static RelativeLayout mFloatLayout;

    public static WindowManager getWindowManager() {
        return mWindowManager;
    }

    public static RelativeLayout getFloatLayout() {
        return mFloatLayout;
    }

    private static void setWindowManager(WindowManager mWindowManager) {
        FloatView.mWindowManager = mWindowManager;
    }

    private static void setFloatLayout(RelativeLayout mFloatLayout) {
        FloatView.mFloatLayout = mFloatLayout;
    }

    public static void createFloatView(final Context ctx) {
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper  
        final WindowManager mWindowManager = (WindowManager)ctx.getSystemService(ctx.WINDOW_SERVICE);
        //设置window type  
        wmParams.type = LayoutParams.TYPE_PHONE;
        //设置图片格式，效果为背景透明  
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）  
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶  
        wmParams.gravity = Gravity.CENTER;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity  
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据    
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        LayoutInflater inflater = LayoutInflater.from(ctx);
        //获取浮动窗口视图所在布局  
        final RelativeLayout mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.float_layout, null);
        //todo 悬浮播放画面处理
        
        //添加mFloatLayout  
        mWindowManager.addView(mFloatLayout, wmParams);
        //浮动窗口按钮  
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        setWindowManager(mWindowManager);
        setFloatLayout(mFloatLayout);
    }
}