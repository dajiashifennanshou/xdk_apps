package com.develop.df.transfermachine.http.askHttp;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import com.develop.df.transfermachine.application.TMApplication;
import com.develop.df.transfermachine.data.Person;
import com.develop.df.transfermachine.http.DialogPolicy;
import com.develop.df.transfermachine.utils.C;
import com.develop.df.transfermachine.utils.CommonUtil;
import com.develop.df.transfermachine.utils.ReadAndWriteCardUtil;
import com.develop.df.transfermachine.utils.ToastUtils;
import com.google.gson.Gson;

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
                            callback.onError(C.SQL_WRONG);
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

    public  void writeTransferRecode(final Activity activity, final SqlHttpCallback callback, final Handler handler,String snNumber,Person person,String content){
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
                    String sql = "update  Person_Dossier set pd_cashmoney = pd_cashmoney - "+properties.getProperty(C.MONEY,"0")+" where pd_cardid = '"+newNumber+"' and pd_cashmoney  = "+person.getMoney();
                    statement = connection.createStatement();
                    int a  = statement.executeUpdate(sql);
                    if(a == 1){
                        String sql1 = "insert into Spend_Detail(clientid,sd_pdid,sd_pdname,sd_pdaccountid,sd_department,sd_datatype,sd_meal,sd_money,sd_oldmoney,sd_newmoney,sd_moneyaccount,sd_spendtype,sd_spenddate,sd_spendtime,sd_cldate,sd_cltime,sd_operator,sd_windowno,sd_computer) " +
                                "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,? )";
                        preparedStatement = connection.prepareStatement(sql1);
                        preparedStatement.setString(1,person.getSchoolcode());
                        preparedStatement.setString(2,person.getPid());
                        preparedStatement.setString(3,person.getName());
                        preparedStatement.setString(4,person.getAccountid());
                        preparedStatement.setString(5,person.getDepartment());
                        preparedStatement.setString(6,"在线");
                        Date date = new Date();
                        if(date.getHours() > 12){
                            preparedStatement.setString(7,"晚");
                        }else{
                            preparedStatement.setString(7,"早");
                        }
                        preparedStatement.setDouble(8,Double.valueOf(properties.getProperty(C.MONEY,"0")));
                        preparedStatement.setDouble(9,person.getMoney());
                        preparedStatement.setDouble(10,person.getMoney() - Double.valueOf(properties.getProperty(C.MONEY,"0")));
                        preparedStatement.setString(11,"转存");
                        preparedStatement.setString(12,"消费");
                        String[] time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date).split(" ");
                        preparedStatement.setString(13,time[0]);
                        preparedStatement.setString(14,time[1]);
                        preparedStatement.setString(15,time[0]);
                        preparedStatement.setString(16,time[1]);
                        preparedStatement.setString(17,"");
                        preparedStatement.setString(18, (String) properties.get(C.MACHINE_TYPE));
                        preparedStatement.setString(19,(String) properties.get(C.MACHINE_NAME));
                        int reslut = preparedStatement.executeUpdate();
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
                                    callback.onError("转存失败1，请重试！");
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
                                callback.onError("转存失败2，请重试！");
                            }
                        });
                    }
                } catch (SQLException e) {
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
                            callback.onError("转存失败3，请重试！");
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
            statement = connection.prepareStatement("select pd_department,pd_id,pd_cashmoney,pd_name,pd_loss,clientid,pd_accountid from Person_Dossier where pd_cardid = ?");
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
                    person.setMoney(rs.getDouble("pd_cashmoney"));
                    person.setDepartment(rs.getString("pd_department"));
                    person.setPid(rs.getString("pd_id"));
                    if(person.getMoney() >= Double.valueOf(properties.getProperty(C.MONEY,"0"))){
                        callback.onRespose(new Gson().toJson(person));
                    }else{
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(C.NO_MONEY);
                            }
                        });
                    }
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
                    callback.onError("转存失败，请重试！");
                }
            });
            e.printStackTrace();
        }finally {
            statement = null;
            rs = null;
        }
    }
}
