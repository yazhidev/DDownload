package com.xiaoyu.download;

import com.xiaoyu.download.filter.IFilter;
import com.xiaoyu.download.task.BasicTask;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public interface IDownload {

    void start(String url);

    void start(List<String> urls);

    void start(BasicTask task);

    //开始执行所有队列中的任务
    void startAll();

    void stopAll();

    //获取所有执行中的任务
    List<BasicTask> getAllTasks();

    //本地是否有文件
    void isFinish();

    //是否正在执行下载
    void isDownloading();

    //拦截器（下载前判断是否是3G）
    void buildFilter(List<IFilter> filters);
}
