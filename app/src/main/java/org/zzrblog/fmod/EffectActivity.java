package org.zzrblog.fmod;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import org.zzrblog.blogapp.R;

/**
 * Created by zzr on 2018/10/29.
 */

public class EffectActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fmod_effect);
        org.fmod.FMOD.init(this);
        VoiceEffectUtils.init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceEffectUtils.release();
        org.fmod.FMOD.close();
    }

    public void onClickedEffect(View btn) {
        //String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.pathSeparator + "singing.wav";
        //String path = "file:///android_asset/"+"dfb.mp3";
        switch (btn.getId()) {
            case R.id.btn_normal:
                VoiceEffectUtils.play(VoiceEffectUtils.MODE_NORMAL);
                break;
            case R.id.btn_luoli:
                VoiceEffectUtils.play(VoiceEffectUtils.MODE_LUOLI);
                break;
            case R.id.btn_dashu:
                VoiceEffectUtils.play(VoiceEffectUtils.MODE_DASHU);
                break;
            case R.id.btn_jingsong:
                VoiceEffectUtils.play(VoiceEffectUtils.MODE_JINGSONG);
                break;
            case R.id.btn_gaoguai:
                VoiceEffectUtils.play(VoiceEffectUtils.MODE_GAOGUAI);
                break;
            case R.id.btn_kongling:
                VoiceEffectUtils.play(VoiceEffectUtils.MODE_KONGLING);
                break;
            default:
                break;
        }
    }
}
