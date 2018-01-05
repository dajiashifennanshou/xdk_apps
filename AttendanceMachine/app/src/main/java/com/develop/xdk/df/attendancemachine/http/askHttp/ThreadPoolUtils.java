package com.develop.xdk.df.attendancemachine.http.askHttp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2017/9/5.
 */
public class ThreadPoolUtils {
    public static  ExecutorService executorService;
     static {
         executorService = Executors.newSingleThreadExecutor();
     }
    public static ExecutorService getInstance(){
        synchronized (ThreadPoolUtils.class){
            if(executorService == null){
                executorService = Executors.newSingleThreadExecutor();
                return executorService;
            }
        }
        return  executorService;
    }
}
