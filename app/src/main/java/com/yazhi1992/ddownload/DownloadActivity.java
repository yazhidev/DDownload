package com.yazhi1992.ddownload;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.xiaoyu.download.AppConstant;
import com.xiaoyu.download.DownloadListener;
import com.xiaoyu.download.DownloadTask;
import com.xiaoyu.download.ProgressListener;
import com.xiaoyu.download.XYDownload;
import com.xiaoyu.download.task.TaskCenter;
import com.xiaoyu.download.util.DownloadUtils;
import com.yazhi1992.ddownload.databinding.ActivityDownloadBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        DownloadTask downloadTask2 = new DownloadTask(AppConstant.path2, AppConstant.url2);
        DownloadTask downloadTask3 = new DownloadTask(AppConstant.path3, AppConstant.url3);

        getFileSize(downloadTask, downloadTask2, downloadTask3);

        mBinding.btnStart.setOnClickListener(v -> {
            List<DownloadTask> list = new ArrayList<>();
            list.add(downloadTask);
            list.add(downloadTask2);
            list.add(downloadTask3);
            XYDownload.getInstance().start(list, "test", new DownloadListener() {
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

        XYDownload.getInstance().setProgressListener(new ProgressListener() {
            @Override
            public void taskProgress(String downloadUrl, long progress, long total) {

            }

            @Override
            public void taskContainerProgress(String containerTag, long progress, long total) {
                Log.e("zyz", "");
                int percent = (int) (progress * 100 / total);
                mModel.progress.set("进度：" + Integer.toString(percent) + "%");

                getFileSize(downloadTask, downloadTask2, downloadTask3);
            }
        });

        mBinding.btnPause.setOnClickListener(v -> {
            getFileSize(downloadTask, downloadTask2, downloadTask2);
            XYDownload.getInstance().stopAll();
        });

        mBinding.btnDelete.setOnClickListener(v -> {
            deleteMyFile(AppConstant.path);
            deleteMyFile(AppConstant.path2);
            deleteMyFile(AppConstant.path3);
            deleteMyFile(downloadTask.getTempFilePath());
            deleteMyFile(downloadTask2.getTempFilePath());
            deleteMyFile(downloadTask3.getTempFilePath());
            mModel.progress.set("");
            TaskCenter.getInstance().removeAll();
            getFileSize(downloadTask, downloadTask2, downloadTask2);
        });
    }

    private void deleteMyFile(String path) {
        File file = new File(path);
        if(file.exists()) {
            file.delete();
        }
    }

    private void getFileSize(DownloadTask downloadTask, DownloadTask downloadTask2, DownloadTask downloadTask3) {
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

        File file2 = new File(downloadTask2.getSavePath());
        if(file2.exists()) {
            mModel.file1Size2.set("本地文件大小：" + DownloadUtils.transferSize(file2.length()));
        } else {
            File file12 = new File(downloadTask2.getTempFilePath());
            if(file12.exists()) {
                mModel.file1Size2.set("本地文件大小：" + DownloadUtils.transferSize(file12.length()));
            } else {
                mModel.file1Size2.set("本地文件大小：");
            }
        }

        File file3 = new File(downloadTask3.getSavePath());
        if(file3.exists()) {
            mModel.file1Size3.set("本地文件大小：" + DownloadUtils.transferSize(file3.length()));
        } else {
            File file13 = new File(downloadTask3.getTempFilePath());
            if(file13.exists()) {
                mModel.file1Size3.set("本地文件大小：" + DownloadUtils.transferSize(file13.length()));
            } else {
                mModel.file1Size3.set("本地文件大小：");
            }
        }
    }
}
