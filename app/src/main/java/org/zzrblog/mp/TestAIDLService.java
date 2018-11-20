package org.zzrblog.mp;
//Multi Process

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import org.zzrblog.ZzrApplication;
import org.zzrblog.blogapp.ITestAIDLInterface;



/**
 * Created by zzr on 2018/11/20.
 */

public class TestAIDLService extends Service {

    private TestAIDLImpl myAidlImpl;
    private String name;

    @Override
    public void onCreate() {
        super.onCreate();
        if(myAidlImpl == null) {
            myAidlImpl = new TestAIDLImpl();
        }

        name = ZzrApplication.getAppName(this, android.os.Process.myPid());

        sendDebugMsg("TestAIDLService onCreated on "+name);
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




    private class TestAIDLImpl extends ITestAIDLInterface.Stub {

        TestAIDLImpl() { }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString)
                throws RemoteException { }

        @Override
        public String getName() throws RemoteException {
            return name + "::TestAIDLService.Impl";
        }

        @Override
        public void addSeconds() throws RemoteException {

        }
    }
}



