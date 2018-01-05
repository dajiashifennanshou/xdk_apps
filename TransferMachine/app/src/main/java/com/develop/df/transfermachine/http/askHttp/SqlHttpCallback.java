package com.develop.df.transfermachine.http.askHttp;

/**
 * Created by Administrator on 2017/9/4.
 */
public interface SqlHttpCallback {
    void onRespose(String msg);
    void onError(String msg);
}
