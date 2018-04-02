package com.xiaoyu.download.task;

import java.util.List;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class TaskBox {

    List<BasicTask> tasks;

    long progress;
    volatile long length;

    public TaskBox(List<BasicTask> tasks) {
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

    public void setLength(long length) {
        this.length = length;
    }
}
