package com.xiaoyu.download;

/**
 * Created by zengyazhi on 2018/4/3.
 */

public interface ProgressListener {
    void taskProgress(String downloadUrl, long progress, long total);

    void taskContainerProgress(String containerTag, long progress, long total);
}
