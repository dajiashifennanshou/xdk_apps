package com.develop.xdk.df.attendancemachine.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.Toast;

import com.develop.xdk.df.attendancemachine.R;
import com.develop.xdk.df.attendancemachine.application.TMApplication;

import butterknife.ButterKnife;


/**
 * Created by Administrator on 2016/8/5.
 */
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.ac_slide_right_in, R.anim.ac_slide_left_out);
        TMApplication.getInstance().addActivity(this);
        ButterKnife.bind(this);
        initView();
    }
    public abstract void initView();
    public void goActivity(Class activity) {
        startActivity(new Intent(this, activity));
    }

    public void toastShort(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    public void toastShort(int s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    public void toastLong(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    public void toastLong(int s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        overridePendingTransition(R.anim.ac_slide_left_in, R.anim.ac_slide_right_out);
        super.finish();
    }
}
