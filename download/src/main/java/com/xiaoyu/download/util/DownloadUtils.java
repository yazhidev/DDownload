package com.xiaoyu.download.util;

import java.text.DecimalFormat;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class DownloadUtils {

    /**
     * 将单位为 B 的值转换其他单位
     *
     * @return
     */
    public static String transferSize(long size) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (size == 0) {
            fileSizeString = "0.00B";
        } else if (size < 1024) {
            fileSizeString = df.format((double) size) + "B";
        } else if (size < 1048576) {
            fileSizeString = df.format((double) size / 1024) + "K";
        } else if (size < 1073741824) {
            fileSizeString = df.format((double) size / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) size / 1073741824) + "G";
        }
        return fileSizeString;
    }
}
