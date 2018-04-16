package com.yazhi1992.download;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.yazhi1992.download.task.TaskCenter;
import com.yazhi1992.download.task.TaskContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
            final int[] num = {0}; //repeatWhen 接收到doOnComplete就会重复订阅。而我们只需要重复订阅一次。第一次遍历订阅用于获取文件总长度/已下载长度。第二次遍历订阅用于下载。
            TaskContainer taskContainer = new TaskContainer(tasks, tasksTag);
            Observable.fromIterable(tasks)
                    .observeOn(Schedulers.newThread())
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask basicTask) throws Exception {
                            //如果已下载，则跳过
                            return checkFinish(basicTask, taskContainer);
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
                            if(num[0] == 0) {
                                return getContentLength(basicTask, taskContainer);
                            } else {
                                return Observable.just(basicTask);
                            }
                        }
                    })
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask downloadTask) throws Exception {
                            if(num[0] == 0) {
                                return getDownloadedLength(downloadTask, taskContainer);
                            } else {
                                return Observable.just(downloadTask);
                            }
                        }
                    })
                    .observeOn(Schedulers.io())
                    .concatMap(new Function<DownloadTask, ObservableSource<DownloadTask>>() {
                        @Override
                        public ObservableSource<DownloadTask> apply(DownloadTask basicTask) throws Exception {
                            if(num[0] == 1) {
                                //获取完总任务进度后开始下载（否则总进度回调异常）
                                return download(basicTask, num[0], taskContainer, callback);
                            } else {
                                return Observable.just(basicTask);
                            }
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            //第一次回调在获取完所有文件大小后
                            //第二次回调在下载完所有文件后
                            if (num[0] == 1) {
                                callback.onComplete();
                            }
                        }
                    })
                    .observeOn(Schedulers.newThread())
                    .repeatWhen(new Function<Observable<Object>, ObservableSource<?>>() {
                        @Override
                        public ObservableSource<?> apply(Observable<Object> objectObservable) throws Exception {
                            return objectObservable.takeWhile(new Predicate<Object>() {
                                @Override
                                public boolean test(Object o) throws Exception {
                                    num[0]++;
                                    return num[0] == 1; //只需要重复订阅一次
                                }
                            });
                        }
                    })
                    .subscribe(new Consumer<DownloadTask>() {
                        @Override
                        public void accept(DownloadTask basicTask) throws Exception {
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                        }
                    });
        }

        public void stopAll() {
            TaskCenter.getInstance().stopAll();
        }

        private Observable<DownloadTask> checkFinish(DownloadTask task, TaskContainer taskContainer) {
            return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                @Override
                public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                    File file = new File(task.getSavePath());
                    if (file.exists()) {
                        emitter.onComplete();
                    } else {
                        emitter.onNext(task);
                        emitter.onComplete();
                    }
                }
            });
        }

        private Observable<DownloadTask> download(DownloadTask task, int num, TaskContainer taskContainer, DownloadListener callback) {
            return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                @Override
                public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                    //继续下一个下载任务
                    emitter.onNext(task);
                    task.setCanceld(false);
                    TaskCenter.getInstance().addTask(task);
                    long length = task.getProgress();
                    File tempFile = new File(task.getTempFilePath());
                    Request request = new Request.Builder()
                            .addHeader("RANGE", "bytes=" + length + "-")
                            .url(task.getDownloadUrl()).build();
                    long finalLength = length;
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
                                if (DDownload.getInstance().getProgressListener() != null) {
                                    DDownload.getInstance().getProgressListener().taskProgress(task.getDownloadUrl(), task.getProgress(), task.getLength());
                                    if (taskContainer.isLengthCompletion() && taskContainer.getLength() != 0) {
                                        DDownload.getInstance().getProgressListener().taskContainerProgress(taskContainer.getContainerTag(), taskContainer.getProgress(), taskContainer.getLength());
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
            //新下载任务，需要先获取文件长度
            return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                @Override
                public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                    if (task.getLength() == 0) {
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
                    } else {
                        //如果是已添加过的任务，则已知文件长度
                        taskContainer.addLength(task.getLength());
                        emitter.onNext(task);
                        emitter.onComplete();
                    }
                }
            });
        }

        /**
         * 计算已下载文件大小
         *
         * @return
         */
        private Observable<DownloadTask> getDownloadedLength(DownloadTask task, TaskContainer taskContainer) {
            return Observable.create(new ObservableOnSubscribe<DownloadTask>() {
                @Override
                public void subscribe(ObservableEmitter<DownloadTask> emitter) throws Exception {
                    if (task.getProgress() == 0) {
                        File tempFile = new File(task.getTempFilePath());
                        if (!tempFile.getParentFile().exists())
                            tempFile.getParentFile().mkdirs();
                        long length = 0;
                        if (tempFile.exists()) {
                            length = tempFile.length();
                        }
                        task.setProgress(length);
                    }
                    taskContainer.addProgress(task.getProgress());
                    emitter.onNext(task);
                    emitter.onComplete();
                }
            });
        }

    }
}
