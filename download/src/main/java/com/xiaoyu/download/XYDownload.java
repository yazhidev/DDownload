package com.xiaoyu.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

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
    }

    public void start(String url, DownloadCallback callback) {
        if (mBinder == null) {
            Intent intent = new Intent(mContext, DownloadService.class);
            mContext.startService(intent);
            mContext.bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mBinder = (DownloadService.DownloadBinder) service;
                    mBinder.start(callback);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
            return;
        }
        mBinder.start(callback);
    }
}
