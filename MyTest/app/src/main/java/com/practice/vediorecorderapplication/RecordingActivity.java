package com.practice.vediorecorderapplication;

import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.practice.videorecordermodel.manage.ScreenRecorder;
import com.practice.videorecordermodel.manage.VideoManage;

import java.io.File;
import java.util.UUID;

public class RecordingActivity extends AppCompatActivity implements View.OnClickListener {
    
    private Button mStartButton;
    private Button mEndButton;
    private VideoManage mVideoManage;
    private ScreenRecorder mScreenRecorder;
    
    String[] permissions = {
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
    };
    private boolean mRead = false;//是否准备好录像
    private boolean isRecording = false;//是否处于录像的状态
    //记录录制时间
    private float mTime = 0;
    private long mStartTime = 0;

    //屏幕录制工具（android5.0推出的录制屏幕的工具）
    private MediaProjectionManager mMediaProjectionManager;
    SurfaceView mFloatView;
    private String mCurrentScreenFileName;
    
/*private String[] myCheckSelfPermissions(String[] permissions){
    List<String> list = new ArrayList<>();
    for (int i = 0;i < permissions.length;i++){
        if(ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED){
            list.add( permissions[i]);
        }
    }
    String[] array = new String[list.size()];
    for (int i = 0;i < list.size();i++){
        array[i] = list.get(i);
    }
    return array;
}*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("getAndroidSDKVersion",getAndroidSDKVersion() + "");
       /* if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            String[] mList = myCheckSelfPermissions(permissions);
            if(mList != null && mList.length > 0){
                //申请SYSTEM_ALERT_WINDOW权限
                ActivityCompat.requestPermissions(this, mList, 0);
            }
        }*/
        setContentView(R.layout.activity_recording);
        init();
    }

    private void init() {
        mStartButton = (Button)findViewById(R.id.StartButton1);
        mEndButton = (Button)findViewById(R.id.EndButton1);
        mFloatView = (SurfaceView)findViewById(R.id.floatSurfaceView);

        mStartButton.setOnClickListener(this);
        mEndButton.setOnClickListener(this);
        
        mVideoManage = VideoManage.getInstance();
//      mVideoManage.setFileDir(); //这个方法设置录制文件的存放路径
        mVideoManage.setSurfaceHolder(mFloatView.getHolder());
    }
    
    public void onRecordingFinish(String fileName) {//录像结束时的回调函数
        Intent mIntent = new Intent();
        mIntent.putExtra("videoFile", fileName);
        //录屏结束
        if (mCurrentScreenFileName != null && !mCurrentScreenFileName.isEmpty()) {
            mIntent.putExtra("screenFile", mCurrentScreenFileName);
        }
        setResult(MainActivity.SCREEN_VIDEO_CODE, mIntent);
        finish();
    }
    
    @Override
    public void onClick(View v) {//点击开始 录屏+录像
        if (v.getId() == R.id.StartButton1 && !isRecording){
            isRecording = true;
            mRead = true;
            mVideoManage.prepareVideo();//开始 录像
            mStartTime = System.currentTimeMillis();

            if(getAndroidSDKVersion() >= 21){//开始 录屏
                mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, MainActivity.SCREEN_CEDE);
            }else {
                Toast.makeText(this,"设备的版本低于5.0，不能录制屏幕",Toast.LENGTH_SHORT).show();
            }
        }
        
        if (v.getId() == R.id.EndButton1){//点击结束  录屏+录像
            endRecordScreeen();//录屏结束
            endRecordVideo();//录像结束
        }
    }

    private void endRecordScreeen() {//录屏结束
        if (mScreenRecorder != null) {
            Toast.makeText(this,"录屏结束",Toast.LENGTH_SHORT).show();
            mScreenRecorder.quit();
            mScreenRecorder = null;
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
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null){
            return;
        }
        if (requestCode == MainActivity.SCREEN_CEDE) {        //录屏的回掉
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e("@@", "media projection is null");
                return;
            }
            // 录制视频尺寸
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            final int width = profile.videoFrameHeight;
            final int height = profile.videoFrameWidth;
            File file = createMFile();//创建保存录制屏幕视频的文件
            final int bitrate = 6000000;
            mScreenRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
            mScreenRecorder.start();
            Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
            //此APP是否在后台运行  即：是否跳转到Home页面
            moveTaskToBack(false);
        }
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0 ;i < permissions.length;i++){
            System.out.println("grantResults:"+grantResults.toString());
            System.out.println("grantResults:"+permissions.toString());
        } 
        if (permissions[0].equals(Manifest.permission.CAMERA)
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(RecordingActivity.this, "权限申请成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(RecordingActivity.this, "获取权限失败", Toast.LENGTH_SHORT).show();
            if (!ActivityCompat.shouldShowRequestPermissionRationale(RecordingActivity.this, Manifest.permission.CALL_PHONE)) {
                Toast.makeText(this,"请到设置里开启该权限",Toast.LENGTH_SHORT).show();
            }
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScreenRecorder != null) {
            mScreenRecorder.quit();
            mScreenRecorder = null;
        }
    }
    
    /**
     * 生成随机文件
     * @return
     */
    private File createMFile() {
        //设置文件存放目录
        String fileDir = Environment.getExternalStorageDirectory() + "/videoRecorder";
        File dir = new File(fileDir);
        //判断文件夹是否存在 如不存在就创建
        if (!dir.exists()) {
            dir.mkdir();
        }
        String fileName = generateFileName();
        File mFile = new File(dir, fileName);
        mCurrentScreenFileName = mFile.getAbsolutePath();
        return mFile;
    }

    /**
     * 随机产生文件名
     *
     * @return
     */
    private String generateFileName() {
        return UUID.randomUUID().toString() + ".mp4";
    }
    //获得当前使用的手机版本
    public static int getAndroidSDKVersion() {
        int version = 0;
        try {
            version = Integer.valueOf(android.os.Build.VERSION.SDK);
        } catch (NumberFormatException e) {
            Log.i("errTag", e.toString());
        }
        return version;
    }
}