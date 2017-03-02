# VideoRecorderDemo
录像+录屏+悬浮框
SDK版本：22，没有做敏感权限的处理

项目的入口MainActivity
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

一、录屏（具体原理看代码；只能支持Android5.0及以上的版本）
   1.屏幕录制工具（android5.0推出的录制屏幕的工具）的初始化
	MediaProjectionManager mMediaProjectionManager  = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    
    2.弹出录屏的提示框
	Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, SCREEN_CEDE);

     3.在onActivityResult方法中，获得弹框提示选择的结果，并做相关处理
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
            final int bitrate = 6000000;//录屏生成视频的帧率，影响录屏的效果
            mScreenRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
            mScreenRecorder.start();//开始录屏
            //此APP是否在后台运行  即：是否跳转到Home页面
            moveTaskToBack(true);
        }

      4.结束录屏
	mScreenRecorder.quit();
        mScreenRecorder = null;


二、录像（消耗资源大）
     1.初始化录像管理器
	VideoManage mVideoManage  = VideoManage.getInstance();
	//mVideoManage.setFileDir(); //这个方法设置录制文件的存放路径
	//设置录像的控件（SurfaceView），该方法封装了初始化摄像头，并监听视频录制。
        mVideoManage.setSurfaceHolder(mFloatView.getHolder());

	关键方法
	//VideoManage.getInstance方法
	public static VideoManage getInstance() {
        if (mInstance == null) {
            synchronized (VideoManage.class) {
                mInstance = new VideoManage();
            }
        }
       	   return mInstance;
    	}

	/**
     * 开启前摄像头录像
     * @throws IOException
     */
    private void openFrontCamera() throws IOException {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT );
        //调用前摄像头时，需要设置摄像头的参数
        Camera.Parameters parameters = mCamera.getParameters();
        mCamera.setDisplayOrientation(0);//竖屏90  横屏0  设置录像的角度
        mCamera.setParameters(parameters);
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.startPreview();
        mCamera.unlock();
    }

    2.开始录像
	isRecording = true;//是否处于录像的状态
        mRead = true;//是否准备好录像
        mVideoManage.prepareVideo();//开始准备录像
        mStartTime = System.currentTimeMillis();//记录开始准备录像的时间

	//关键方法mVideoManage.prepareVideo()
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
            mMediaRecorder.start();//开始录像
            if (mListener != null) {
                mListener.wellPrepared();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    3.录像结束时调用的方法
	private void endRecordVideo() {//录像结束
        mTime = (System.currentTimeMillis() - mStartTime) / 1000f;获得录像的总时间
        Log.e("time", mTime + "");
        if (!mRead) {                                     //没准备好
            reset();//重置 将mRead、isRecording设置为false
        }
        if (!isRecording || mTime < 0.6f) {      //录制时间太短或prepare方法未完成
            Toast.makeText(this, com.practice.videorecordermodel.R.string.time_is_too_short, Toast.LENGTH_SHORT).show();
            mVideoManage.cancel();//取消录像 删除文件
        } else {    //录制成功
            mVideoManage.release();// 释放资源  重要
            //回掉录制完成接口
            onRecordingFinish(mVideoManage.getCurrentFileName());
        }
        reset();//重置 将mRead、isRecording设置为false
        isRecording = false;
    }