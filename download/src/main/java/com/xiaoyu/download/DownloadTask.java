package com.xiaoyu.download;

import com.xiaoyu.download.task.BasicTask;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class DownloadTask extends BasicTask {

    public DownloadTask(String savePath, String downloadUrl) {
        super(savePath, downloadUrl);
    }
}
