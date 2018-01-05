package com.develop.xdk.df.attendancemachine.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2017/11/29.
 */

public class SingleThreadUtil {
    static ExecutorService singleThreadExecutor = null;
    public static ExecutorService getSingleThreadUtil(){
        if(singleThreadExecutor == null){
            singleThreadExecutor = Executors.newSingleThreadExecutor();
        }
        return singleThreadExecutor;
    }
}
