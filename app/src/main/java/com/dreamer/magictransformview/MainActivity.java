package com.dreamer.magictransformview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.dreamer.magictransformview.widget.MagicTransformView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MagicTransformView magicTransformView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        setListener();
    }

    private void initView() {
        magicTransformView = findViewById(R.id.magic_view);
    }

    private void setListener() {
        magicTransformView.setMagicTransformListener(new MagicTransformView.MagicTransformListener() {
            @Override
            public void onSwitchPage(int position) {
                Log.d(TAG, "switch page=" + position);
            }

            @Override
            public void onLeftViewClick() {
                Toast.makeText(MainActivity.this, "click left", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRightViewClick(int state) {
                Toast.makeText(MainActivity.this, "click right", Toast.LENGTH_SHORT).show();
                dummyDownload();
            }
        });
    }

    private void dummyDownload() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(5000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Log.d(TAG, "progress=" + animation.getAnimatedFraction());
                float value = animation.getAnimatedFraction();
                magicTransformView.setRightText("下载中("+((int)(value * 100))+"%)");
                magicTransformView.setDownloadState(MagicTransformView.DOWNLOADING);
                magicTransformView.setDownloadProgress(animation.getAnimatedFraction());
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                magicTransformView.setDownloadState(MagicTransformView.INSTALL_APP);
                magicTransformView.resetDownloadAnim();
                magicTransformView.setRightText("下载(520M)");
            }
        });
        valueAnimator.start();
    }

}
