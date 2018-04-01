package com.yazhi1992.ddownload;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.xiaoyu.download.DownloadCallback;
import com.xiaoyu.download.XYDownload;
import com.yazhi1992.ddownload.databinding.ActivityDownloadBinding;

public class DownloadActivity extends AppCompatActivity {

    private ActivityDownloadBinding mBinding;
    DownloadModel mModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_download);
        mModel = new DownloadModel();
        mBinding.setModel(mModel);

        XYDownload.getInstance().start("", new DownloadCallback() {
            @Override
            public void update(int progress, int total) {
                Log.e("zyz", "");
            }

            @Override
            public void onError(String msg, int code) {
                Log.e("zyz", "onError");
            }
        });

    }
}
