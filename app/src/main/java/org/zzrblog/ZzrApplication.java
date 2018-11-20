package org.zzrblog;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

/**
 * Created by zzr on 2018/11/20.
 */

public class ZzrApplication extends Application {

    public static final String TAG = "ZzrBlogApp";
    public static final String DEBUG_ACTION = "org.zzrblog.DEBUG_MSG_ACTION";

    @Override
    public void onCreate() {
        super.onCreate();

        int pid = android.os.Process.myPid();
        Log.i(TAG, getAppName(this, pid)+" onCreated ...");
    }


    /**
     * 根据Pid获取当前进程的名字
     * @param context 上下文
     * @param pid 进程的id
     * @return 返回进程的名字
     */
    public static String getAppName(Context context, int pid)
    {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List list = activityManager.getRunningAppProcesses();
        Iterator i = list.iterator();
        while (i.hasNext())
        {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try
            {
                if (info.pid == pid)
                {
                    // 根据进程的信息获取当前进程的名字
                    return info.processName;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        // 没有匹配的项，返回为null
        return null;
    }
}
