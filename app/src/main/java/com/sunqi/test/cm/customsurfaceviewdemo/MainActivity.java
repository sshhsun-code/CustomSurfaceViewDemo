package com.sunqi.test.cm.customsurfaceviewdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.sunqi.test.cm.customsurfaceviewdemo.customview.MultiCircleProgressView;

public class MainActivity extends AppCompatActivity {
        MultiCircleProgressView mView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mView = findViewById(R.id.process_view);
        mView.setAngle(200);
    }
}
