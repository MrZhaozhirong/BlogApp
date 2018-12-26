package org.zzrblog.ffmp;
//Multi Process

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import org.zzrblog.ZzrApplication;
import org.zzrblog.blogapp.IFFmpegAIDLInterface;



/**
 * Created by zzr on 2018/11/20.
 */

public class FFmpegAIDLService extends Service {

    private FFmpegAIDLImpl myAidlImpl;
    private String name;

    @Override
    public void onCreate() {
        super.onCreate();
        if(myAidlImpl == null) {
            myAidlImpl = new FFmpegAIDLImpl();
        }

        name = ZzrApplication.getAppName(this, android.os.Process.myPid());

        sendDebugMsg("FFmpegAIDLService onCreated on "+name);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myAidlImpl;
    }

    void sendDebugMsg(String msg) {
        Intent intent = new Intent(ZzrApplication.DEBUG_ACTION);
        intent.putExtra("DEBUG_MSG", msg);
        sendBroadcast(intent);
        Log.d(ZzrApplication.TAG, msg);
    }




    private class FFmpegAIDLImpl extends IFFmpegAIDLInterface.Stub {

        FFmpegAIDLImpl() { }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString)
                throws RemoteException { }

        @Override
        public String getName() throws RemoteException {
            return name + "::IFFmpegAIDLInterface.Impl";
        }

        //@Override
        //public int Mp4_TO_YUV(String input_mp4_str, String output_yuv_str, String output_h264_str) throws RemoteException {
        //    sendDebugMsg(name+" begin Mp4_TO_YUV");
        //    return ZzrFFmpeg.Mp4TOYuv(input_mp4_str, output_yuv_str, output_h264_str);
        //}

    }
}



