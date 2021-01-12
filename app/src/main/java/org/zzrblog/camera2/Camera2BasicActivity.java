package org.zzrblog.camera2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.zzrblog.blogapp.R;

/**
 * Created by zzr on 2021/1/12.
 */

public class Camera2BasicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2basic);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
    }
}
