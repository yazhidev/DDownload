package com.yazhi1992.download.task;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yazhi1992.download.DownloadTask;
import com.yazhi1992.download.util.SpUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class TaskCenter {

    private Map<String, DownloadTask> mTasks = new HashMap<>();

    private TaskCenter() {
    }

    private static class TaskCenterHolder {
        private static TaskCenter INSTANCE = new TaskCenter();
    }

    public void init(Context context) {
        SpUtils.init(context);
        String localTasks = SpUtils.getLocalTasks();
        if(!localTasks.isEmpty()) {
            stopAll();
            mTasks.clear();
            Map<String, DownloadTask> tasks = (Map<String, DownloadTask>)JSON.parseObject(localTasks, new TypeReference<Map<String, DownloadTask>>(){});
            for (Map.Entry<String, DownloadTask> entry : tasks.entrySet()) {
                entry.getValue().setCanceld(true);
                DownloadTask value = entry.getValue();
                DownloadTask basicTask = new DownloadTask(value.getSavePath(), value.getDownloadUrl());
                basicTask.setLength(value.getLength());
                basicTask.setCanceld(true);
                mTasks.put(entry.getKey(), basicTask);
            }
        }
    }

    public static TaskCenter getInstance() {
        return TaskCenter.TaskCenterHolder.INSTANCE;
    }

    public void addTask(DownloadTask task) {
        if(contains(task.getDownloadUrl())) {
            //获取之前缓存的需要的数据
            DownloadTask basicTask = get(task.getDownloadUrl());
            task.setLength(basicTask.getLength());
            task.setProgress(basicTask.getProgress());
            mTasks.put(task.getDownloadUrl(), task);
        }
        mTasks.put(task.getDownloadUrl(), task);

        updateLocalData();
    }

    //保存至本地
    public void updateLocalData() {
        if(mTasks == null || mTasks.isEmpty()) {
            SpUtils.saveTasksToLocal("");
        } else {
            SpUtils.saveTasksToLocal(JSON.toJSONString(mTasks));
        }
    }

    public void removeTask(String key) {
        mTasks.remove(key);
        updateLocalData();
    }

    public void removeAll() {
        mTasks.clear();
        updateLocalData();
    }

    public boolean isDownloading(DownloadTask key) {
        return contains(key.getDownloadUrl()) && !get(key.getDownloadUrl()).isCanceld();
    }

    public boolean contains(String key) {
        return mTasks.containsKey(key);
    }

    public DownloadTask get(String key) {
        return mTasks.get(key);
    }

    public Map<String, DownloadTask> getTasks() {
        return mTasks;
    }

    public void stopAll() {
        for (Map.Entry<String, DownloadTask> entry : getTasks().entrySet()) {
            entry.getValue().setCanceld(true);
        }
    }
}
