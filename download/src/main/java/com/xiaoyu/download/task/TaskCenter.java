package com.xiaoyu.download.task;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xiaoyu.download.util.SpUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class TaskCenter {

    private Map<String, BasicTask> mTasks = new HashMap<>();

    private TaskCenter() {
    }

    private static class TaskCenterHolder {
        private static TaskCenter INSTANCE = new TaskCenter();
    }

    public void init(Context context) {
        SpUtils.init(context);
        String localTasks = SpUtils.getLocalTasks();
        if(!localTasks.isEmpty()) {
            Map<String, BasicTask> tasks = (Map<String, BasicTask>)JSON.parseObject(localTasks, new TypeReference<Map<String, BasicTask>>(){});
            for (Map.Entry<String, BasicTask> entry : tasks.entrySet()) {
                entry.getValue().setCanceld(true);
                BasicTask value = entry.getValue();
                BasicTask basicTask = new BasicTask(value.getSavePath(), value.getDownloadUrl());
                basicTask.setLength(value.getLength());
                basicTask.setTotalLength(value.getTotalLength());
                basicTask.setCanceld(true);
                mTasks.put(entry.getKey(), basicTask);
            }
        }
    }

    public static TaskCenter getInstance() {
        return TaskCenter.TaskCenterHolder.INSTANCE;
    }

    public void addTask(BasicTask task) {
        if(contains(task.getDownloadUrl())) {
            //获取之前缓存的需要的数据
            BasicTask basicTask = get(task.getDownloadUrl());
            task.setLength(basicTask.getLength());
            task.setTotalLength(basicTask.getTotalLength());
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

    public boolean isDownloading(BasicTask key) {
        return contains(key.getDownloadUrl()) && !get(key.getDownloadUrl()).isCanceld();
    }

    public boolean contains(String key) {
        return mTasks.containsKey(key);
    }

    public BasicTask get(String key) {
        return mTasks.get(key);
    }

    public Map<String, BasicTask> getTasks() {
        return mTasks;
    }
}
