package com.yazhi1992.ddownload;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.xiaoyu.download.AppConstant;
import com.xiaoyu.download.DownloadCallback;
import com.xiaoyu.download.DownloadTask;
import com.xiaoyu.download.task.TaskCenter;
import com.xiaoyu.download.util.DownloadUtils;
import com.xiaoyu.download.XYDownload;
import com.yazhi1992.ddownload.databinding.ActivityDownloadBinding;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadActivity extends AppCompatActivity {

    private ActivityDownloadBinding mBinding;
    DownloadModel mModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_download);
        mModel = new DownloadModel();
        mBinding.setModel(mModel);

        DownloadTask downloadTask = new DownloadTask(AppConstant.path, AppConstant.url);

        getFileSize(downloadTask);

        mBinding.btnStart.setOnClickListener(v -> {
            XYDownload.getInstance().start(downloadTask, new DownloadCallback() {
                @Override
                public void update(long progress, long total) {
                    Log.e("zyz", "");
                    int percent = (int) (progress * 100 / total);
                    mModel.progress.set("进度：" + Integer.toString(percent) + "%");

                    getFileSize(downloadTask);
                }

                @Override
                public void onError(String msg, int code) {
                    Log.e("zyz", "onError");
                }

                @Override
                public void onComplete() {
                    File file = new File(downloadTask.getSavePath());
                    mModel.file1Size.set("下载完成，大小：" + DownloadUtils.transferSize(file.length()));
                }
            });
        });

        mBinding.btnPause.setOnClickListener(v -> {
            getFileSize(downloadTask);
            XYDownload.getInstance().stopAll();
        });

        mBinding.btnDelete.setOnClickListener(v -> {
            deleteMyFile(AppConstant.path);
            deleteMyFile(downloadTask.getTempFilePath());
            deleteMyFile(AppConstant.path2);
            mModel.progress.set("");
            TaskCenter.getInstance().removeAll();
            getFileSize(downloadTask);
        });
    }

    /**
     * 得到下载内容的大小
     * @param downloadUrl
     * @return
     */
    private long getContentLength(String downloadUrl){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(downloadUrl).build();
        try {
            Response response=client.newCall(request).execute();
            if(response!=null&&response.isSuccessful()){
                long contentLength=response.body().contentLength();
                response.body().close();
                Log.e("zyz", "length" + contentLength);
                return contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  0;
    }

    private void deleteMyFile(String path) {
        File file = new File(path);
        if(file.exists()) {
            file.delete();
        }
    }

    private void getFileSize(DownloadTask downloadTask) {
        File file = new File(downloadTask.getSavePath());
        if(file.exists()) {
            mModel.file1Size.set("本地文件大小：" + DownloadUtils.transferSize(file.length()));
        } else {
            File file1 = new File(downloadTask.getTempFilePath());
            if(file1.exists()) {
                mModel.file1Size.set("本地文件大小：" + DownloadUtils.transferSize(file1.length()));
            } else {
                mModel.file1Size.set("本地文件大小：");
            }
        }
    }
}
