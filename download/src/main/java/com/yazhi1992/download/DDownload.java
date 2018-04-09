package com.yazhi1992.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.yazhi1992.download.filter.FilterCallback;
import com.yazhi1992.download.filter.FilterChain;
import com.yazhi1992.download.filter.IFilter;
import com.yazhi1992.download.task.TaskCenter;
import com.yazhi1992.download.util.InitException;

import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public class DDownload {

    private Context mContext;
    private DownloadService.DownloadBinder mBinder;
    private FilterChain mAfterDownloadFilterChain;
    private FilterChain mBeforeDownloadFilterChain;
    private ProgressListener mProgressListener;

    private DDownload() {
        mBeforeDownloadFilterChain = new FilterChain();
        mAfterDownloadFilterChain = new FilterChain();
    }

    private static class DownloadHolder {
        private static DDownload INSTANCE = new DDownload();
    }

    public ProgressListener getProgressListener() {
        return mProgressListener;
    }

    public synchronized void setProgressListener(ProgressListener progressListener) {
        mProgressListener = progressListener;
    }

    public void addAfterDownloadFilter(IFilter filter) {
        mAfterDownloadFilterChain.add(filter);
    }

    public void addBeforeDownloadFilter(IFilter filter) {
        mBeforeDownloadFilterChain.add(filter);
    }

    public static DDownload getInstance() {
        return DownloadHolder.INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        TaskCenter.getInstance().init(context);
    }

    public void start(List<DownloadTask> tasks, String tasksTag, DownloadListener callback) {
        mBeforeDownloadFilterChain.filter(tasks, new FilterCallback() {
            @Override
            public void onContinue(List<DownloadTask> thisTasks) {
                getBinder(new GetBinderCallback() {
                    @Override
                    public void getBinder(DownloadService.DownloadBinder binder) {
                        mBinder.start(thisTasks, tasksTag, new DownloadListener() {

                            @Override
                            public void onError(String msg, int code) {
                                callback.onError(msg, code);
                            }

                            @Override
                            public void onComplete() {
                                mAfterDownloadFilterChain.filter(tasks, new FilterCallback() {

                                    @Override
                                    public void onContinue(List<DownloadTask> tasks) {
                                        callback.onComplete();
                                    }

                                    @Override
                                    public void onInterrupt(String msg, int code) {
                                        callback.onError(msg, code);
                                    }
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onInterrupt(String msg, int code) {
                callback.onError(msg, code);
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
        if(mContext == null) throw new InitException();
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
