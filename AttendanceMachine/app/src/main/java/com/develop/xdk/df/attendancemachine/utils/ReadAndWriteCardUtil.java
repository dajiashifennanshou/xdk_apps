package com.develop.xdk.df.attendancemachine.utils;

import android.app.Activity;
import android.os.Handler;
import android.reader.ReaderAndroid;
import android.text.TextUtils;


import com.develop.xdk.df.attendancemachine.application.TMApplication;

import java.io.IOException;
import java.util.Properties;

/**
 * 读写卡的再封装
 */
public class ReadAndWriteCardUtil {
    //返回sn号码结果
    protected static byte[] card_flag = new byte[2];
    //读写卡so类
    protected static ReaderAndroid mSerialPort = null;
    protected static int handle;
    //数据容器
    protected static byte[] ver = new byte[20];
    static {
        if(mSerialPort == null){
            try {
                mSerialPort = TMApplication.getInstance().getSerialPort();
                handle = mSerialPort.getHandle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static String readSnNumber(Handler handler, final Activity activity){
        int ret = mSerialPort.getCardSN(handle, C.CARD_ADDRESS, C.CARD_MODE,C.CARD_HALT, card_flag, ver);
        String snnumber = CommonUtil.ByteToStr(card_flag[1], ver);
        if(snnumber != null){
            snnumber = snnumber.replace(" ","");
        }
        if(0 == ret) {
            return snnumber;
        }else if(1 == ret){
            LogUtils.e("请将卡放到转存机指定位置");
        }else if(ret == 4){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.showShort(activity,"设备连接失败");
                }
            });
        }else{
            LogUtils.e("错误码："+ret);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.showShort(activity,"读卡错误");
                }
            });
        }
        if(snnumber == null ||snnumber.length() != 8){
            return null;
        }
        return null;
    }

    public  static String readCardBlock(Handler handler, final Activity ac,String snnumber){
        String key = getCardPass(handler,ac,snnumber);
        Properties properties = CommonUtil.loadConfig(ac,C.FILE_PATH);
        String block = properties.getProperty(C.CARD_ADRESS,"null");
        if(block.equals("null")){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.showShort(ac,"请配置读写块参数");
                }
            });
            return null;
        }
        byte mblock = (byte)(Integer.valueOf(block)&0xff);
        byte[] password = new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
        if(password == null||CommonUtil.strToByte(key.toUpperCase(),6,password)!=0){
            return null;
        }
        int result = mSerialPort.MFRead(handle, C.CARD_ADDRESS, C.CARD_MODE,mblock,(byte)(0x01&0xff), password, ver);
        if(result == 0){
           return CommonUtil.ByteToStr(16, ver);
        }else{
            final String reslut = "key"+key+"读卡错误，错误码："+result+"状态码："+CommonUtil.ByteToStr(1, ver);
            LogUtils.e(reslut);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.showShort(ac,reslut);
                }
            });
        }
        return  null;
    }
    
    public static String getCardPass(Handler handler,Activity activity,String snNumber){
        if(TextUtils.isEmpty(snNumber)||snNumber.length() != 8){
            return null;
        }
        String key = null;
        char[] usercode = { 0x85 , 0x3F , 0x4E , 0xA7 , 0x91 , 0x62 };
        char[] sn = new char[6];

        return key.toUpperCase();

    }
    public static boolean writeCardBlock(String content, Handler handler, final Activity activity, byte which, String snnumber){
        String key = getCardPass(handler,activity,snnumber);
        byte[] password = new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
        if(key == null||password == null ||CommonUtil.strToByte(key,6,password)!=0){
            return false;
        }
        byte[] value=new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
                (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
                (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
                (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
        if(!TextUtils.isEmpty(content)){
            if(CommonUtil.strToByte(content,16,value) != 0) {
                LogUtils.e("写入内容有误,必须为16字节32字符");
                return false;
            }
        }else{
            LogUtils.e("请输入写入内容");
            return false;
        }
        String data = CommonUtil.ByteToStr(16,value);
        int write =   mSerialPort.MFWrite(handle, C.CARD_ADDRESS, C.CARD_MODE,which,(byte)(0x01 & 0xff), password, value);
        if(write == 0){
            LogUtils.e("写入卡"+CommonUtil.ByteToStr(password[0], value)+"成功");
            return true;
        }else{
            LogUtils.e("写卡错误，错误码："+write+"状态码："+CommonUtil.ByteToStr(1, value));
            return false;
        }
    }
}
