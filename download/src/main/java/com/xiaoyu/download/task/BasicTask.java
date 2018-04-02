package com.xiaoyu.download.task;

import java.io.File;
import java.io.Serializable;

/**
 * Created by zengyazhi on 2018/4/1.
 */

public class BasicTask implements Serializable {
    protected int mStatus;
    protected boolean mIsCanceld;
    protected String mTempFilePath;
    protected String mSavePath;
    protected String mDownloadUrl;
    protected long mProgress;
    protected long mTotal;

    public BasicTask() {}

    public BasicTask(String savePath, String downloadUrl) {
        mSavePath = savePath;
        mDownloadUrl = downloadUrl;
        setTempFilePath(savePath + ".download");
        setTempProgress();
    }

    private void setTempProgress() {
        File file = new File(mSavePath);
        if (!file.exists()) {
            File tempFile = new File(mTempFilePath);
            setProgress(tempFile.length());
        }
    }

    public boolean isCanceld() {
        return mIsCanceld;
    }

    public void setCanceld(boolean canceld) {
        mIsCanceld = canceld;
    }

    public String getTempFilePath() {
        return mTempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        mTempFilePath = tempFilePath;

    }

    public String getSavePath() {
        return mSavePath;
    }

    public void setSavePath(String savePath) {
        mSavePath = savePath;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    public long getProgress() {
        return mProgress;
    }

    public void setProgress(long progress) {
        mProgress = progress;
    }

    public long getTotal() {
        return mTotal;
    }

    public void setTotal(long total) {
        mTotal = total;
    }

    public void updateTotal(long total) {
        setTotal(total);
        TaskCenter.getInstance().updateLocalData();
    }
}
