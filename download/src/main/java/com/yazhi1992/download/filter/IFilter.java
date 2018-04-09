package com.yazhi1992.download.filter;

import com.yazhi1992.download.DownloadTask;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public interface IFilter {
    void doFilter(List<DownloadTask> tasks, FilterCallback callback);
}
