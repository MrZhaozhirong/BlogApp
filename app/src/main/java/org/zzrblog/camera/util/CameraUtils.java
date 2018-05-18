package org.zzrblog.camera.util;

import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.util.Log;

import org.zzrblog.camera.ContinuousRecordActivity;

import java.util.List;

/**
 * Created by zzr on 2017/11/14.
 */

public class CameraUtils {
    private static final String TAG = ContinuousRecordActivity.TAG;

    public static void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.println("\t"+"0x"+ Integer.toHexString(c));
        }
        System.out.println();
    }
    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     *
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred size,
        // and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }
        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }


    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     *
     * It doesn't seem like there's a great deal of flexibility here.
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }
}
