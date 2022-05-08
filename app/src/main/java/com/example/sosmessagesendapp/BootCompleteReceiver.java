package com.example.sosmessagesendapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("yun_log", "start boot receiver");
        Intent sosIntent = new Intent(context, SendSOSForeground.class);
        if (intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(sosIntent);
            }else {
                context.startService(sosIntent);
            }
        }
    }
}
