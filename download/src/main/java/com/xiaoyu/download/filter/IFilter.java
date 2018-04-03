package com.xiaoyu.download.filter;

import com.xiaoyu.download.DownloadTask;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public interface IFilter {
    void doFilter(List<DownloadTask> tasks, FilterCallback callback);
}
