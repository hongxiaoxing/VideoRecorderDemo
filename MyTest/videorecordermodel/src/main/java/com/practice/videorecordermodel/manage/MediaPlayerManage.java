package com.practice.videorecordermodel.manage;

import android.app.DialogFragment;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.practice.videorecordermodel.R;

/**
 * 〈用于管理视频播放〉
 *
 * @author XLC
 * @version [1.0, 2016/2/29]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class MediaPlayerManage extends DialogFragment implements SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private View mView;
    private String filePath = "";
    private int vWidth, vHeight;
    private Display currDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.mediaPlayer_style);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        mView = inflater.inflate(R.layout.player_fragment, container);

        getArgumentsFromActivity();
        initView();

        currDisplay = getActivity().getWindowManager().getDefaultDisplay();


        Display display = getActivity().getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        layoutParams.height = display.getHeight();
        layoutParams.width = display.getWidth();

        return mView;
    }

    private void initView() {
        mSurfaceView = (SurfaceView) mView.findViewById(R.id.mSurfaceView);
        mSurfaceView.setOnClickListener(surfaceViewClickListener);
        mSurfaceHolder = mSurfaceView.getHolder();
        //如果要在本页面再次播放视频就必须要先调用removeCallback（）这里只调用一次所以不用
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //必须在surface创建后才能初始化MediaPlayer,否则不会显示图像
        if (!filePath.isEmpty()) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            try {
                mMediaPlayer.setDataSource(filePath);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getActivity(), R.string.filePath_can_not_empty, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            //Activity销毁时停止播放，释放资源。不做这个操作，即使退出还是能听到视频播放的声音
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mMediaPlayer = null;

    }

    /**
     * 获取从Activity传送过来的值
     */
    private void getArgumentsFromActivity() {
        filePath = getArguments().getString("filePath");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // 当MediaPlayer播放完成后触发
        Toast.makeText(getActivity(), R.string.touch_to_back, Toast.LENGTH_LONG).show();
    }

    private View.OnClickListener surfaceViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();//退出录制屏幕的视频播放（"轻触退出"）
        }
    };

    @Override
    public void onPrepared(MediaPlayer mp) {

    }
}
