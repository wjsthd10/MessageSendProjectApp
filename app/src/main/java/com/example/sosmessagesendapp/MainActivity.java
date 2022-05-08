package com.example.sosmessagesendapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

public class MainActivity extends AppCompatActivity {// 인트로로 사용

    private static final int GPS_REQUEST_CODE=1001;
    private static final int PERMISSION_REQUEST_CODE=1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isLocationEnabled()) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    },
                    PERMISSION_REQUEST_CODE);
//                    Intent intent=new Intent(MainActivity.this, MapActivity.class);
//                    startActivity(intent);
        }else {
            setGPS_start();
        }

    }

    private void setGPS_start(){
        android.app.AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("위치정보 설정이 비활성화 상태입니다.");
        builder.setMessage("위치정보 설정화면으로 이동하시겠습니까?");
        builder.setPositiveButton("이동", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("취소", null);
        builder.create().show();
    }

    public boolean isLocationEnabled(){// 위치정보 활성화 여부 리턴
        int locationMode=0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                locationMode= Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE);
            }catch (Exception e){
                e.getMessage();
                return false;
            }
            return locationMode!=Settings.Secure.LOCATION_MODE_OFF;
        }else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED && grantResults[1]== PackageManager.PERMISSION_GRANTED ){// 앱 이용 권한이 있을 경우
                if (isLocationEnabled()) {
                    Intent intent=new Intent(this, MapActivity.class);
                    startActivity(intent);
                    finish();
                }else {
                    setGPS_start();
                }

            }
        }
    }

}