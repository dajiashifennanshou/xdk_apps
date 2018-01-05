package com.develop.xdk.df.attendancemachine.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.develop.xdk.df.attendancemachine.R;
import com.develop.xdk.df.attendancemachine.base.BaseActivity;
import com.develop.xdk.df.attendancemachine.data.Person;
import com.develop.xdk.df.attendancemachine.http.askHttp.SqlHttp;
import com.develop.xdk.df.attendancemachine.http.askHttp.SqlHttpCallback;
import com.develop.xdk.df.attendancemachine.utils.C;
import com.develop.xdk.df.attendancemachine.utils.CommonUtil;
import com.develop.xdk.df.attendancemachine.utils.FTPManager;
import com.develop.xdk.df.attendancemachine.utils.LogUtils;
import com.develop.xdk.df.attendancemachine.utils.ReadAndWriteCardUtil;
import com.develop.xdk.df.attendancemachine.utils.SingleThreadUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import butterknife.BindView;

public class MainActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PictureCallback {

    private Thread lookingForSnNumber;
    private Boolean isLookFor = true;
    private Boolean isNoPerson = false;
    Properties properties = null;
    SqlHttp sqlHttp = new SqlHttp();
    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    Ringtone rt = null;
    String snNumber = null;
    Person person = new Person();
    @BindView(R.id.main_show_state)
    TextView tx_show_state;
    @BindView(R.id.main_show_school)
    TextView tx_show_school;
    @BindView(R.id.main_show_class)
    TextView tx_show_class;
    @BindView(R.id.main_show_name)
    TextView tx_show_name;
    @BindView(R.id.main_show_time)
    TextView tx_show_time;
    @BindView(R.id.main_progress)
    ProgressBar progressBar;
    @BindView(R.id.main_time)
    TextView tx_time;
    @BindView(R.id.main_show_photo)
    ImageView img_show_photo;
    @BindView(R.id.main_surface)
    SurfaceView surfaceView;
    @BindView(R.id.main_title_tx)
    TextView tx_main_title;
    private static final int msgKey1 = 1;
    private Camera mCamera;
    private SurfaceHolder surfaceHolder;
    Bitmap bitmap;
    FTPManager ftpManager = new FTPManager();
    TimeThread timeThread;
    String up_imgpath = null;
    Date date;
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
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
    }
    @Override
    public void initView() {
        initProperties();
        lookingForSnNumber = new Thread(new Runnable() {
            @Override
            public void run() {
                sqlHttp.getMachineName(MainActivity.this, new SqlHttpCallback() {
                    @Override
                    public void onRespose(final String msg) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                tx_main_title.setText(msg);
                            }
                        });
                    }

                    @Override
                    public void onError(String msg) {
                        tx_show_state.setText(msg);
                    }
                },handler);
                while (true) {
                    if (isLookFor) {
                        snNumber = ReadAndWriteCardUtil.readSnNumber(handler, MainActivity.this);
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

    @Override
    public void onPictureTaken(final byte[] bytes, Camera camera) {
        SingleThreadUtil.getSingleThreadUtil().execute(new Runnable() {
            @Override
            public void run() {
                sqlHttp.getImgPath(MainActivity.this, new SqlHttpCallback() {
                    @Override
                    public void onRespose(String msg) {
                        try {
                            if (ftpManager.connect()) {
                                final String filename = C.IMG_PATH+msg.replaceAll(" ","")+ ".jpg";
                                FileOutputStream fos = new FileOutputStream(new File(filename));
                                //旋转角度，保证保存的图片方向是对的
                                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                Matrix matrix = new Matrix();
                                matrix.setRotate(0);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, fos);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        img_show_photo.setImageBitmap(bitmap);
                                        img_show_photo.setVisibility(View.VISIBLE);
                                    }
                                });
                                fos.flush();
                                fos.close();
                                sqlHttp.writeTransferRecode(MainActivity.this, new SqlHttpCallback() {
                                    @Override
                                    public void onRespose(String msg) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
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
                                        progressBar.setVisibility(View.GONE);
                                        isNoPerson = true;
                                    }
                                }, handler, snNumber, person,msg,date);
                                if (ftpManager.uploadFile(filename, "C:/FTPYktFile/Image/Check/")) {
                                }else{
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            toastShort("拍照上传失败");
                                        }
                                    });
                                }
                            }else{
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        toastShort("ftp连接失败");
                                    }
                                });
                            }
                        } catch (final FileNotFoundException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    toastShort(e.getMessage());
                                }
                            });
                            e.printStackTrace();
                        } catch (final IOException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    toastShort(e.getMessage());
                                }
                            });
                            e.printStackTrace();
                        }catch (final Exception e){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    toastShort(e.getMessage());
                                }
                            });
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(final String emsg) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                toastShort(emsg);
                            }
                        });
                    }
                }, handler,new SimpleDateFormat("yyyyMMddHHmmss").format(date));
            }
        });
        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void startPreview() {
        if (mCamera != null) {
            return;
        }
        mCamera = Camera.open(1);
        Camera.Parameters parameters = mCamera.getParameters();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        Camera.Size size = getBestPreviewSize(width, height, parameters);
        if (size != null) {
            //设置预览分辨率
            parameters.setPreviewSize(size.width, size.height);
            //设置保存图片的大小
            parameters.setPictureSize(size.width, size.height);
        }

        //自动对焦
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPreviewFrameRate(20);

        //设置相机预览方向
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }

        mCamera.startPreview();
    }
    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return result;
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
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        rt = RingtoneManager.getRingtone(MainActivity.this, uri);
        boolean is_first = CommonUtil.createFile(C.FILE_PATH);
        CommonUtil.createFile(C.IMG_PATH+"init.txt");
        if (is_first) {
            Properties properties = CommonUtil.loadConfig(this, C.FILE_PATH);
            if (properties != null) {
                try {
                    properties.put(C.SQL_PATH, "106.14.68.153:2055/czn_alykt");
                    properties.put(C.SQL_USER_NAME, "sa");
                    properties.put(C.SQL_PASS_WORD, "cinzn2055");
                    properties.put(C.FTP_IP, "106.14.68.153");
                    properties.put(C.FTP_PORT, "21");
                    properties.put(C.FTP_USER_NAME, "cznyktfile");
                    properties.put(C.FTP_PASS_WORD, "Cinzn2017");
                    properties.put(C.MACHINE_NAME, "11");
                    properties.store(new FileOutputStream(C.FILE_PATH), "firstinit");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        properties = CommonUtil.loadConfig(this, C.FILE_PATH);
    }


    /**
     * 查询到无sn号码
     */
    private void checkNoSnNumber() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //恢复第一状态
//                img_show_photo.setVisibility(View.GONE);
//                tx_show_name.setText("");
//                tx_show_school.setText("");
//                tx_show_class.setText("");
//                tx_show_time.setText("");
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
     * 开始考勤
     */
    private void readAndWriteCard() {
        date = new Date();
        handler.post(new Runnable() {
            @Override
            public void run() {
                tx_show_name.setText(String.format(getResources().getString(R.string.str_trans_name),person.getName()));
                tx_show_class.setText(String.format(getResources().getString(R.string.str_trans_class),person.getSclass()));
                tx_show_school.setText(String.format(getResources().getString(R.string.str_trans_school),person.getSchool()));
                tx_show_time.setText(String.format(getResources().getString(R.string.str_trans_time),new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒").format(date)));
                progressBar.setVisibility(View.VISIBLE);
                tx_show_state.setText(getResources().getText(R.string.str_trans_state4));
                mCamera.takePicture(null, null, null, MainActivity.this);
            }
        });
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
        rt.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rt = null;
        //停止循环操作
        if (isNoPerson) {
            isNoPerson = false;
        }
        if (isLookFor) {
            isLookFor = false;
        }
        lookingForSnNumber.interrupt();
        timeThread.interrupt();
        lookingForSnNumber = null;
        timeThread = null;
    }
    /**
     *  播放系统拍照声音
     */
    public void playSound() {
        MediaPlayer mediaPlayer = null;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume( AudioManager.STREAM_NOTIFICATION);
        if (volume != 0) {
            if (mediaPlayer == null)
                mediaPlayer = MediaPlayer.create(this,
                        Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
        }
    }
}
