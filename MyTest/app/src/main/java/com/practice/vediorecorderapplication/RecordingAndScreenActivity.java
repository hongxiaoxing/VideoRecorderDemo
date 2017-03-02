package com.practice.vediorecorderapplication;

import android.Manifest;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.CamcorderProfile;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.practice.videorecordermodel.manage.ScreenRecorder;
import com.practice.videorecordermodel.manage.VideoManage;

import java.io.File;
import java.util.UUID;

public class RecordingAndScreenActivity extends AppCompatActivity implements View.OnClickListener {
    
    private Button mStartButton;

    private Button mEndButton;

    private VideoManage mVideoManage;

    String[] permissions = {
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private boolean mRead = false;//是否准备好录像
    private boolean isRecording = false;//是否处于录像的状态
    //记录录制时间
    private float mTime = 0;
    private long mStartTime = 0;

    //屏幕录制工具（android5.0推出的录制屏幕的工具）
    private MediaProjectionManager mMediaProjectionManager;
    
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
        /*if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED){
            String[] mList = myCheckSelfPermissions(permissions);
            if(mList != null && mList.length > 0){
                //申请SYSTEM_ALERT_WINDOW权限
                ActivityCompat.requestPermissions(this, mList, 0);
            }
        }*/
        createFloatView();
        setContentView(R.layout.activity_recording_and_screen);
        init();
    }

    private void init() {
        mStartButton = (Button)findViewById(R.id.StartButton2);
        mEndButton = (Button)findViewById(R.id.EndButton2);

        mStartButton.setOnClickListener(this);
        mEndButton.setOnClickListener(this);
        
        mVideoManage = VideoManage.getInstance();
        // mVideoManage.setFileDir(); //这个方法设置录制文件的存放路径
        mVideoManage.setSurfaceHolder(mFloatView.getHolder());
    }
 
    public void onRecordingFinish(String fileName) {//录像结束时,将保存录像的文件返回给MainActivity
        Intent mIntent = new Intent();
        mIntent.putExtra("videoFile", fileName);
        //录屏结束
        if (mCurrentScreenFileName != null && !mCurrentScreenFileName.isEmpty()) {
            Log.e("获取录像文件：","路径:path="+mCurrentScreenFileName);
            mIntent.putExtra("screenFile", mCurrentScreenFileName);
        }
        setResult(MainActivity.SCREEN_FLOAT_VIDEO_CODE, mIntent);
        finish();
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.StartButton2 && !isRecording){
//            startVideo();//开始录像

            if(getAndroidSDKVersion() >= 21){
                //开始录屏
                mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, MainActivity.SCREEN_CEDE);
            }else {
                Toast.makeText(this,"设备的版本低于5.0，不能录制屏幕",Toast.LENGTH_SHORT).show();
            }
        }
        
        if (v.getId() == R.id.EndButton2){//结束  录像+录屏
            if (mScreenRecorder != null) {
                //录制屏幕结束
                Toast.makeText(this,"录屏结束",Toast.LENGTH_SHORT).show();
                mScreenRecorder.quit();
                mScreenRecorder = null;
            }
            
            mTime = (System.currentTimeMillis() - mStartTime) / 1000f;
            Log.e("time", mTime + "");
            if (!mRead) {                                     //没准备好
                reset();
            }
            if (!isRecording || mTime < 0.6f) {               //录制时间太短或prepare方法未完成
                Toast.makeText(this, com.practice.videorecordermodel.R.string.time_is_too_short, Toast.LENGTH_SHORT).show();
                mVideoManage.cancel();
            } else { //录制成功
                mVideoManage.release();// 释放资源
                //将结果返回给MainActivity
                onRecordingFinish(mVideoManage.getCurrentFileName());
            }
            reset();
            isRecording = false;
        }
    }
    
    private void reset() {
        mTime = 0;
        mRead = false;
        isRecording = false;
    }


    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private LinearLayout mFloatLayout;
    private SurfaceView mFloatView;
    private ScreenRecorder mScreenRecorder;
    private String mCurrentScreenFileName;
    private void createFloatView() {
        //获取LayoutParams对象  
        wmParams = new WindowManager.LayoutParams();
        //获取的是LocalWindowManager对象  
        mWindowManager = this.getWindowManager();
        //mWindowManager = getWindow().getWindowManager();  

        //获取的是CompatModeWrapper对象  
        //mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);  
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(getApplication());  // this.getLayoutInflater();

        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_layout, null);
        mWindowManager.addView(mFloatLayout, wmParams);
        mFloatView = (SurfaceView)mFloatLayout.findViewById(R.id.floatSurfaceView);
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        //绑定触摸移动监听  
        mFloatView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wmParams.x = (int)event.getRawX() - mFloatLayout.getWidth()/2;
                //25为状态栏高度  
                wmParams.y = (int)event.getRawY() - mFloatLayout.getHeight()/2 - 40;
                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;
            }
        });
        //绑定点击监听  
        mFloatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(RecordingAndScreenActivity.this,"点击了悬浮窗口",Toast.LENGTH_SHORT).show();
            }
        });
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
            final int bitrate = 100000;//比特率  影响生成的视频大小
            mScreenRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
            mScreenRecorder.start();
            Toast.makeText(this, "开始录屏了！", Toast.LENGTH_SHORT).show();
            //此APP是否在后台运行  即：是否跳转到Home页面
            moveTaskToBack(false);

            startVideo();//开始录像
        }
    }

    private void startVideo() {
        isRecording = true;
        mRead = true;
        mVideoManage.prepareVideo();//开始录像
        mStartTime = System.currentTimeMillis();
    }

   /* @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0 ;i < permissions.length;i++){
            System.out.println("grantResults:"+grantResults.toString());
            System.out.println("grantResults:"+permissions.toString());
        } 
        if (permissions[0].equals(Manifest.permission.CAMERA)
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(RecordingAndScreenActivity.this, "权限申请成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(RecordingAndScreenActivity.this, "获取权限失败", Toast.LENGTH_SHORT).show();
            if (!ActivityCompat.shouldShowRequestPermissionRationale(RecordingAndScreenActivity.this, Manifest.permission.CALL_PHONE)) {
                Toast.makeText(this,"请到设置里开启该权限",Toast.LENGTH_SHORT).show();
            }
        }
    }*/

    @Override
    protected void onDestroy() {
        if(mFloatLayout != null) {
            //移除悬浮窗口  
            mWindowManager.removeView(mFloatLayout);
            mWindowManager = null;
            mFloatLayout = null;
        }
        super.onDestroy();
        if (mScreenRecorder != null) {
            mScreenRecorder.quit();
            mScreenRecorder = null;
        }
    }
    /**
     * 生成随机文件
     *
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
        Log.e("保存录屏文件：","路径:path="+mCurrentScreenFileName);
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
