package com.xiaoyu.download;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xiaoyu.download.task.BasicTask;
import com.xiaoyu.download.task.TaskCenter;
import com.xiaoyu.download.util.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public class DownloadService extends Service {

    private DownloadBinder mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new DownloadBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DownloadBinder extends Binder {

        public void start(BasicTask task, DownloadCallback callback) {
            if(TaskCenter.getInstance().isDownloading(task)) return;
            File file = new File(task.getSavePath());
            if (file.exists()) {
                callback.onComplete();
                return;
            }
            task.setCanceld(false);
            TaskCenter.getInstance().addTask(task);
//            mTasks.get(task.getDownloadUrl()).setTotal(getContentLength(task.getDownloadUrl()));
            // retrofit   @Streaming/*大文件需要加入这个判断，防止下载过程中写入到内存中*/
            File tempFile = new File(task.getTempFilePath());
            if (!tempFile.getParentFile().exists())
                tempFile.getParentFile().mkdirs();

            long length = 0;
            if (tempFile.exists()) {
                length = tempFile.length();
            }

            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + length + "-")
                    .url(task.getDownloadUrl()).build();
            long finalLength = length;
            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("error", -1);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    RandomAccessFile rwd = new RandomAccessFile(tempFile, "rwd");
                    rwd.seek(finalLength);
                    if(task.getTotal() == 0) {
                        task.updateTotal(response.body().contentLength());
                    }
                    Log.e("zyz", "start total " + task.getTotal() + " " + DownloadUtils.transferSize(task.getTotal()) + " randomLength " + DownloadUtils.transferSize(finalLength));
                    InputStream is = response.body().byteStream();
                    byte[] buffer = new byte[1024 * 8];
                    int len = -1;
                    long progress = finalLength;
                    while ((len = is.read(buffer)) != -1 && !task.isCanceld()) {
                        //写入文件
                        rwd.write(buffer, 0, len);
                        progress += len;
                        task.setProgress(progress);
                        callback.update(progress, task.getTotal());
                    }
                    response.body().byteStream().close();
                    rwd.close();
                    if (tempFile.length() >= task.getTotal()) {
                        tempFile.renameTo(new File(task.getSavePath()));
                        //任务完成，移除任务
                        TaskCenter.getInstance().removeTask(task.getDownloadUrl());
                        callback.onComplete();
                    }
                }
            });

        }

//        public void newStart(BasicTask task, DownloadCallback callback) {
//            Observable.create(new ObservableOnSubscribe<BasicTask>() {
//                @Override
//                public void subscribe(ObservableEmitter<BasicTask> e) throws Exception {
//                    if(mTasks.containsKey(task.getSavePath()) && !mTasks.get(task.getSavePath()).isCanceld()) return;
//                    File file = new File(task.getSavePath());
//                    if (file.exists()) {
//                        e.onComplete();
//                    } else {
//                        e.onNext(task);
//                    }
//                }
//            })
//                    .observeOn(Schedulers.newThread())
//                    .concatMap(new Function<BasicTask, ObservableSource<BasicTask>>() {
//                        @Override
//                        public ObservableSource<BasicTask> apply(BasicTask absTask) throws Exception {
//                            if(absTask.getProgress() != 0) {
//                                //
//                                return getContentLength(absTask);
//                            } else{
//
//                            }
//                            return getContentLength(absTask);
//                        }
//                    });
//        }


        public void stopAll() {
            for (Map.Entry<String, BasicTask> entry : TaskCenter.getInstance().getTasks().entrySet()) {
                entry.getValue().setCanceld(true);
            }
        }

        /**
         * 得到下载内容的大小
         * @return
         */
        private Observable<BasicTask> getContentLength(BasicTask task){
            return Observable.create(new ObservableOnSubscribe<BasicTask>() {
                @Override
                public void subscribe(ObservableEmitter<BasicTask> emitter) throws Exception {
                    OkHttpClient client=new OkHttpClient();
                    Request request=new Request.Builder().url(task.getDownloadUrl()).build();
                    try {
                        Response response=client.newCall(request).execute();
                        if(response!=null&&response.isSuccessful()){
                            long contentLength=response.body().contentLength();
                            response.body().close();
                            task.updateTotal(contentLength);
                            emitter.onNext(task);
                        } else {
                            emitter.onError(new Throwable("getContentLength error"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
