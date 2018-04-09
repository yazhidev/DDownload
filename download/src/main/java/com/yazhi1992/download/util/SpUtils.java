package com.yazhi1992.download.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by zengyazhi on 2018/4/2.
 */

public class SpUtils {

    private static SharedPreferences sPreferences;
    private static String DOWNLOAD_TASK = "DOWNLOAD_TASK";

    public static void init(Context context) {
        // 默认SharedPreferences的文件名：包名_preferences，模式默认私有访问Context.MODE_PRIVATE
        sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void saveTasksToLocal(String tasksInfo) {
        if(sPreferences == null) throw new InitException();
        SharedPreferences.Editor edit = sPreferences.edit();
        edit.putString(DOWNLOAD_TASK, tasksInfo);
        edit.commit();
    }

    public static String getLocalTasks() {
        return sPreferences.getString(DOWNLOAD_TASK, "");
    }
}
