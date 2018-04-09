package com.yazhi1992.download.task;

import com.yazhi1992.download.DownloadTask;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class TaskContainer {

    private String mContainerTag;
    List<DownloadTask> tasks;
    int lenghNum;
    int progresshNum;

    long progress;
    volatile long length; //文件

    public TaskContainer(List<DownloadTask> tasks, String tag) {
        this.tasks = tasks;
        this.mContainerTag = tag;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getLength() {
        return length;
    }

    public void addLength(long length) {
        this.length += length;
        lenghNum++;
    }

    public void addProgress(long progress) {
        this.progress += progress;
        progresshNum++;
    }

    //总长度是否计算完成
    public boolean isLengthCompletion() {
        return lenghNum >= tasks.size() && progresshNum >= tasks.size();
    }



    public String getContainerTag() {
        return mContainerTag;
    }

    public void setContainerTag(String containerTag) {
        mContainerTag = containerTag;
    }
}
