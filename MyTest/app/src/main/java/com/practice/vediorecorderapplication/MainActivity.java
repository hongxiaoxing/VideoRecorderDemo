package com.practice.vediorecorderapplication;

import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.practice.videorecordermodel.manage.MediaPlayerManage;
import com.practice.videorecordermodel.manage.ScreenRecorder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int SCREEN_CEDE = 1;

    public static final int SCREEN_VIDEO_CODE = 2;

    public static final int SCREEN_FLOAT_VIDEO_CODE = 3;

    public static final int VIDEO_CODE = 4;


    private Button startVideoScreen, startScreen,starVideoScreenFloat,startVideo;

    private String fileName;

    //存放文件路径
    private List<String> fileData;

    private ListView mListView;

    private MyAdapter myAdapter;

    //屏幕录制工具（android5.0推出的录制屏幕的工具）
    private MediaProjectionManager mMediaProjectionManager;

    private ScreenRecorder mScreenRecorder;

    private String mCurrentScreenFileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initScreenRecorder();//初始化按钮---->录屏
        initRecordingVideo();//初始化按钮---->录像
        initRecordScreenAndVideo();//初始化按钮---->录屏+录像
        initRecordScreenAndVideoAndFloatView();//初始化按钮---->录屏+录像+悬浮框
        initListView();
    }

    /**
     * 初始化按钮---->录像
     */
    private void initRecordingVideo() {
        //设置显示视频显示在SurfaceView上
        startVideo = (Button) findViewById(R.id.press_to_recordingVideo);
        startVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordingVideoActivity.class);
                startActivityForResult(intent, VIDEO_CODE);
            }
        });
    }

    /**
     * 初始化按钮---->录屏
     */
    private void initScreenRecorder() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startScreen = (Button) findViewById(R.id.press_to_recording_screen);
        startScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScreenRecorder != null) {
                    //录屏结束
                    if (mCurrentScreenFileName != null && !mCurrentScreenFileName.isEmpty()) {
                        fileName = mCurrentScreenFileName;
                        fileData.add(fileName);
                        myAdapter.notifyDataSetChanged();
                    }
                    mScreenRecorder.quit();
                    mScreenRecorder = null;
                    startScreen.setText("开始录屏");
                } else {
                    Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, SCREEN_CEDE);
                }
            }
        });
    }

    /**
     * 初始化按钮---->录屏+录像
     */
    private void initRecordScreenAndVideo() {
        //设置显示视频显示在SurfaceView上
        startVideoScreen = (Button) findViewById(R.id.press_to_recording);
        startVideoScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordingActivity.class);
                startActivityForResult(intent, SCREEN_VIDEO_CODE);
            }
        });
    }

    /**
     * 初始化按钮---->录屏+录像+悬浮框
     */
    private void initRecordScreenAndVideoAndFloatView() {
        //设置显示视频显示在SurfaceView上
        starVideoScreenFloat = (Button) findViewById(R.id.press_to_recording_and_screen);
        starVideoScreenFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordingAndScreenActivity.class);
                startActivityForResult(intent, SCREEN_FLOAT_VIDEO_CODE);
            }
        });
    }

    
    private void initListView() {
        mListView = (ListView) findViewById(R.id.myListView);
        fileData = new ArrayList<>();
//        try{
//            File file = new File(Environment.getExternalStorageDirectory() + "/videoRecorder");
//            File[]  files =  file.listFiles();
//            for (int i = 0;i < files.length;i++){
//                if(files[i].getName().endsWith(".mp4") && files[i].length() > 0){
//                    fileData.add(files[i].getAbsolutePath());
//                }
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        myAdapter = new MyAdapter(fileData, this);
        mListView.setAdapter(myAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaPlayerManage mediaPlayerManage = new MediaPlayerManage();
                Bundle bundle = new Bundle();
                bundle.putString("filePath", fileData.get(position));
                mediaPlayerManage.setArguments(bundle);
                mediaPlayerManage.show(getFragmentManager(), "mediaPlayerDialog");
            }
        });
    }




    /**
     * 录制完成的回掉
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null){
            return;
        }
        //选择同意/拒绝录屏  的结果
        if (requestCode == SCREEN_CEDE) { 
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {//拒绝录屏
                Log.e("@@", "media projection is null");
                return;
            }
            //同意录屏  设置录制视频尺寸，要注意
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            final int width = profile.videoFrameHeight;
            final int height = profile.videoFrameWidth;
            File file = createMFile();//创建保存录制屏幕视频的文件
            final int bitrate = 6000000;
            mScreenRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
            mScreenRecorder.start();
            startScreen.setText("停止录屏");
            //此APP是否在后台运行  即：是否跳转到Home页面
            moveTaskToBack(true);
        }

        //录像  返回的结果
        if (requestCode == VIDEO_CODE) {
            fileName = data.getStringExtra("videoFile");
            fileData.add(fileName);
            myAdapter.notifyDataSetChanged();
        }

        //录像+录屏+悬浮框  返回的结果
        if (requestCode == SCREEN_FLOAT_VIDEO_CODE) {
            fileName = data.getStringExtra("videoFile");
            fileData.add(fileName);
            fileName = data.getStringExtra("screenFile");
            fileData.add(fileName);
            myAdapter.notifyDataSetChanged();
        }

        //录像+录屏  返回的结果
        if (requestCode == SCREEN_VIDEO_CODE) {
            fileName = data.getStringExtra("videoFile");
            fileData.add(fileName);
            fileName = data.getStringExtra("screenFile");
            fileData.add(fileName);
            myAdapter.notifyDataSetChanged();
        }
    }

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
     *
     * @return
     */
    private File createMFile() {
        String fileDir ; //设置文件存放目录
        if (Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED)) {
            fileDir = Environment.getExternalStorageDirectory() + "/videoRecorder";
        }else {
            fileDir = this.getFilesDir().getAbsolutePath() + "/videoRecorder";
        }
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
}

