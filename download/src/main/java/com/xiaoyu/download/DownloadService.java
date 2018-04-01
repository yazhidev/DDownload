package com.xiaoyu.download;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

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
    private String url = "http://pubgm.qq.com/d/index.html?device=android";
    private String url2 = "http://imtt.dd.qq.com/16891/3379F96C4ED3740C015C2F39C3ED44AC.apk?fsname=com.tencent.mobileqq_7.5.5_806.apk&csr=1bbd";
    private String path = Environment.getExternalStorageDirectory() + "/test.apk";

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

        public void start(DownloadCallback callback) {
            // retrofit   @Streaming/*大文件需要加入这个判断，防止下载过程中写入到内存中*/
            Request request = new Request.Builder()
                    .addHeader("RANGE","bytes="+0+"-")
                    .url(url2).build();
            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("error", -1);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    File file = new File(path);
                    if (!file.getParentFile().exists())
                        file.getParentFile().mkdirs();
                    RandomAccessFile rwd = new RandomAccessFile(file, "rwd");
                    rwd.seek(0);
//                    rwd.seek(info.getReadLength());
                    InputStream is = response.body().byteStream();
                    byte[] buffer = new byte[1024 * 8];
                    int len = -1;
                    while ((len = is.read(buffer)) != -1) {
                        //写入文件
                        rwd.write(buffer, 0, len);
                    }
                    response.body().byteStream().close();
                    rwd.close();
                }
            });
        }
    }
}
