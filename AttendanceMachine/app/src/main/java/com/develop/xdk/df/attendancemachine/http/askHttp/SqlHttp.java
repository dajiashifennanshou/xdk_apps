package com.develop.xdk.df.attendancemachine.http.askHttp;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;


import com.develop.xdk.df.attendancemachine.data.Person;
import com.develop.xdk.df.attendancemachine.utils.C;
import com.develop.xdk.df.attendancemachine.utils.CommonUtil;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Administrator on 2017/9/4.
 */
public class SqlHttp {
    public  Connection connection;
    public static boolean isNetworkAvailable(Activity activity) {
        Context context = activity.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        } else {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0) {
                for (int i = 0; i < networkInfo.length; i++) {
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public  Connection getConnectSQl(final SqlHttpCallback callback, Handler handler,Activity activity) {
        if (!isNetworkAvailable(activity)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(C.STRING_NO_NET);
                }
            });
            return null;
        }
        Properties properties  = CommonUtil.loadConfig(activity,C.FILE_PATH);
        try {
            if(connection == null||connection.isClosed()){
                try { // 加载驱动程序
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    connection = DriverManager.getConnection(
                            "jdbc:jtds:sqlserver://"+properties.getProperty(C.SQL_PATH,"null"), properties.getProperty(C.SQL_USER_NAME,"null"),
                            properties.getProperty(C.SQL_PASS_WORD,"null"));
                    return connection;
                } catch (final ClassNotFoundException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                } catch (final SQLException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(C.SQL_WRONG);
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(C.SQL_WRONG);
                        }
                    });
                }
            }else{
                return connection;
            }
        } catch (final SQLException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e.getMessage());
                }
            });
        }
        return null;
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
    public void getImgPath(final Activity activity, final SqlHttpCallback callback, final Handler handler,String date){
        Connection connection = getConnectSQl(callback,handler,activity);
        if(connection == null){
            return ;
        }
        Properties properties  = CommonUtil.loadConfig(activity,C.FILE_PATH);
        PreparedStatement preparedStatement = null;
        String imgpath = null;
        ResultSet rs = null;
        try {
            String sql = "select clientid,om_name,om_checktype,om_style from Ocom_Machine where om_mac = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,properties.getProperty(C.MACHINE_NAME,null));
            rs  = preparedStatement.executeQuery();
            if(rs.next()) {
                imgpath = rs.getString("om_name")+date+rs.getString("om_style");
                imgpath = rs.getString("clientid")+" "+stringToAscii(imgpath);
                callback.onRespose(imgpath);
            }
        } catch (SQLException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                        callback.onError("数据库连接失败");
                }
            });
            e.printStackTrace();
        }finally {
            preparedStatement = null;
            rs = null;
        }
    }

    public  void writeTransferRecode(final Activity activity, final SqlHttpCallback callback, final Handler handler, String snNumber, Person person, String imgpath,Date date){
                Connection connection = getConnectSQl(callback,handler,activity);
                if(connection == null){
                    return;
                }
                String newNumber =  snNumber;
                Statement statement = null;
                Properties properties  = CommonUtil.loadConfig(activity,C.FILE_PATH);
                PreparedStatement preparedStatement = null;
                ResultSet rs = null;
                try {
                    connection.setAutoCommit(false);
                    String sql = "select om_name,om_checktype,om_style from Ocom_Machine where om_mac = ?";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1,properties.getProperty(C.MACHINE_NAME,null));
                    rs  = preparedStatement.executeQuery();
                    if(rs.next()){
                        String sql1 = "insert into Check_Record(clientid,cr_pdid,cr_pdname,cr_pdaccountid,cr_department,cr_date,cr_time,cr_mactype,cr_checkmac,cr_checktype,cr_style) " +
                                "values(?,?,?,?,?,?,?,?,?,?,?)";
                        preparedStatement = connection.prepareStatement(sql1);
                        preparedStatement.setString(1,person.getSchoolcode());
                        preparedStatement.setString(2,person.getPid());
                        preparedStatement.setString(3,person.getName());
                        preparedStatement.setString(4,person.getAccountid());
                        preparedStatement.setString(5,person.getDepartment());
                        String[] time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date).split(" ");
                        preparedStatement.setString(6,time[0]);
                        preparedStatement.setString(7,time[1]);
                        preparedStatement.setInt(8,0);
                        preparedStatement.setString(9,rs.getString("om_name"));
                        preparedStatement.setInt(10,rs.getInt("om_checktype"));
                        preparedStatement.setInt(11,rs.getInt("om_style"));
                        int reslut = preparedStatement.executeUpdate();
//                        String imgpath = rs.getString("om_name")+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+rs.getString("om_style");
//                        imgpath = stringToAscii(imgpath);
                        preparedStatement = connection.prepareStatement("insert into Check_Record_Photo(clientid,crp_picid,crp_date) values(?,?,?)");
                        preparedStatement.setString(1,person.getSchoolcode());
                        preparedStatement.setString(2,imgpath.split(" ")[1]);
                        preparedStatement.setString(3,new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                        reslut = preparedStatement.executeUpdate();
                        if(reslut == 1){
                                connection.commit();
                                callback.onRespose(C.SQL_OK);
                        }else{
                            if(connection != null){
                                try {
                                    connection.rollback();
                                } catch (SQLException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError("考勤异常！");
                                }
                            });
                        }
                    }else{
                        if(connection != null){
                            try {
                                connection.rollback();
                            } catch (SQLException e1) {
                                e1.printStackTrace();
                            }
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("配置文件出错！");
                            }
                        });
                    }
                } catch (final SQLException e) {
                    if(connection != null){
                        try {
                            connection.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                    e.printStackTrace();
                }finally {
                    statement = null;
                    preparedStatement = null;
                }
    }
    public  void checkSnNumber(Activity activity, final String snNumber, final Handler handler, final SqlHttpCallback callback){
        Connection connection = getConnectSQl(callback,handler,activity);
        if(connection == null){
            return;
        }
        if(snNumber == null ||snNumber.length() != 8){
            callback.onError("操作失败，请重试！4");
            return;
        }
        Properties properties  = CommonUtil.loadConfig(activity,C.FILE_PATH);
        String newNumber =  snNumber;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement("select pd_class,pd_grade,pd_department,pd_id,pd_name,pd_loss,clientid,pd_accountid from Person_Dossier where pd_cardid = ? and changestate != 99 ");
            statement.setString(1,newNumber);
            rs = statement.executeQuery();
            if(rs.next()){
                if(rs.getInt("pd_loss") == 1){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(C.CARD_LOSS);
                        }
                    });
                }else{
                    Person person = new Person();
                    person.setAccountid(rs.getString("pd_accountid"));
                    person.setSchoolcode(rs.getString("clientid"));
                    person.setName(rs.getString("pd_name"));
                    person.setDepartment(rs.getString("pd_department"));
                    person.setPid(rs.getString("pd_id"));
                    person.setSclass(rs.getInt("pd_grade")+"级"+rs.getInt("pd_class")+"班");
                    statement = connection.prepareStatement("select clientname from ClientDossier where clientid = ?");
                    statement.setString(1,rs.getString("clientid"));
                    rs = statement.executeQuery();
                    if(rs.next()){
                        person.setSchool(rs.getString("clientname"));
                    }
                    callback.onRespose(new Gson().toJson(person));
                }
            }else{
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(C.CARD_NO_RECODE);
                    }
                });
            }
        } catch (final SQLException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e.getMessage());
                }
            });
            e.printStackTrace();
        }finally {
            statement = null;
            rs = null;
        }
    }

    public void getMachineName(final Activity activity, final SqlHttpCallback callback, final Handler handler) {
        Connection connection = getConnectSQl(callback,handler,activity);
        if(connection == null){
            return ;
        }
        Properties properties  = CommonUtil.loadConfig(activity,C.FILE_PATH);
        PreparedStatement preparedStatement = null;
        String name = "";
        ResultSet rs = null;
        try {
            String sql = "select om_name from Ocom_Machine where om_mac = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,properties.getProperty(C.MACHINE_NAME,null));
            rs  = preparedStatement.executeQuery();
            if(rs.next()) {
                name = rs.getString("om_name");
            }
            callback.onRespose(name);
        } catch (SQLException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError("数据库连接失败");
                }
            });
            e.printStackTrace();
        }finally {
            preparedStatement = null;
            rs = null;
        }
    }
}
