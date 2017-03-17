package com.walfud.bindview.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.walfud.bindview.apt.BindView;
import com.walfud.bindviewdemo.R;


public class MainActivity extends Activity {

    @BindView
    public TextView mFooTv;
    @BindView
    public ImageView mIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        com.walfud.bindview.BindView.inject(this);

        findViewById(foo());
//        mTv.setText("bind successfully");
    }

    private int foo() {
        return 123;
    }
}
