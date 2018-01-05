package com.develop.xdk.df.attendancemachine.utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by Administrator on 2017/11/29.
 */

public class test {
    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static void main(String[] args){
        String a = "测试考勤机201712011312520";
        System.out.println(stringToAscii(a));
    }
    public static String stringToAscii(String value)
    {
        StringBuffer sbu = new StringBuffer();
        byte[]  chars = new byte[0];
        try {
            chars = value.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (int i = chars.length-1; i > -1; i--) {
                sbu.append(Integer.toHexString(Integer.valueOf((int)chars[i])&0xff));
        }
        return sbu.toString().toUpperCase();
    }
}
