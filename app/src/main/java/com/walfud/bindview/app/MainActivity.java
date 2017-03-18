package com.walfud.bindview.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.walfud.bindview.BindView;
import com.walfud.bindview.apt.Bind;
import com.walfud.bindviewdemo.R;


public class MainActivity extends Activity {

    @Bind
    public TextView mTv;
    @Bind
    public ImageView mFooIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BindView.inject(this);

        mTv.setText("Hello Bind!");
        mFooIv.setImageResource(R.mipmap.ic_launcher_round);
    }
}
