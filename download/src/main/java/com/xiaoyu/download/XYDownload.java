package com.xiaoyu.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.xiaoyu.download.task.BasicTask;
import com.xiaoyu.download.task.TaskCenter;

import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public class XYDownload {

    private Context mContext;
    private DownloadService.DownloadBinder mBinder;

    private XYDownload() {
    }

    private static class DownloadHolder {
        private static XYDownload INSTANCE = new XYDownload();
    }

    public static XYDownload getInstance() {
        return DownloadHolder.INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        TaskCenter.getInstance().init(context);
    }

    public void start(BasicTask task, DownloadCallback callback) {
        getBinder(new GetBinderCallback() {
            @Override
            public void getBinder(DownloadService.DownloadBinder binder) {
                mBinder.start(task, callback);
            }
        });
    }

    public void start(List<BasicTask> tasks, DownloadCallback callback) {
        getBinder(new GetBinderCallback() {
            @Override
            public void getBinder(DownloadService.DownloadBinder binder) {
                mBinder.startAll(tasks, callback);
            }
        });
    }

    public void stopAll() {
        getBinder(new GetBinderCallback() {
            @Override
            public void getBinder(DownloadService.DownloadBinder binder) {
                mBinder.stopAll();
            }
        });
    }

    interface GetBinderCallback {
        void getBinder(DownloadService.DownloadBinder binder);
    }

    private void getBinder(GetBinderCallback callback) {
        if (mBinder == null) {
            Intent intent = new Intent(mContext, DownloadService.class);
            mContext.startService(intent);
            mContext.bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mBinder = (DownloadService.DownloadBinder) service;
                    callback.getBinder(mBinder);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
            return;
        }
        callback.getBinder(mBinder);
    }
}
