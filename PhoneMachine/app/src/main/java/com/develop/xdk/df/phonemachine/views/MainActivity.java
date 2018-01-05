package com.develop.xdk.df.phonemachine.views;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.develop.xdk.df.phonemachine.R;
import com.develop.xdk.df.phonemachine.base.BaseActivity;
import com.develop.xdk.df.phonemachine.data.Person;
import com.develop.xdk.df.phonemachine.http.DialogPolicy;
import com.develop.xdk.df.phonemachine.http.askHttp.SqlHttp;
import com.develop.xdk.df.phonemachine.http.askHttp.SqlHttpCallback;
import com.develop.xdk.df.phonemachine.utils.C;
import com.develop.xdk.df.phonemachine.utils.CommonUtil;
import com.develop.xdk.df.phonemachine.utils.ReadAndWriteCardUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    private Thread lookingForSnNumber;
    private Thread lookingForSnNumberHelp;
    private Boolean isLookFor = true;
    private Boolean isNoPerson = false;
    DialogPolicy waitPolicy = new DialogPolicy(MainActivity.this);
    Properties properties = null;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    SqlHttp sqlHttp = new SqlHttp();
    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    Ringtone rt = null;
    String snNumber = null;
    Person person = new Person();
    @BindView(R.id.main_show_state)
    TextView tx_show_state;
    @BindView(R.id.main_show_money)
    TextView tx_show_money;
    @BindView(R.id.main_show_wash_money)
    TextView tx_show_wash_money;
    @BindView(R.id.main_show_name)
    TextView tx_show_name;
    @BindView(R.id.main_progress)
    ProgressBar progressBar;
    @BindView(R.id.main_time)
    TextView tx_time;
    @BindView(R.id.main_text_money)
    TextView tx_money;
    private static final int msgKey1 = 1;
    boolean iscanplay = false;
    Thread timeThread;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgKey1:
                    long time = System.currentTimeMillis();
                    Date date = new Date(time);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
                    tx_time.setText(format.format(date));
                    break;
                default:
                    break;
            }
        }
    };

    @OnClick(R.id.main_clean)
    public void clik() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //停止循环操作
                if (isNoPerson) {
                    isNoPerson = false;
                }
                if (isLookFor) {
                    isLookFor = false;
                }
                String block = properties.getProperty(C.CARD_ADRESS, "null");
                byte mblock = (byte) (Integer.valueOf(block) & 0xff);
                byte mblock1 = (byte) ((Integer.valueOf(block) + 1) & 0xff);
                final boolean issuccess = ReadAndWriteCardUtil.writeCardBlock("00000000000000000000000000000000", handler, MainActivity.this, mblock, snNumber);
                final boolean issuccess1 = ReadAndWriteCardUtil.writeCardBlock("00000000000000000000000000000000", handler, MainActivity.this, mblock1, snNumber);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (issuccess && issuccess1) {
                            toastShort("ok");
                        } else {
                            toastShort("no");
                        }
                        isLookFor = true;
                    }
                });
            }
        }).start();
    }

    @Override
    public void initView() {
        setContentView(R.layout.view_main);
        initProperties();
        lookingForSnNumber = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (isLookFor) {
                        snNumber = ReadAndWriteCardUtil.readSnNumber(handler, MainActivity.this);
//                        snNumber = "9C714D1C";
                        if (snNumber != null) {
                            hasSnNumber();
                        }
                    }
                    if (isNoPerson) {
                        snNumber = ReadAndWriteCardUtil.readSnNumber(handler, MainActivity.this);
                        if (snNumber == null) {
                            checkNoSnNumber();
                        }
                    }
                }
            }
        });
        lookingForSnNumber.start();
        timeThread = new TimeThread();
        timeThread.start();
    }

    public class TimeThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = msgKey1;
                    handler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    /**
     * 初始化配置文件
     */
    private void initProperties() {
        rt = RingtoneManager.getRingtone(MainActivity.this, uri);
        File file = new File(C.MP3_FILE_PATH);
        if (file.exists()) {
            try {
                mediaPlayer.setDataSource(file.getPath()); // 指定音频文件的路径
                mediaPlayer.prepare();
                iscanplay = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        boolean is_first = CommonUtil.createFile(C.FILE_PATH);
        if (is_first) {
            Properties properties = CommonUtil.loadConfig(this, C.FILE_PATH);
            if (properties != null) {
                try {
                    properties.put(C.SQL_PATH, "");
                    properties.put(C.SQL_USER_NAME, "");
                    properties.put(C.SQL_PASS_WORD, "");
                    properties.put(C.MACHINE_NAME, "");
                    properties.put(C.MACHINE_TYPE, "");
                    properties.put(C.COMPANY_CODE, "");
                    properties.put(C.CITY_PLACE, "");
                    properties.put(C.CARD_ADRESS, "");
                    properties.store(new FileOutputStream(C.FILE_PATH), "firstinit");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        properties = CommonUtil.loadConfig(this, C.FILE_PATH);
    }


    /**
     * 守护线程查询到无sn号码
     */
    private void checkNoSnNumber() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //恢复第一状态
                tx_show_name.setText("");
                tx_show_wash_money.setText("");
                tx_show_money.setText("");
                tx_show_state.setText(getResources().getText(R.string.str_trans_state1));
            }
        });
        if (!isLookFor) {
            isLookFor = true;
        }
        if (isNoPerson) {
            isNoPerson = false;
        }
    }

    /**
     * 查询到sn号码
     */
    private void hasSnNumber() {
        //停止循环操作
        if (isNoPerson) {
            isNoPerson = false;
        }
        if (isLookFor) {
            isLookFor = false;
        }
        checkSnNumber();
    }

    /**
     * 检测卡号是否存在系统，是否挂失
     *
     * @return
     */
    private void checkSnNumber() {
        sqlHttp.checkSnNumber(this, snNumber, handler, new SqlHttpCallback() {
            @Override
            public void onRespose(String msg) {
                person = new Gson().fromJson(msg, Person.class);
                readAndWriteCard();
            }

            @Override
            public void onError(String msg) {
                tx_show_state.setText(msg);
                isNoPerson = true;
            }
        });

    }

    /**
     * 读写卡操作
     */
    private void readAndWriteCard() {
        //展示信息
        handler.post(new Runnable() {
            @Override
            public void run() {
                tx_show_state.setText(getResources().getText(R.string.str_trans_state2));
                progressBar.setVisibility(View.VISIBLE);
                tx_show_name.setText(String.format(getResources().getString(R.string.str_trans_name), person.getName()));
                tx_show_wash_money.setText(String.format(getResources().getString(R.string.str_trans_money), person.getMoney() + ""));
            }
        });
        String money = "0000"+Integer.toHexString((int) person.getMoney()*100);
        money = money.substring(money.length()-4);
        String writeContent = "";
        String content = money+"3C3C3C3C3C3C3C3C3C3C0000";
        int reslut = 0xAA;
        for(int i = 0;i<content.length();i=i+2){
            reslut = reslut^Integer.valueOf(content.substring(i,i+2),16)&0xFF;
        }
        String last1 = "00"+Integer.toHexString(reslut);
        last1 = last1.substring(last1.length()-2);
        int a = ~(reslut + 39)&0xFF;
        String last = "00"+Integer.toHexString(a);
        last = last.substring(last.length()-2);
        writeContent = content+last1+last;
        String block = properties.getProperty(C.CARD_ADRESS, "null");
        if (block.equals("null")) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tx_show_state.setText("请配置读写块参数");
                    //                    waitPolicy.disappear();
                    progressBar.setVisibility(View.GONE);
                    isNoPerson = true;
                }
            });
            return;
        }
        byte mblock = (byte) (Integer.valueOf(block) & 0xff);
        boolean issuccess = ReadAndWriteCardUtil.writeCardBlock(writeContent.toUpperCase(), handler, MainActivity.this, mblock, snNumber);
        if (issuccess) {
            sqlHttp.writeTransferRecode(MainActivity.this, new SqlHttpCallback() {
                @Override
                public void onRespose(String msg) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            DecimalFormat df = new DecimalFormat("########0.00");
                            tx_show_money.setText(String.format(getResources().getString(R.string.str_trans_wash_money), person.getMoney()+""));
                            tx_show_wash_money.setText(String.format(getResources().getString(R.string.str_trans_money), person.getMoney()+""));
                            tx_show_state.setText(getResources().getText(R.string.str_trans_state3));
                            progressBar.setVisibility(View.GONE);
                            playSuccess();
                        }
                    });
                    isNoPerson = true;
                }

                @Override
                public void onError(String msg) {
                    tx_show_state.setText(msg);
                    //                    waitPolicy.disappear();
                    progressBar.setVisibility(View.GONE);
                    isNoPerson = true;
                }
            }, handler, snNumber, person, writeContent.toUpperCase());
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tx_show_state.setText("转存失败，请重试！");
//                    waitPolicy.disappear();
                    progressBar.setVisibility(View.GONE);
                    isNoPerson = true;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        //停止循环操作
        if (isNoPerson) {
            isNoPerson = false;
        }
        if (isLookFor) {
            isLookFor = false;
        }
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!isLookFor) {
            isLookFor = true;
        }
        if (isNoPerson) {
            isNoPerson = false;
        }
    }

    /**
     * 播放成功提示
     */
    private void playSuccess() {
//        if(iscanplay) {
//            mediaPlayer.start();
//        }
        rt.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//        }
        rt = null;
        //停止循环操作
        if (isNoPerson) {
            isNoPerson = false;
        }
        if (isLookFor) {
            isLookFor = false;
        }
        lookingForSnNumber.interrupt();
        lookingForSnNumber = null;
    }
}
