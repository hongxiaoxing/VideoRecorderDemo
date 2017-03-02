package com.practice.videorecordermodel.manage;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.practice.videorecordermodel.R;
import com.practice.videorecordermodel.view.RecordButton;

/**
 * 〈用来管理progress和现实在录制界面上文字的类〉
 *
 * @author XLC
 * @version [1.0, 2016/2/24]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class ProgressDialogManage {

    private ProgressBar mProgressBar;

    //提示用户松手取消的
    private TextView alertTextView;

    //提示用户上滑取消的
    private TextView slideAlertTextView;

    private Dialog mDialog;
    private Context mContext;

    public ProgressDialogManage(Context context) {
        mContext = context;
    }

    /**
     * 显示progressDialog
     */
    public void showProgress() {
        mDialog = new Dialog(mContext, R.style.Theme_AudioDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.diallog_layout, null);
        mDialog.setContentView(view);

        alertTextView = (TextView) view.findViewById(R.id.alertText);
        slideAlertTextView = (TextView) view.findViewById(R.id.slideAlert);
        mProgressBar = (ProgressBar) view.findViewById(R.id.mProgressBar);

        mProgressBar.setMax((int) RecordButton.getMaxTime());
        mDialog.show();
    }

    /**
     * 显示松开取消
     */
    public void showAlertView() {
        if (mDialog != null && mDialog.isShowing()) {
            alertTextView.setVisibility(View.VISIBLE);
            slideAlertTextView.setVisibility(View.GONE);
        }
    }

    /**
     * 显示上滑取消
     */
    public void showSlideAlertView() {
        if (mDialog != null && mDialog.isShowing()) {
            alertTextView.setVisibility(View.GONE);
            slideAlertTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 更新进度条状态
     *
     * @param timeCount
     */
    public void updateProgress(int timeCount) {
        if (mDialog != null && mDialog.isShowing()) {
            mProgressBar.setProgress(timeCount);
        }
    }


    /**
     * 关闭Dialog
     */
    public void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

}
