package com.xiaoyu.download;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xiaoyu.download.task.BasicTask;
import com.xiaoyu.download.task.TaskBox;
import com.xiaoyu.download.task.TaskCenter;
import com.xiaoyu.download.util.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
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
            File file = new File(task.getSavePath());
            if (file.exists()) {
                callback.onComplete();
                return;
            }
            if (TaskCenter.getInstance().isDownloading(task)) return;
            task.setCanceld(false);
            TaskCenter.getInstance().addTask(task);
//            mTasks.get(task.getDownloadUrl()).setLength(getContentLength(task.getDownloadUrl()));
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
                    Log.e("zyz", "start total " + task.getLength() + " " + DownloadUtils.transferSize(task.getLength()) + " randomLength " + DownloadUtils.transferSize(finalLength));
                    InputStream is = response.body().byteStream();
                    byte[] buffer = new byte[1024 * 8];
                    int len = -1;
                    long progress = finalLength;
                    while ((len = is.read(buffer)) != -1 && !task.isCanceld()) {
                        //写入文件
                        rwd.write(buffer, 0, len);
                        progress += len;
                        task.setProgress(progress);
                        if (task.getLength() != 0) {
                            callback.update(progress, task.getLength());
                        }
                    }
                    response.body().byteStream().close();
                    rwd.close();
                    if (tempFile.length() >= task.getLength()) {
                        tempFile.renameTo(new File(task.getSavePath()));
                        //任务完成，移除任务
                        TaskCenter.getInstance().removeTask(task.getDownloadUrl());
                        callback.onComplete();
                    }
                }
            });
        }

        public void startAll(List<BasicTask> tasks, DownloadCallback callback) {
            final long[] total = {0};
            final int[] num = {0};
            TaskBox taskBox = new TaskBox(tasks);
            Observable.fromIterable(tasks)
                    .observeOn(Schedulers.newThread())
                    .concatMap(new Function<BasicTask, ObservableSource<BasicTask>>() {
                        @Override
                        public ObservableSource<BasicTask> apply(BasicTask basicTask) throws Exception {
                            return checkFinish(basicTask);
                        }
                    })
                    .filter(new Predicate<BasicTask>() {
                        @Override
                        public boolean test(BasicTask basicTask) throws Exception {
                            //如果在下载中则跳过
                            return !TaskCenter.getInstance().isDownloading(basicTask);
                        }
                    })
                    .concatMap(new Function<BasicTask, ObservableSource<BasicTask>>() {
                        @Override
                        public ObservableSource<BasicTask> apply(BasicTask basicTask) throws Exception {
                            return getContentLength(basicTask);
                        }
                    })
                    .doOnNext(new Consumer<BasicTask>() {
                        @Override
                        public void accept(BasicTask basicTask) throws Exception {
                            taskBox.setLength(taskBox.getLength() + basicTask.getLength());
                            Log.e("zyz", "get " + basicTask.getLength());
                        }
                    })
                    .concatMap(new Function<BasicTask, ObservableSource<BasicTask>>() {
                        @Override
                        public ObservableSource<BasicTask> apply(BasicTask basicTask) throws Exception {
                            return download(basicTask, taskBox, callback);
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.e("zyz", "doOnComplete total " + total[0]);
                            callback.onComplete();
                        }
                    })
                    .repeatWhen(new Function<Observable<Object>, ObservableSource<?>>() {
                        @Override
                        public ObservableSource<?> apply(Observable<Object> objectObservable) throws Exception {
                            return objectObservable.takeWhile(new Predicate<Object>() {
                                @Override
                                public boolean test(Object o) throws Exception {
                                    //获取完所有大小后，重新订阅一次逐个下载
                                    num[0]++;
                                    return num[0] == 1;
                                }
                            });
                        }
                    })
                    .subscribe(new Consumer<BasicTask>() {
                        @Override
                        public void accept(BasicTask basicTask) throws Exception {
                            Log.e("zyz", "subscribe");
                        }
                    });
        }

        public void stopAll() {
            for (Map.Entry<String, BasicTask> entry : TaskCenter.getInstance().getTasks().entrySet()) {
                entry.getValue().setCanceld(true);
            }
        }

        private Observable<BasicTask> checkFinish(BasicTask task) {
            return Observable.create(new ObservableOnSubscribe<BasicTask>() {
                @Override
                public void subscribe(ObservableEmitter<BasicTask> emitter) throws Exception {
                    File file = new File(task.getSavePath());
                    if (file.exists()) {
//                        Log.e("zyz", "checkFinish exists");
                        emitter.onComplete();
                    } else {
//                        Log.e("zyz", "checkFinish");
                        emitter.onNext(task);
                        emitter.onComplete();
                    }
                }
            });
        }

        private Observable<BasicTask> download(BasicTask task, TaskBox taskBox, DownloadCallback callback) {
            return Observable.create(new ObservableOnSubscribe<BasicTask>() {
                @Override
                public void subscribe(ObservableEmitter<BasicTask> emitter) throws Exception {
                    if(task.getTotalLength() == 0) emitter.onComplete();
                    task.setCanceld(false);
                    TaskCenter.getInstance().addTask(task);
//            mTasks.get(task.getDownloadUrl()).setLength(getContentLength(task.getDownloadUrl()));
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
                            emitter.onError(new Throwable("download error"));
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            RandomAccessFile rwd = new RandomAccessFile(tempFile, "rwd");
                            rwd.seek(finalLength);
                            Log.e("zyz", "start " + task.getLength() + " " + DownloadUtils.transferSize(task.getLength()) + " randomLength " + DownloadUtils.transferSize(finalLength) + " total " + DownloadUtils.transferSize(taskBox.getLength()));
                            InputStream is = response.body().byteStream();
                            byte[] buffer = new byte[1024 * 8];
                            int len = -1;
                            long progress = finalLength;
                            taskBox.setProgress(taskBox.getProgress() + finalLength);
                            while ((len = is.read(buffer)) != -1 && !task.isCanceld()) {
                                //写入文件
                                rwd.write(buffer, 0, len);
                                progress += len;
                                taskBox.setProgress(taskBox.getProgress() + len);
                                task.setProgress(progress);
                                if (taskBox.getLength() != 0) {
                                    callback.update(taskBox.getProgress(), taskBox.getLength());
                                }
                            }
                            response.body().byteStream().close();
                            rwd.close();
                            if (tempFile.length() >= task.getLength()) {
                                tempFile.renameTo(new File(task.getSavePath()));
                                //任务完成，移除任务
                                TaskCenter.getInstance().removeTask(task.getDownloadUrl());
                                callback.onComplete();
                            }
                            emitter.onNext(task);
                            emitter.onComplete();
                        }
                    });
                }
            });
        }


        /**
         * 如果总长度为空则发起请求获取下载内容的大小
         *
         * @return
         */
        private Observable<BasicTask> getContentLength(BasicTask task) {
            if (task.getLength() == 0) {
                return Observable.create(new ObservableOnSubscribe<BasicTask>() {
                    @Override
                    public void subscribe(ObservableEmitter<BasicTask> emitter) throws Exception {
                        Log.e("zyz", "getContent");
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder().url(task.getDownloadUrl()).build();
                        try {
                            Response response = client.newCall(request).execute();
                            if (response != null && response.isSuccessful()) {
                                long contentLength = response.body().contentLength();
                                response.body().close();
                                task.updateLength(contentLength);
                                emitter.onNext(task);
                                emitter.onComplete();
                            } else {
                                emitter.onError(new Throwable("getContentLength error"));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                return Observable.just(task);
            }
        }

    }
}
