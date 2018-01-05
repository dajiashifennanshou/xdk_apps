package com.develop.xdk.df.phonemachine.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Administrator on 2017/9/15.
 */
public class CommonUtil {
    //ByteToString
    public static  String  ByteToStr(int byteSize, byte[] in) {
        String ret = new String("");
        if(in.length < byteSize)
            return ret;

        for(int i = 0; i < byteSize; i++){
            ret = ret.concat(String.format("%1$02X ", in[i]));
        }
        return ret;
    }
    //StringToByte
    public  static int  strToByte(String in,int byteSize,byte[] out){
        String str=in.replace(" ", "");
        if(str.length()!=byteSize*2 || out==null)
        {
            return -1;
        }
        char[] hexChars = str.toCharArray();
        if(hexChars==null)
        {
            return -1;
        }
        for (int i = 0; i < byteSize; i++) {
            int pos = i * 2;
            out[i] = (byte) ((charToByte(hexChars[pos]) << 4 )| (charToByte(hexChars[pos + 1])));
        }
        return 0;
    }
    private static int charToByte(char c) {
        return  "0123456789ABCDEF".indexOf(c);
    }
    //读取配置文件
    public static Properties loadConfig(Context context, String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return properties;
    }
    //保存配置文件
    public boolean saveConfig(Context context, String file, Properties properties) {
        try {
            File fil=new File(file);
            if(!fil.exists())
                fil.createNewFile();
            FileOutputStream s = new FileOutputStream(fil);
            properties.store(s, "");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    // 创建单个文件
    public static boolean createFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {// 判断文件是否存在
            LogUtils.e("目标文件已存在" + filePath);
            return false;
        }
        if (filePath.endsWith(File.separator)) {// 判断文件是否为目录
            LogUtils.e("目标文件不能为目录" );
            return false;
        }
        if (!file.getParentFile().exists()) {// 判断目标文件所在的目录是否存在
            // 如果目标文件所在的文件夹不存在，则创建父文件夹
            LogUtils.e("目标文件所在目录不存在，准备创建它！" );
            if (!file.getParentFile().mkdirs()) {// 判断创建目录是否成功
                LogUtils.e("创建目标文件所在的目录失败" );
                return false;
            }
        }
        try {
            if (file.createNewFile()) {// 创建目标文件
                LogUtils.e("创建文件成功:" + filePath );
                return true;
            } else {
                LogUtils.e("创建文件失败"  );
                return false;
            }
        } catch (IOException e) {// 捕获异常
            e.printStackTrace();
            System.out.println("创建文件失败！" + e.getMessage());
            LogUtils.e("创建文件失败" + e.getMessage() );
            return false;
        }
    }
    public static boolean compareCurrentDate(String time){
        if(time ==  null||time.equals("")){
            return false;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
        try {
            Date dt1 = df.parse(time);
            Date dt2 = df.parse((df.format(new Date())));
            if (dt1.getTime() > dt2.getTime()) {
                return true;
            } else if (dt1.getTime() < dt2.getTime()) {
                return false;
            } else {
                return true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }
}
