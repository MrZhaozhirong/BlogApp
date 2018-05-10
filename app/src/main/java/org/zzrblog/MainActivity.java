package org.zzrblog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.cube.CubeActivity;
import org.zzrblog.blogapp.hockey.HockeyActivity;
import org.zzrblog.blogapp.panorama.PanoramaActivity;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    public void clickOnHockey(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, HockeyActivity.class));
    }

    public void clickOnCube(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, CubeActivity.class));
    }

    public void clickOnPanorama(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, PanoramaActivity.class));
    }
}
