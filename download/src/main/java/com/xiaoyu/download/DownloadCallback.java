package com.xiaoyu.download;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public interface DownloadCallback {
    void update(int progress, int total);

    void onError(String msg, int code);
}
