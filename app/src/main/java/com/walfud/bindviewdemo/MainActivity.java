package com.walfud.bindviewdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.BindView;

public class MainActivity extends Activity {

    @BindView
    private TextView mTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        com.walfud.bindview.BindView.inject(this);

        mTv.setText("bind successfully");
    }
}
