package com.xiaoyu.download;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public interface DownloadCallback {
    void update(long progress, long total);

    void onError(String msg, int code);

    void onComplete();
}
