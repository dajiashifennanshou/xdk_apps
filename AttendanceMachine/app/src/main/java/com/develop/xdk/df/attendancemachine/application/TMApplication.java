package com.develop.xdk.df.attendancemachine.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.reader.ReaderAndroid;

import com.develop.xdk.df.attendancemachine.jni.SerialPortFinder;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2017/1/19.
 */
public class TMApplication extends Application {
    private List<Activity> activityList = new LinkedList();
    private static TMApplication instance;
    private static Context context;
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    public ReaderAndroid mSerialPort = null;
    public static  int baudrate = 9600;
    public static String path = "/dev/ttyS2";

    public ReaderAndroid getSerialPort() throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
//            /* Read serial port parameters from  android.reader_preferences.xml*/
//            SharedPreferences sp = getSharedPreferences("android.reader_preferences", MODE_PRIVATE);
//            String path = sp.getString("DEVICE", "");
//
//            int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));
//            if ((path.length() == 0) || (baudrate == -1)) {
//                throw new InvalidParameterException();
//            }
			/* Open the serial port */
            mSerialPort = new ReaderAndroid(new File(path), baudrate);

        }
        return mSerialPort;
    }

    public void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close(mSerialPort.getHandle());
            mSerialPort = null;
        }
    }

    @Override
    public void onCreate() {
        context = getApplicationContext();
        super.onCreate();
    }

    public static TMApplication getInstance() {
        if (null == instance) {
            instance = new TMApplication();
        }
        return instance;
    }

    public List getActivityList() {
        return activityList;
    }

    //添加Activity到容器中
    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    //遍历所有Activity并finish
    public void exit() {
        for (Activity activity : activityList) {
            activity.finish();
        }
        System.exit(0);
    }

    public static Context getContext() {
        return context;
    }
}
