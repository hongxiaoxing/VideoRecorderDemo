package com.practice.videorecordermodel.manage;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 〈录制视频管理〉
 * 〈功能详细描述〉
 *
 * @author XLC
 * @version [1.0, 2016/2/23]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class VideoManage implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    private MediaRecorder mMediaRecorder;

    private static VideoManage mInstance;

    private String fileDir = Environment.getExternalStorageDirectory() + "/videoRecorder";
    ; //录制文件存放的文件夹路径

    private String mCurrentFileName = ""; //当前录制文件的路径

    private boolean isPrepared = false;

    private SurfaceHolder mSurfaceHolder;

    private Camera mCamera;

    //录制准备好了的回掉接口
    private videoPreparedListener mListener;

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {

    }

    /**
     * 设置文件保存路径
     *
     * @param fileDir
     */
    public void setFileDir(String fileDir) {
        this.fileDir = fileDir;
    }


    public interface videoPreparedListener {
        void wellPrepared();
    }

    public void setVideoPreparedListener(videoPreparedListener listener) {
        mListener = listener;
    }

    private VideoManage() {
    }

    public static VideoManage getInstance() {
        if (mInstance == null) {
            synchronized (VideoManage.class) {
                mInstance = new VideoManage();
            }
        }
        return mInstance;
    }

    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        if (mSurfaceHolder != null) {
            initMSurfaceHolder();
        }
    }

    private SurfaceHolder.Callback mCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                initCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCameraResource();
        }
    };

    private void initMSurfaceHolder() {
        mSurfaceHolder.addCallback(mCallBack);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void prepareVideo() {
        try {
            isPrepared = false;
            File dir = new File(fileDir);
            //判断文件夹是否存在 如不存在就创建
            if (!dir.exists()) {
                dir.mkdir();
            }
            String fileName = generateFileName();

            File mFile = new File(dir, fileName);
            mCurrentFileName = mFile.getAbsolutePath();

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.reset();
            if (mCamera == null) {
                initCamera();
            }
            //取消录制后再次进行录制时必须加如下两步操作，不然会报错
            mCamera.lock();
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setOnInfoListener(this);
            mMediaRecorder.setOnErrorListener(this);
            /**
             视频编码格式：default，H263，H264，MPEG_4_SP
             获得视频资源：default，CAMERA
             音频编码格式：default，AAC，AMR_NB，AMR_WB
             获得音频资源：defalut，camcorder，mic，voice_call，voice_communication,
             voice_downlink,voice_recognition, voice_uplink
             输出方式：amr_nb，amr_wb,default,mpeg_4,raw_amr,three_gpp
             */
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);// 摄像头为视频源
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风为音频源     不设置则没有声音
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);// 设置视频输出格式为MP4
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);// 设置音频编码格式   不设置则没有声音
            //根据屏幕分辨率设置录制尺寸   
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//          mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);//设置视频尺寸大小例如512 * 1000   注释掉可以适配某些手机（华为荣耀5X）
            mMediaRecorder.setVideoFrameRate(20); // 设置视频的帧率,显著提高录像时的流畅度
            mMediaRecorder.setVideoEncodingBitRate(profile.videoFrameWidth * profile.videoFrameHeight);////设置视频编码的码率
            mMediaRecorder.setOrientationHint(0);// 1.输出旋转90度，保持竖屏录制(后摄像头)    2.输出旋转270度，保持竖屏录制(前摄像头) 
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);//  设置视频录制格式
            mMediaRecorder.setOutputFile(mFile.getAbsolutePath());//设置视频输出路径

            mMediaRecorder.prepare();
            isPrepared = true;
            mMediaRecorder.start();
            if (mListener != null) {
                mListener.wellPrepared();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化摄像头
     */
    private void initCamera() {
        releaseCameraResource();
        try {
            openFrontCamera();//开启前摄像头录像
        } catch (Exception e) {
            e.printStackTrace();
            releaseCameraResource();
        }
        if (mCamera == null)
            return;


    }

    /**
     * 开启前摄像头录像
     * @throws IOException
     */
    private void openFrontCamera() throws IOException {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT );
        //调用前摄像头时，需要设置摄像头的参数
        Camera.Parameters parameters = mCamera.getParameters();
        mCamera.setDisplayOrientation(0);//竖屏90
        mCamera.setParameters(parameters);
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.startPreview();
        mCamera.unlock();
    }

    /**
     * 开启后摄像头录像
     * @throws IOException
     */
    private void openBackCamera() throws IOException {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK );//或Camera.open();
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.startPreview();
        mCamera.unlock();
    }
    
    
    /**
     * 释放摄像头资源
     */
    private void releaseCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    //录制出错处理
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        if (mr != null) {
            mr.reset();
        }
    }

    /**
     * 随机产生文件名
     *
     * @return
     */
    private String generateFileName() {
        return UUID.randomUUID().toString() + ".mp4";
    }

    /**
     * 释放资源
     */
    public void release() {
        try {
//            releaseCameraResource();
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaRecorder = null;
    }

    /**
     * 删除刚刚录制的文件并释放资源
     */
    public void cancel() {
        release();
        File file = new File(mCurrentFileName);
        file.delete();
//        initCamera();
        mCurrentFileName = null;
    }

    /**
     * 获取当前录制文件的绝对路径
     *
     * @return
     */
    public String getCurrentFileName() {
        return mCurrentFileName;
    }

    /**
     * 获取当前录音准备状态
     *
     * @return
     */
    public boolean getPrepared() {
        return isPrepared;
    }

}
