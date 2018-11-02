package org.zzrblog.fmod;

/**
 * Created by zzr on 2018/10/26.
 */

public class VoiceEffectUtils {

    //音效的类型
    public static final int MODE_NORMAL = 0;
    public static final int MODE_LUOLI = 1;
    public static final int MODE_DASHU = 2;
    public static final int MODE_JINGSONG = 3;
    public static final int MODE_GAOGUAI = 4;
    public static final int MODE_KONGLING = 5;

    public static native void init();

    public static native void play(int type);

    public static native void release();

    static
    {
        // Try logging libraries...
        try { System.loadLibrary("fmodL");
        } catch (UnsatisfiedLinkError e) { }
        // Try release libraries...
        try { System.loadLibrary("fmod");
        } catch (UnsatisfiedLinkError e) { }

        System.loadLibrary("fmod-effect-lib");
    }
}
