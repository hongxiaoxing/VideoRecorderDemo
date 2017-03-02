package com.practice.vediorecorderapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.practice.videorecordermodel.manage.VideoManage;

public class RecordingVideoActivity extends AppCompatActivity implements View.OnClickListener {
    
    private Button mStartButton;
    private Button mEndButton;
    private VideoManage mVideoManage;
    private boolean mRead = false;//是否准备好录像
    private boolean isRecording = false;//是否处于录像的状态
    //记录录制时间
    private float mTime = 0;
    private long mStartTime = 0;
    SurfaceView mFloatView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordingvideo);
        init();
    }

    private void init() {
        mStartButton = (Button)findViewById(R.id.StartButton3);
        mEndButton = (Button)findViewById(R.id.EndButton3);
        mFloatView = (SurfaceView)findViewById(R.id.floatSurfaceView3);

        mStartButton.setOnClickListener(this);
        mEndButton.setOnClickListener(this);
        
        mVideoManage = VideoManage.getInstance();
//      mVideoManage.setFileDir(); //这个方法设置录制文件的存放路径
        mVideoManage.setSurfaceHolder(mFloatView.getHolder());
    }
    
    public void onRecordingFinish(String fileName) {//录像结束时的回调函数
        Intent mIntent = new Intent();
        mIntent.putExtra("videoFile", fileName);
        setResult(MainActivity.VIDEO_CODE, mIntent);
        finish();
    }
    
    @Override
    public void onClick(View v) {//点击开始 录像
        if (v.getId() == R.id.StartButton3 && !isRecording){
            Toast.makeText(this,"开始录像",Toast.LENGTH_SHORT).show();
            isRecording = true;
            mRead = true;
            mVideoManage.prepareVideo();//开始 录像
            mStartTime = System.currentTimeMillis();
        }
        
        if (v.getId() == R.id.EndButton3){//点击结束  录像
            endRecordVideo();//录像结束
            Toast.makeText(this,"结束录像",Toast.LENGTH_SHORT).show();
        }
    }
    

    private void endRecordVideo() {//录像结束
        mTime = (System.currentTimeMillis() - mStartTime) / 1000f;
        Log.e("time", mTime + "");
        if (!mRead) {                                     //没准备好
            reset();//重置
        }
        if (!isRecording || mTime < 0.6f) {               //录制时间太短或prepare方法未完成
            Toast.makeText(this, com.practice.videorecordermodel.R.string.time_is_too_short, Toast.LENGTH_SHORT).show();
            mVideoManage.cancel();//取消录像 删除文件
        } else {    //录制成功
            mVideoManage.release();// 释放资源
            //回掉录制完成接口
            onRecordingFinish(mVideoManage.getCurrentFileName());
        }
        reset();//重置
        isRecording = false;
    }

    private void reset() {
        mTime = 0;
        mRead = false;
        isRecording = false;
    }
}