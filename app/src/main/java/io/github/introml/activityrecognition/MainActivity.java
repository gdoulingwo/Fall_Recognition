package io.github.introml.activityrecognition;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.max;

public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {

    private static final int N_SAMPLES = 128;
    private static List<Float> x;
    private static List<Float> y;
    private static List<Float> z;
    private static List<Float> gravity_x;
    private static List<Float> gravity_y;
    private static List<Float> gravity_z;
    private static List<Float> linear_acceleration_x;
    private static List<Float> linear_acceleration_y;
    private static List<Float> linear_acceleration_z;
    private TextView downstairsTextView;

    private TextView joggingTextView;
    private TextView sittingTextView;
    private TextView standingTextView;
    private TextView upstairsTextView;
    private TextView walkingTextView;
    private TextToSpeech textToSpeech;
    private float[] results = new float[6];
    private TensorFlowClassifier classifier;
    //int index_max = 0;

    //private String[] labels = {"Downstairs", "Jogging", "Sitting", "Standing", "Upstairs", "Walking"};

    private String[] labels = {"WALKING",
            "WALKING_UPSTAIRS",
            "WALKING_DOWNSTAIRS",
            "SITTING",
            "STANDING",
            "LAYING"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        //todo
        gravity_x = new ArrayList<>();
        gravity_y = new ArrayList<>();
        gravity_z = new ArrayList<>();
        linear_acceleration_x = new ArrayList<>();
        linear_acceleration_y = new ArrayList<>();
        linear_acceleration_z = new ArrayList<>();

        downstairsTextView = (TextView) findViewById(R.id.downstairs_prob);
        joggingTextView = (TextView) findViewById(R.id.jogging_prob);
        sittingTextView = (TextView) findViewById(R.id.sitting_prob);
        standingTextView = (TextView) findViewById(R.id.standing_prob);
        upstairsTextView = (TextView) findViewById(R.id.upstairs_prob);
        walkingTextView = (TextView) findViewById(R.id.walking_prob);

        classifier = new TensorFlowClassifier(getApplicationContext());

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);

    }

    @Override
    public void onInit(int status) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (results == null || results.length == 0) {
                    return;
                }
                float max = -1;
                int idx = -1;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] > max) {
                        idx = i;
                        max = results[i];
                    }
                }

                textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, Integer.toString(new Random().nextInt()));
            }
        }, 2000, 5000);
    }

    protected void onPause() {
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        activityPrediction();
        int type = event.sensor.getType();
        switch (type){
            case Sensor.TYPE_ACCELEROMETER:
                if(x.size() < N_SAMPLES
                        && y.size() < N_SAMPLES
                        && z.size() < N_SAMPLES) {
                    x.add(event.values[0]);
                    y.add(event.values[1]);
                    z.add(event.values[2]);
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                if(gravity_x.size() < N_SAMPLES
                        && gravity_y.size() < N_SAMPLES
                        && gravity_z.size() < N_SAMPLES) {
                    gravity_x.add(event.values[0]);
                    gravity_y.add(event.values[1]);
                    gravity_z.add(event.values[2]);
                }
                break;
            case Sensor.TYPE_GRAVITY:
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                if(linear_acceleration_x.size() < N_SAMPLES
                        && linear_acceleration_y.size() < N_SAMPLES
                        && linear_acceleration_z.size() < N_SAMPLES) {
                    linear_acceleration_x.add(event.values[0]);
                    linear_acceleration_y.add(event.values[1]);
                    linear_acceleration_z.add(event.values[2]);

                }
                break;
        }

//        final float alpha = 0.8f;
//
//
//        // Isolate the force of gravity with the low-pass filter.
//        gravity_x = add(alpha * gravity[0] + (1 - alpha) * event.values[0]);
//        gravity_y = add(alpha * gravity[1] + (1 - alpha) * event.values[1]);
//        gravity_z = add(alpha * gravity[2] + (1 - alpha) * event.values[2]);
//
//        // Remove the gravity contribution with the high-pass filter.
//        linear_acceleration_x = add(event.values[0] - gravity[0]);
//        linear_acceleration_y = add(event.values[1] - gravity[1]);
//        linear_acceleration_z = add(event.values[2] - gravity[2]);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void activityPrediction() {
        if (x.size() == N_SAMPLES
                && y.size() == N_SAMPLES
                && z.size() == N_SAMPLES
                && linear_acceleration_x.size() == N_SAMPLES
                && linear_acceleration_y.size() == N_SAMPLES
                && linear_acceleration_z.size() == N_SAMPLES
                && gravity_x.size() == N_SAMPLES
                && gravity_y.size() == N_SAMPLES
                && gravity_z.size() == N_SAMPLES)
        {
            List<Float> data = new ArrayList<>();
            for(int i=0;i<N_SAMPLES;i++){
                data.add(x.get(i));
                data.add(y.get(i));
                data.add(z.get(i));
                data.add(linear_acceleration_x.get(i));
                data.add(linear_acceleration_y.get(i));
                data.add(linear_acceleration_z.get(i));
                data.add(gravity_x.get(i));
                data.add(gravity_y.get(i));
                data.add(gravity_z.get(i));




            }
//            data.addAll(linear_acceleration_x);
//            data.addAll(linear_acceleration_y);
//            data.addAll(linear_acceleration_z);
//            data.addAll(gravity_x);
//            data.addAll(gravity_y);
//            data.addAll(gravity_z);
//            data.addAll(x);
//            data.addAll(y);
//            data.addAll(z);

            Log.d("test","ssssssssssssssssssssssssss");

            Log.d("test", Arrays.toString(data.toArray()));
            Log.d("test", Arrays.toString(toFloatArray(data)));


//            if (classifier.predictProbabilities(toFloatArray(data)).length == 0) {
//                Log.d("test","get an 0");
//            }

            Log.d("test","get an 1");

            //Log.d("test", Arrays.toString(classifier.predictProbabilities(toFloatArray(data))));

            Log.d("test","get an 2");

            results =  classifier.predictProbabilities(toFloatArray(data));
            Log.d("test", Arrays.toString(results));
            Log.d("test", Integer.toString(results.length));
            downstairsTextView.setText(Float.toString(round(results[0], 2)));
            joggingTextView.setText(Float.toString(round(results[1], 2)));
            sittingTextView.setText(Float.toString(round(results[2], 2)));
            standingTextView.setText(Float.toString(round(results[3], 2)));
            upstairsTextView.setText(Float.toString(round(results[4], 2)));
            walkingTextView.setText(Float.toString(round(results[5], 2)));
            float max = 0;
            int index_max = 0;
            for(int i=0;i<results.length;i++) {
                if (max <= results[i]) {
                    index_max = i;
                    max = results[i];
                }
            }

            x.clear();
            y.clear();
            z.clear();
            linear_acceleration_x.clear();
            linear_acceleration_y.clear();
            linear_acceleration_z.clear();
            gravity_x.clear();
            gravity_y.clear();
            gravity_z.clear();
            new MyJob().execute("http://192.168.1.152:8000?"+labels[index_max]+"="+Integer.toString(index_max));
        }
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }
//    private int[] toFloatArray(List<int> list) {
//        int i = 0;
//        float[] array = new float[list.size()];
//
//        for (Float f : list) {
//            array[i++] = (f != null ? f : Float.NaN);
//        }
//        return array;
//    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }
    public class MyJob extends AsyncTask<String,Void,String> {

        //doInBackground在子线程中执行命令，有耗时的操作写在这里面，
        // doInBackground是必须实现的方法

        //UI线程中声明的变量，子线程内不能使用，两个方法进行转换
        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection con=null;
            InputStream is=null;
            StringBuilder sbd=new StringBuilder();

            try {
                URL url=new URL(strings[0]);
                con= (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5*1000);
                con.setReadTimeout(5*1000);
                /*
                * http响应码：getResponseCode
                  200：成功 404：未找到 500：发生错误
              */
                if (con.getResponseCode()==200){
                    is=con.getInputStream();
                    int next=0;
                    byte[] bt=new byte[1024];
                    while ((next=is.read(bt))>0){
                        sbd.append(new String(bt,0,next));
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (is!=null){
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (con!=null){
                    con.disconnect();
                    //断开连接
                }
            }
            return sbd.toString();
        }
        // doInBackground执行完成后会自动进入onPostExecute方法
        //doInBackground返回的参数是onPostExecute方法的参数  S
        //onPostExecute在UI线程中执行命令
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //doInBackground返回的数据
        }
    }
}


