package com.yazhi1992.ddownload;

import android.app.Application;

import com.yazhi1992.download.DDownload;


/**
 * Created by zengyazhi on 2018/4/1.
 */

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DDownload.getInstance().init(this);
    }
}
