package org.zzrblog.fmod;

/**
 * Created by zzr on 2018/10/26.
 */

public class VoiceEffectUtils {


    private native void play(String path, int type);


    static
    {

        // Try logging libraries...
        try { System.loadLibrary("fmodL");
        } catch (UnsatisfiedLinkError e) { }
        // Try release libraries...
        try { System.loadLibrary("fmod");
        } catch (UnsatisfiedLinkError e) { }

        //System.loadLibrary("stlport_shared");
        System.loadLibrary("fmod-voice-lib");
    }
}
