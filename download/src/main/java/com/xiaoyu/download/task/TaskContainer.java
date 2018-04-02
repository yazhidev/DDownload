package com.xiaoyu.download.task;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class TaskContainer {

    List<BasicTask> tasks;
    int lenghNum;

    long progress;
    volatile long length;

    public TaskContainer(List<BasicTask> tasks) {
        this.tasks = tasks;
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

    //总长度是否计算完成
    public boolean isLengthCompletion() {
        return lenghNum == tasks.size();
    }

}
