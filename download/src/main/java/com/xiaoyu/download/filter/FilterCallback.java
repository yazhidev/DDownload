package com.xiaoyu.download.filter;

import com.xiaoyu.download.DownloadTask;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/3.
 */

public interface FilterCallback {
    //继续向下传递
    void onContinue(List<DownloadTask> tasks);

    //拦截
    void onInterrupt(String msg, int code);
}
