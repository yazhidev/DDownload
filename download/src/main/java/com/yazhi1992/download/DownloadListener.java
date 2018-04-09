package com.yazhi1992.download;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public interface DownloadListener {

    void onError(String msg, int code);

    void onComplete();
}
