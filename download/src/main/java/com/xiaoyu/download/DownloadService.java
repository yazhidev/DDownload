package com.xiaoyu.download;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xiaoyu.download.task.TaskCenter;
import com.xiaoyu.download.task.TaskContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

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
    private OkHttpClient mOkHttpClient = new OkHttpClient();

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

        public void start(List<DownloadTask> tasks, String tasksTag, DownloadListener callback) {
            final long[] total = {0};
            final int[] num = {0};
            TaskContainer taskContainer = new TaskContainer(tasks, tasksTag);
            Observable.fromIterable(tasks)
                    .observeOn(Schedulers.newThread())
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask basicTask) throws Exception {
                            return checkFinish(basicTask);
                        }
                    })
                    .filter(new Predicate<DownloadTask>() {
                        @Override
                        public boolean test(DownloadTask basicTask) throws Exception {
                            //如果在下载中则跳过
                            return !TaskCenter.getInstance().isDownloading(basicTask);
                        }
                    })
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask basicTask) throws Exception {
                            return getContentLength(basicTask, taskContainer);
                        }
                    })
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask downloadTask) throws Exception {
                            return getDownloadLength(downloadTask, taskContainer);
                        }
                    })
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask basicTask) throws Exception {
                            return download(basicTask, taskContainer, callback);
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            //第一次回调在获取完所有文件大小后
                            //第二次回调在下载完所有文件后
                            if(num[0] == 1) {
                                callback.onComplete();
                            }
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
                    .subscribe(new Consumer<DownloadTask>() {
                        @Override
                        public void accept(DownloadTask basicTask) throws Exception {
                            Log.e("zyz", "subscribe");
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e("zyz", "start download error:" + throwable.getMessage());
                        }
                    });
        }

        public void stopAll() {
            TaskCenter.getInstance().stopAll();
        }

        private Observable<DownloadTask> checkFinish(DownloadTask task) {
            return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                @Override
                public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
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

        private Observable<DownloadTask> download(DownloadTask task, TaskContainer taskContainer, DownloadListener callback) {
            return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                @Override
                public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                    if(task.getTotalLength() == 0) emitter.onComplete();
                    task.setCanceld(false);
                    TaskCenter.getInstance().addTask(task);
//            mTasks.get(task.getDownloadUrl()).addLength(getContentLength(task.getDownloadUrl()));
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
                    Log.e("zyz", "length " + finalLength);
                    mOkHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            callback.onError("error", -1);
                            emitter.onError(new Throwable("download error"));
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            RandomAccessFile rwd = new RandomAccessFile(tempFile, "rwd");
                            rwd.seek(finalLength);
//                            Log.e("zyz", "start " + task.getLength() + " " + DownloadUtils.transferSize(task.getLength()) + " randomLength " + DownloadUtils.transferSize(finalLength) + " total " + DownloadUtils.transferSize(taskContainer.getLength()));
                            InputStream is = response.body().byteStream();
                            byte[] buffer = new byte[1024 * 8];
                            int len = -1;
                            long progress = finalLength;
                            while ((len = is.read(buffer)) != -1 && !task.isCanceld()) {
                                //写入文件
                                rwd.write(buffer, 0, len);
                                progress += len;
                                taskContainer.setProgress(taskContainer.getProgress() + len);
                                task.setProgress(progress);
                                //进度回调
                                if(XYDownload.getInstance().getProgressListener() != null) {
                                    XYDownload.getInstance().getProgressListener().taskProgress(task.getDownloadUrl(), task.getProgress(), task.getLength());
                                    if (taskContainer.isLengthCompletion() && taskContainer.getLength() != 0) {
                                        XYDownload.getInstance().getProgressListener().taskContainerProgress(taskContainer.getContainerTag(), taskContainer.getProgress(), taskContainer.getLength());
                                    }
                                }
                            }
                            response.body().byteStream().close();
                            rwd.close();
                            if (tempFile.length() >= task.getLength()) {
                                tempFile.renameTo(new File(task.getSavePath()));
                                //任务完成，移除任务
                                TaskCenter.getInstance().removeTask(task.getDownloadUrl());
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
        private Observable<DownloadTask> getContentLength(DownloadTask task, TaskContainer taskContainer) {
            if (task.getLength() == 0) {
                return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                        Log.e("zyz", "getContent");
                        Request request = new Request.Builder().url(task.getDownloadUrl()).build();
                        try {
                            Response response = mOkHttpClient.newCall(request).execute();
                            if (response != null && response.isSuccessful()) {
                                long contentLength = response.body().contentLength();
                                response.body().close();
                                task.updateLength(contentLength);
                                taskContainer.addLength(task.getLength());
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
                taskContainer.addLength(task.getLength());
                return Observable.just(task);
            }
        }

        /**
         * 计算已下载文件大小
         *
         * @return
         */
        private Observable<DownloadTask> getDownloadLength(DownloadTask task, TaskContainer taskContainer) {
            if (taskContainer.isLengthCompletion()) {
                return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                        Log.e("zyz", "getDownloadLength");
                        File tempFile = new File(task.getTempFilePath());
                        if (!tempFile.getParentFile().exists())
                            tempFile.getParentFile().mkdirs();
                        long length = 0;
                        if (tempFile.exists()) {
                            length = tempFile.length();
                        }
                        taskContainer.addProgress(length);
                        emitter.onNext(task);
                        emitter.onComplete();
                    }
                });
            } else {
                return Observable.just(task);
            }
        }

    }
}
