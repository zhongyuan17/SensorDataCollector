package com.chengcheng.sensordata;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    private SensorManager sensorManager;

    private EditText nameEdit;
    private Button btnName;

    private RadioGroup actionGroup1;
    private RadioGroup actionGroup2;
    private RadioButton rbWalk;
    private RadioButton rbStand;
    private RadioButton rbSit;
    private RadioButton rbStairsdown;
    private RadioButton rbStairsup;

    private Button startBtn;

    private TextView nameText;
    private TextView actionText;
    private TextView countText;
    private TextView timeText;

    private TextView accxText;
    private TextView accyText;
    private TextView acczText;
    private TextView gyroxText;
    private TextView gyroyText;
    private TextView gyrozText;

    long sampleStart;
    long sampleEnd;
    long count;

    String strName = "";
    String strAction = "";
    boolean bRunning = false;
    String strCurAccFileName = "";
    String strCurGyroFileName = "";
    long accIndex = 0;
    long gyroIndex = 0;

    FileOutputStream accFileStream;
    FileOutputStream gyroFileStream;

    private PowerManager.WakeLock mWakeLock;

    private static final String TAG = "CreateFile";
    private static final String DATASET_DIR = "SensorData";

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!bRunning || !intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }
            if (sensorManager != null) {//取消监听后重写监听，以保持后台运行
                sensorManager.unregisterListener(listenera);
                sensorManager.unregisterListener(listenerg);
                Sensor sensora = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor sensorg = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                sensorManager.registerListener(listenera, sensora, 25000);    // 25ms
                sensorManager.registerListener(listenerg, sensorg, 25000);    // 25ms
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameEdit = (EditText) findViewById(R.id.input_edit);
        btnName = (Button) findViewById(R.id.btn_name);
        btnName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                strName = nameEdit.getText().toString();
                nameText.setText(strName);
            }
        });
        actionGroup1 = (RadioGroup) findViewById(R.id.group1);
        actionGroup2 = (RadioGroup) findViewById(R.id.group2);
        rbWalk = (RadioButton) findViewById(R.id.rb_walk);
        rbStand = (RadioButton) findViewById(R.id.rb_stand);
        rbSit = (RadioButton) findViewById(R.id.rb_sit);
        rbStairsup = (RadioButton) findViewById(R.id.rb_stairsup);
        rbStairsdown = (RadioButton) findViewById(R.id.rb_stairsdown);
        rbWalk.setOnCheckedChangeListener(this);
        rbStand.setOnCheckedChangeListener(this);
        rbSit.setOnCheckedChangeListener(this);
        rbStairsup.setOnCheckedChangeListener(this);
        rbStairsdown.setOnCheckedChangeListener(this);

        startBtn = (Button) findViewById(R.id.btn_start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bRunning) {
                    startBtn.setText("Start");
                    endSensorListening();
                }
                else {
                    startBtn.setText("End");
                    startSensorListening();
                }
            }
        });

        nameText = (TextView) findViewById(R.id.text_name);
        actionText = (TextView) findViewById(R.id.text_action);
        countText = (TextView) findViewById(R.id.text_count);
        timeText = (TextView) findViewById(R.id.text_time);

        accxText = (TextView) findViewById(R.id.accx);
        accyText = (TextView) findViewById(R.id.accy);
        acczText = (TextView) findViewById(R.id.accz);

        gyroxText = (TextView) findViewById(R.id.gyrox);
        gyroyText = (TextView) findViewById(R.id.gyroy);
        gyrozText = (TextView) findViewById(R.id.gyroz);

        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);// CPU保存运行
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);// 屏幕熄掉后依然运行
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
    }

    private SensorEventListener listenera = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float accx = event.values[0];
            float accy = event.values[1];
            float accz = event.values[2];

            sampleEnd = System.currentTimeMillis();

            if (accFileStream != null) {
                try {
                    String strContent = "" + accIndex + "," + sampleEnd + "," + accx + "," + accy + "," + accz + "," + strName + "," + strAction + "\n";
                    accFileStream.write(strContent.getBytes());
                } catch (IOException e) {
                    Log.d(TAG, "startSensorListening: Write File Exception: " + e.toString());
                }
            }

            count++;

            accxText.setText("" + accx);
            accyText.setText("" + accy);
            acczText.setText("" + accz);
            countText.setText("" + count);
            timeText.setText("" + (sampleEnd - sampleStart) + "ms");

            accIndex++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private SensorEventListener listenerg = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float gyrox = event.values[0];
            float gyroy = event.values[1];
            float gyroz = event.values[2];

            sampleEnd = System.currentTimeMillis();

            if (gyroFileStream != null) {
                try {
                    String strContent = "" + gyroIndex + "," + sampleEnd + "," + gyrox + "," + gyroy + "," + gyroz + "," + strName + "," + strAction + "\n";
                    gyroFileStream.write(strContent.getBytes());
                } catch (IOException e) {
                    Log.d(TAG, "startSensorListening: Write File Exception: " + e.toString());
                }
            }

            gyroxText.setText("" + gyrox);
            gyroyText.setText("" + gyroy);
            gyrozText.setText("" + gyroz);

            gyroIndex++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        endSensorListening();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) {
            return;
        }
        switch (buttonView.getId()) {
            case R.id.rb_walk:
                strAction = "walk";
                actionGroup2.clearCheck();
                break;
            case R.id.rb_stand:
                strAction = "stand";
                actionGroup2.clearCheck();
                break;
            case R.id.rb_sit:
                strAction = "sit";
                actionGroup2.clearCheck();
                break;
            case R.id.rb_stairsdown:
                strAction = "stairsdown";
                actionGroup1.clearCheck();
                break;
            case R.id.rb_stairsup:
                strAction = "stairsup";
                actionGroup1.clearCheck();
                break;
        }
        actionText.setText(strAction);
    }

    private void startSensorListening() {
        btnName.setEnabled(false);
        actionGroup1.setEnabled(false);
        actionGroup2.setEnabled(false);
        count = 0;

        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return;
        }

        String filepath = "";
        File dir = new File(Environment.getExternalStorageDirectory(),
                DATASET_DIR);
        if (!dir.exists()) {
            try {
                dir.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        filepath = dir.getAbsolutePath();
        Log.d(TAG, filepath);

        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }

        bRunning = true;
        sampleStart = System.currentTimeMillis();
        mWakeLock.acquire();// 屏幕熄后，CPU继续运行
        Sensor sensora = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor sensorg = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(listenera, sensora, 25000);    // 25ms
        sensorManager.registerListener(listenerg, sensorg, 25000);    // 25ms

        strCurAccFileName = "acc" + strName + "_" + strAction + "_" + sampleStart + ".csv";
        strCurGyroFileName = "gyro" + strName + "_" + strAction + "_" + sampleStart + ".csv";
        try {
            File file = new File(filepath, strCurAccFileName);
            accFileStream = new FileOutputStream(file);
            file = new File(filepath, strCurGyroFileName);
            gyroFileStream = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
            accFileStream = null;
            gyroFileStream = null;
            return;
        }

        String strTitle = "index,timestamp,x,y,z,user,action\n";
        try {
            if (accFileStream != null) {
                accFileStream.write(strTitle.getBytes());
            }
            if (gyroFileStream != null) {
                gyroFileStream.write(strTitle.getBytes());
            }
        } catch (IOException e) {
            Log.d(TAG, "startSensorListening: Write File Exception: " + e.toString());
            accFileStream = null;
            gyroFileStream = null;
        }
    }

    private void endSensorListening() {
        bRunning = false;
        accIndex = 0;
        gyroIndex = 0;
        btnName.setEnabled(true);
        actionGroup1.setEnabled(true);
        actionGroup2.setEnabled(true);

        if (sensorManager != null) {
            sensorManager.unregisterListener(listenera);
            sensorManager.unregisterListener(listenerg);
        }

        mWakeLock.release();

        try {
            if (accFileStream != null) {
                accFileStream.flush();
                accFileStream.close();
            }
            if (gyroFileStream != null) {
                gyroFileStream.flush();
                gyroFileStream.close();
            }
        } catch (IOException e) {
            Log.d(TAG, "startSensorListening: Write File Exception: " + e.toString());
        }

        accxText.setText("");
        accyText.setText("");
        acczText.setText("");
        gyroxText.setText("");
        gyroyText.setText("");
        gyrozText.setText("");

    }

    private void preprocessData() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(strCurAccFileName));
            br.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(br!=null){
                try {
                    br.close();
                    br = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}