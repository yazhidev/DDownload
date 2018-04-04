package com.xiaoyu.download.filter;

import com.xiaoyu.download.DownloadTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zengyazhi on 2018/4/3.
 */

public class FilterChain {

    List<IFilter> mFilters = new ArrayList<>();

    public void filter(List<DownloadTask> tasks, FilterCallback callback) {
        if(mFilters.isEmpty()) {
            callback.onContinue(tasks);
        } else {
            startFilter(tasks, 0, callback);
        }
    }

    private void startFilter(List<DownloadTask> tasks, int index, FilterCallback callback) {
        if(index < mFilters.size()) {
            IFilter iFilter = mFilters.get(index);
            iFilter.doFilter(tasks, new FilterCallback() {
                @Override
                public void onContinue(List<DownloadTask> thisTask) {
                    startFilter(thisTask, index + 1, callback);
                }

                @Override
                public void onInterrupt(String msg, int code) {
                    callback.onInterrupt(msg, code);

                }
            });
        } else {
            callback.onContinue(tasks);
        }
    }

    public void add(IFilter filter) {
        mFilters.add(filter);
    }
}
