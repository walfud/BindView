package com.walfud.dustofappearancedemo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.walfud.dustofappearance.DustOfAppearance;
import com.walfud.dustofappearance.annotation.FindView;
import com.walfud.dustofappearance.annotation.OnClick;
import com.walfud.dustofappearancedemo.R;


public class MainActivity extends Activity {

    @FindView
    TextView mTv;
    @FindView
    private ImageView mFooIv;
    @FindView
    ViewGroup mVg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DustOfAppearance.inject(this);

        mTv.setText("Hello Find!");
        mFooIv.setImageResource(R.mipmap.ic_launcher_round);

        new Inner(mVg);
    }

    @OnClick
    public void onClickCancelBtn(View view) {
        Toast.makeText(this, "public listener test ok", Toast.LENGTH_SHORT).show();
    }
    @OnClick
    private void onClickOkBtn(View view) {
        Toast.makeText(this, "PRIVATE listener test ok", Toast.LENGTH_SHORT).show();
    }

    //
    static class Inner {
        @FindView
        TextView mCancelBtn;

        public Inner(View view) {
            DustOfAppearance.inject(this, view);
            mCancelBtn.setAlpha(0.5f);
        }
    }
}
