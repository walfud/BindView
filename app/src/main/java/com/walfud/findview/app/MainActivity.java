package com.walfud.findview.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.walfud.findview.FindView;
import com.walfud.findview.annotation.Find;
import com.walfud.findviewdemo.R;


public class MainActivity extends Activity {

    @Find
    public TextView mTv;
    @Find
    public ImageView mFooIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FindView.inject(this);

        mTv.setText("Hello Find!");
        mFooIv.setImageResource(R.mipmap.ic_launcher_round);
    }
}
