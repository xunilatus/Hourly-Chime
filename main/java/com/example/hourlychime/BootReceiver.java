package com.example.hourlychime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Reset all alarms
            SharedPreferences sharedPreferences = context.getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE);
            for (int i = 0; i < 24; i++) {
                boolean isAlarmSet = sharedPreferences.getBoolean("swt" + (i + 1), false);
                if (isAlarmSet) {
                    MainActivity.setAlarm(context, i);
                }
            }
        }
    }
}

