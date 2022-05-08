package com.example.sosmessagesendapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SendSOSForeground extends Service implements SensorEventListener, LocationListener {

    public final String CHANNEL_ID = "shakeEventChannel";
    public static final int NOTIFICATION_ID = 1001;

    SensorManager sensorManager;
    Sensor sensor;


    ICallback mCallback;
    final IBinder mBinder = new BindServiceBinder();

    private long shakeTime;// 흔들림 감지 시간
    private static final int SHAKE_SKIP_TIME = 500;// 연속 흔들림 감지 0.5초 뒤에 흔들림이 감지되면 무시
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7f;// 중력 가속도 높을 수록 강하게 흔들어야 감지가능 2.7f
    NotificationManagerCompat notificationManagerCompat;
    int trafficsTag = 2255;
    int shakeCount = 0;

    TimerTask tt;
    Timer timer = new Timer();

    TimerTask locationTimerTask;
    Timer locationTimer = new Timer();
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 20;// = 1200000

    double lat = 0;
    double lng = 0;

    String tGroupCd = "KI00000123", tCustId = "gme00036", tProvider = "F", tType = "S";// test date
    String locationTime = "", locationAccuracy = "";

    protected LocationManager locationManager;
    Context mContext;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;


    public SendSOSForeground() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        createNotiChannel();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        TrafficStats.setThreadStatsTag(trafficsTag);
        getLocation();

        Log.e("yun_log", "onCreate");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder;
        builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID);
        builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
        builder.setVibrate(new long[]{0});
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("SOS신호 감지중");
        builder.setAutoCancel(true);
        builder.setOnlyAlertOnce(true);
        builder.setPriority(NotificationManagerCompat.IMPORTANCE_MIN);

        notificationManagerCompat = NotificationManagerCompat.from(getBaseContext());
        startForeground(NOTIFICATION_ID, builder.build());
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        new Thread(new Runnable() {
            @Override
            public void run() {
                locationTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        Log.e("yun_log", "send location timer");
                        scanLocationTimer();
                    }
                };

                locationTimer = new Timer();
                locationTimer.schedule(locationTimerTask, 5000, MIN_TIME_BW_UPDATES);
            }
        });


        return START_STICKY;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            float gravityX = axisX / SensorManager.GRAVITY_EARTH;
            float gravityY = axisY / SensorManager.GRAVITY_EARTH;
            float gravityZ = axisZ / SensorManager.GRAVITY_EARTH;

            Float f = gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ;
            double squaredD = Math.sqrt(f.doubleValue());
            float gForce = (float) squaredD;
            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                long currentTime = System.currentTimeMillis();
                if (shakeTime + SHAKE_SKIP_TIME > currentTime) {// 0.5초 이내로 흔들림 감지 되지 않을 경우 리턴
                    return;
                }
                shakeTime = currentTime;

                tt = new TimerTask() {
                    @Override
                    public void run() {
                        Log.e("yun_log", "shakeCount clear = " + shakeCount);
                        shakeCount = 0;
                        timer.cancel();
                    }
                };

                if (shakeCount == 0 ) {
                    timer = new Timer();
                    timer.schedule(tt, 1300, 1);
                }

                shakeCount++;
                // 진동으로 횟수 추가됨 표시
                Log.e("yun_log", "shakeCount = " + shakeCount);
                if (shakeCount > 2) {
                    Log.e("yun_log", "shakeEvent star");
                    shakeEventOn();
                    shakeCount = 0;
                    timer.cancel();
                }

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.e("yun_log", "location listner in");
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER) || location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            if (lat != location.getLatitude() || lng != location.getLongitude()) {
                lat = location.getLatitude();
                lng = location.getLongitude();
                locationTime = getHourMinuteSecond(location.getTime());
                locationAccuracy = Math.round(location.getAccuracy()) + "";
            }
        }else {
            if (getLocation() != null) {
                lat = getLocation().getLatitude();
                lng = getLocation().getLongitude();
                locationTime = getHourMinuteSecond(getLocation().getTime());
                locationAccuracy = Math.round(getLocation().getAccuracy()) + "";
            }
        }
    }

    public class BindServiceBinder extends Binder {
        public SendSOSForeground getService() {
            return SendSOSForeground.this;
        }
    }

    public interface ICallback {
        public void remoteCallIsShaked();

        public void scanLocation();
    }

    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    public void shakeEventOn() {
        if (mCallback != null) {
            mCallback.remoteCallIsShaked();
        } else {
            getLocation();
            Log.e("yun_log", "get location data  = "+getLocation().getLatitude()+", "+getLocation().getLongitude());
        }
    }

    public void scanLocationTimer() {
        if (mCallback != null) {
            mCallback.scanLocation();
        } else {
            getLocation();
        }
    }

    private void createNotiChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "위치전송";
            String description = "location date send";

            @SuppressLint("WrongConstant")
            NotificationChannel notiChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            notiChannel.setVibrationPattern(new long[]{0});
            notiChannel.enableVibration(true);
            notiChannel.setDescription(description);

            NotificationManager notiManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notiManager.createNotificationChannel(notiChannel);

        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }



    Handler sHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public static String getHourMinuteSecond(long timeMillis) {
        long currentTime = (timeMillis == 0 ? System.currentTimeMillis() : timeMillis);
        Date date = new Date(currentTime);
        SimpleDateFormat aaa = new SimpleDateFormat("HH:mm:ss");
        String getTime = aaa.format(date);

        return getTime;
    }

    private Location getLocation() {
        Location location = null;
        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("yun_log", "no permission return");
            return null;
        }
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        if (locationManager != null) {
            location = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                // 위도 경도 저장
                lat = location.getLatitude();
                lng = location.getLongitude();
            }
        }


        if (location == null) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
            }
        }

        return location;
    }

}
