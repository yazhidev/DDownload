package com.yazhi1992.download.filter;

import com.yazhi1992.download.DownloadTask;

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
