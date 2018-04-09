package com.yazhi1992.download;

import com.yazhi1992.download.task.BasicTask;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class DownloadTask extends BasicTask {

    public DownloadTask() {}

    public DownloadTask(String savePath, String downloadUrl) {
        super(savePath, downloadUrl);
    }
}
