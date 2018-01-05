package com.develop.xdk.df.phonemachine.utils;

import android.app.Activity;
import android.os.Handler;
import android.reader.ReaderAndroid;
import android.text.TextUtils;


import com.develop.xdk.df.phonemachine.application.TMApplication;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.RunnableFuture;

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
        if(snnumber != null&&!snnumber.equals("")){
            snnumber = snnumber.replace(" ","");
        }
        if(0 == ret) {
            return snnumber.toUpperCase();
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
    
    public static String getCardPass(Handler handler, final Activity activity, String snNumber){
        if(TextUtils.isEmpty(snNumber)||snNumber.length() != 8){
            return null;
        }
        Properties properties  = CommonUtil.loadConfig(activity,C.FILE_PATH);
        String cityplace = properties.getProperty(C.CITY_PLACE);
        String companycode = properties.getProperty(C.COMPANY_CODE);
        if(TextUtils.isEmpty(cityplace)||TextUtils.isEmpty(companycode)||cityplace.length()!=4||companycode.length()!=4){
            handler.post(new Runnable() {
                @Override
                public void run() {
                   ToastUtils.showShort(activity,"配置有误！");
                }
            });
            return null;
        }
        int sn1 = Integer.valueOf(snNumber.substring(0,2),16);
        int sn2 = Integer.valueOf(snNumber.substring(2,4),16);
        int sn3 = Integer.valueOf(snNumber.substring(4,6),16);
        int sn4 = Integer.valueOf(snNumber.substring(6,8),16);
        char[] CityCode = cityplace.toCharArray();
        char[] CompanyCode = companycode.toCharArray();
        int[] UserCodeRoot = {0x58 , 0xF3 , 0xE4 , 0x7A , 0x19 , 0x26 };
        int[] SN = new int[6];
        int[] CardSN = {sn1,sn2,sn3,sn4};
        int[] CardCode = new int[6];
        CardSN[0]=(CardSN[0]+CityCode[0]-0x30+CompanyCode[0]-0x30);
        CardSN[1]=(CardSN[1]+CityCode[1]-0x30+CompanyCode[1]-0x30);
        CardSN[2]=(CardSN[2]+CityCode[2]-0x30+CompanyCode[2]-0x30);
        CardSN[3]=(CardSN[3]+CityCode[3]-0x30+CompanyCode[3]-0x30);
        int Temp = 0;
        for(int i = 0;i < 4;i++){
            Temp += CardSN[i];
            Temp = Temp&0xff;
        }
        SN[0] = CardSN[1]&0xff;
        SN[1] = CardSN[3]&0xff;
        SN[2] = (Temp - 0x49)&0xff;
        SN[3] = CardSN[2]&0xff;
        SN[4] = CardSN[0]&0xff;
        SN[5] = (Temp + 0x94)&0xff;
        String pass = "";
        for(int i = 0;i<6;i++){
            CardCode[i] = (((~( SN[i]<<4 | SN[5-i]>>4 )) + UserCodeRoot[i]));
            String a = "00"+Integer.toHexString(CardCode[i]&0xff);
            pass += a.substring(a.length()-2);
        }
        return pass.toUpperCase();
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
