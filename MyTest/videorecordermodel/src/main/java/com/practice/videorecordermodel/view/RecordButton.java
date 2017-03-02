package com.practice.videorecordermodel.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.practice.videorecordermodel.R;
import com.practice.videorecordermodel.manage.ProgressDialogManage;
import com.practice.videorecordermodel.manage.VideoManage;

/**
 * 〈用于录制视屏的Button〉
 * 〈按住录制视频，松开发送,上滑取消〉
 *
 * @author XLC
 * @version [1.0, 2016/2/23]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class RecordButton extends Button implements VideoManage.videoPreparedListener {

    private final static int STATE_WANT_TO_CANCEL = 0;
    private final static int STATE_RECORDING = 1;
    private final static int STATE_NORMAL = 2;

    //手指移动的距离
    private static final int DISTANCE = 50;

    private boolean isRecording = false;

    private boolean mRead = false;

    private int mCurrentState = STATE_NORMAL;

    //记录录制时间
    private float mTime = 0;

    //规定的录制最长时间为6秒(6f)
    private static float MAX_TIME = 3600f;

    private onRecordingFinishListener mFinishListener;

    private VideoManage mVideoManage;

    private ProgressDialogManage dialogManage;

    private Toast mToast;

    //录制完成回掉接口
    public interface onRecordingFinishListener {
        void onRecordingFinish(float seconds, String fileName);
    }

    public void setRecordingFinishListener(onRecordingFinishListener listener) {
        mFinishListener = listener;
    }

    /**
     * 设置最长录制时间 (默认为6秒)
     */
    public void setLongestRecordingTime(float recordingTime) {
        MAX_TIME = recordingTime + 1f;
    }

    public static float getMaxTime() {
        return MAX_TIME;
    }

    public RecordButton(Context context) {
        this(context, null);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mVideoManage = VideoManage.getInstance();

        dialogManage = new ProgressDialogManage(context);

        mVideoManage.setVideoPreparedListener(this);
        //长按开始录制
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //TODO
                mRead = true;
                dialogManage.showProgress();//显示并更新录制的进度条
                mVideoManage.prepareVideo();//开始录制视频
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:

                stateChange(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRecording) {
                    if (wantCancel(x, y)) {
                        stateChange(STATE_WANT_TO_CANCEL);
                    } else {
                        stateChange(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.e("time", mTime + "");
                if (!mRead) {                                     //没准备好
                    reset();
                    return super.onTouchEvent(event);
                }
                if (!isRecording || mTime < 0.6f) {               //录制时间太短或prepare方法未完成
                    tooShort();
                    mVideoManage.cancel();
                    dialogManage.dismissDialog();
                } else if (mCurrentState == STATE_RECORDING) {    //录制成功
                    mVideoManage.release();// 释放资源
                    if (mFinishListener != null) {
                        dialogManage.dismissDialog();
                        //回掉录制完成接口
                        mFinishListener.onRecordingFinish(mTime, mVideoManage.getCurrentFileName());
                    }

                } else if (mCurrentState == STATE_WANT_TO_CANCEL) {//用户上划取消
                    Log.e("touch", "cancel");
                    mVideoManage.cancel();
                    dialogManage.dismissDialog();
                }
                reset();
                break;
        }

        return super.onTouchEvent(event);
    }

    private void reset() {
        mTime = 0;
        mRead = false;
        isRecording = false;
        stateChange(STATE_NORMAL);
    }

    /**
     * 根据x，y坐标判断是否要取消发送视频
     */
    private boolean wantCancel(int x, int y) {
//        if (x < 0 || x > getWidth()) {
//            return true;
//        }
        if (y < -DISTANCE || y > getHeight() + DISTANCE) {
            return true;
        }
        return false;
    }

    /**
     * 状态改变时做的操作
     *
     * @param state
     */
    private void stateChange(int state) {
        if (mCurrentState != state) {
            mCurrentState = state;
        }
        switch (state) {
            case STATE_NORMAL:         //平常的状态
                setText(R.string.press_to_produce);
                break;
            case STATE_WANT_TO_CANCEL: //取消

                //此状态时显示松开取消
                dialogManage.showAlertView();
                setText(R.string.cancel_recording);
                break;
            case STATE_RECORDING:      //录制
                setText(R.string.up_to_post);
                dialogManage.showSlideAlertView();
                break;
        }

    }

    private static final int MSG_VIDEO_PREPARED = 0x110;      //开始录制
    private static final int MSG_VIDEO_TIME_CHANGE = 0x111;   //正在录制
    private static final int MSG_DIALOG_VIDEO_DISMISS = 0x112;//录制结束

    /**
     * 消息回掉
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_VIDEO_PREPARED:
                    isRecording = true;

                    new Thread(timerRunnable).start();
                    break;
                case MSG_VIDEO_TIME_CHANGE:      //时间改变时更新progress的状态
                    dialogManage.updateProgress((int) mTime);
                    break;
                case MSG_DIALOG_VIDEO_DISMISS:
                    dialogManage.dismissDialog();
                    reset();
                    break;
            }
        }
    };

    //录制准备完毕
    @Override
    public void wellPrepared() {
        handler.sendEmptyMessage(MSG_VIDEO_PREPARED);
    }

    /**
     * 录制时间太短
     */
    public void tooShort() {
        if (mToast == null) {
            mToast = Toast.makeText(getContext(), R.string.time_is_too_short, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }
    

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRecording) {
                try {
                    Thread.sleep(100);
                    mTime += 0.1f;
                    handler.sendEmptyMessage(MSG_VIDEO_TIME_CHANGE);
                    if (mTime > MAX_TIME) {  //达到录制规定最大时间结束录制
                        if (mFinishListener != null && mCurrentState == STATE_RECORDING) {
                            //回掉录制完成接口
                            handler.sendEmptyMessage(MSG_DIALOG_VIDEO_DISMISS);
                            mVideoManage.release();
                            mFinishListener.onRecordingFinish(mTime, mVideoManage.getCurrentFileName());
                        } else if (mCurrentState == STATE_WANT_TO_CANCEL) {
                            //达到录制最大时间时选择时  是取消录制状态
                            handler.sendEmptyMessage(MSG_DIALOG_VIDEO_DISMISS);
                            Log.e("Thread", "cancel");
                            mVideoManage.cancel();

                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
