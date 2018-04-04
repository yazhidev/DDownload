package com.xiaoyu.download.util;

/**
 * Created by zengyazhi on 2018/4/4.
 */

public class InitException extends RuntimeException {

    public InitException() {
        super("forget to init XYDownload ?");
    }
}
