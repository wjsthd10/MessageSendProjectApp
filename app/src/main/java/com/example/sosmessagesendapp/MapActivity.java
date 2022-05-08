package com.example.sosmessagesendapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.MapView;
import com.naver.maps.map.MapView;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.util.FusedLocationSource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_LOCATION = 2022;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10021;

    private static final int NETWORK_DISCONNECTED=3001;
    private static final int NETWORK_CONNECTED=3002;
    private static final int SEND_SOS_MESSAGE=3003;
    private static final int SEND_LOCATION_MESSAGE=3004;

    MapView mapView;
    NaverMap nMap;
    FusedLocationSource locationSource;

    SendSOSForeground mSosService;

//    String tGroupCd="AC00000137", tCustId="cai08123", tProvider="F", tType="S";
    String tGroupCd="KI00000123", tCustId="gme00036", tProvider="F", tType="S";

    BroadcastReceiver br;

//    private long shakeTime;// 흔들림 감지 시간
//    private static final int SHAKE_SKIP_TIME=500;// 연속 흔들림 감지 0.5초 뒤에 흔들림이 감지되면 무시
//    private static final float SHAKE_THRESHOLD_GRAVITY=2.7f;// 중력 가속도 높을 수록 강하게 흔들어야 감지가능 2.7f
//
//    SensorManager sensorManager;
//    Sensor sensor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.map_view);
        Log.e("yun_log", "onCreate mapview");

//        sensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);
//        sensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        mapView.getMapAsync(this);
        Log.e("yun_log", "onCreate getMapAsync");
        BroadcastReceiver br2 = new BootCompleteReceiver();
        if (!br2.isOrderedBroadcast() && !br2.isInitialStickyBroadcast()) {
            Log.e("yun_log", "receiver set");
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
            registerReceiver(br2, intentFilter);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e("yun_log", "permissions = " + Arrays.toString(permissions) + ", requestCode = " + requestCode);
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                nMap.setLocationTrackingMode(LocationTrackingMode.Follow);
                nMap.setLocationSource(locationSource);
                Log.e("yun_log", "return None");
                nMap.setMaxZoom(21.0);
                nMap.setMinZoom(8.0);

            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.e("yun_log", "in onMapReady");
        Intent fIntent=new Intent(this, SendSOSForeground.class);
        bindService(fIntent, mConnection, Context.BIND_AUTO_CREATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(fIntent);
        }else {
            startService(fIntent);
        }
        nMap = naverMap;
        UiSettings uiSettings = nMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        if (!locationSource.isActivated()) {
            if (locationSource.getLastLocation() != null) {
                Double lat =locationSource.getLastLocation().getLatitude();
                Double lng = locationSource.getLastLocation().getLongitude();
                LatLng latLng = new LatLng(lat, lng);
                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(latLng);
                nMap.moveCamera(cameraUpdate);
            }
            nMap.setLocationSource(locationSource);
            nMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            nMap.setMaxZoom(21.0);
            nMap.setMinZoom(8.0);
        }
    }

    private Location getLocation(){
        Location location=null;
        if (locationSource.getLastLocation() != null) {
            location=locationSource.getLastLocation();
        }
        return location;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SendSOSForeground.BindServiceBinder binder = (SendSOSForeground.BindServiceBinder) service;
            mSosService = binder.getService();
            mSosService.registerCallback(mCallback);
            Log.e( "yun_log","onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSosService=null;
        }
    };

    private SendSOSForeground.ICallback mCallback = new SendSOSForeground.ICallback() {
        @Override
        public void remoteCallIsShaked() {// SOS신호
            Message message=new Message();
            message.what=SEND_SOS_MESSAGE;
            tType="S";
            sHandler.sendMessage(message);
            Log.e("yun_log", "in callback");
        }

        @Override
        public void scanLocation() {// 실시간 위치정보
            tType="M";
            sHandler.sendEmptyMessage(SEND_LOCATION_MESSAGE);
        }
    };

    public void setCallback(){
        mCallback = new SendSOSForeground.ICallback() {
            @Override
            public void remoteCallIsShaked() {// SOS신호
                Message message=new Message();
                message.what=SEND_SOS_MESSAGE;
                tType="S";
                sHandler.sendMessage(message);
                Log.e("yun_log", "in callback");
            }

            @Override
            public void scanLocation() {// 실시간 위치정보
                tType="M";
                sHandler.sendEmptyMessage(SEND_LOCATION_MESSAGE);
            }
        };
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                SendSOSForeground.BindServiceBinder binder = (SendSOSForeground.BindServiceBinder) service;
                mSosService = binder.getService();
                mSosService.registerCallback(mCallback);
                Log.e( "yun_log","onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mSosService=null;
            }
        };
    }


    private Handler sHandler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){

                case NETWORK_CONNECTED:
                    Log.e("yun_log", "NETWORK_CONNECTED");
                    break;
                case NETWORK_DISCONNECTED:
                    Log.e("yun_log", "NETWORK_DISCONNECTED");
                    break;
                case SEND_SOS_MESSAGE:
                    // 구조신호 발송 코드
                    break;

                case SEND_LOCATION_MESSAGE:// 위치정보 지속적으로 확인하는 코드

                    break;
            }

        }
    };

    public static String getHourMinuteSecond(long timeMillis) {
        long currentTime = (timeMillis == 0 ? System.currentTimeMillis() : timeMillis);
        Date date = new Date(currentTime);
        SimpleDateFormat aaa = new SimpleDateFormat("HH:mm:ss");
        String getTime = aaa.format(date);

        return getTime;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}