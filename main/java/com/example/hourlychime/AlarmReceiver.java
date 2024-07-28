package com.example.hourlychime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int hour = intent.getIntExtra("hour", -1); // Retrieve the hour from the intent
        if (hour != -1) { // Check if the hour is valid
            Log.d(TAG, "Alarm received for hour: " + hour);

            // Acquire a wake lock to ensure the CPU is awake while processing the alarm
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::AlarmWakeLock");
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

            // Create the notification channel
            createNotificationChannel(context);

            // Play the alarm sound and show the notification
            playAlarmSound(context, hour);

            // Release the wake lock
            wakeLock.release();
        } else {
            Log.d(TAG, "Invalid hour received");
        }
    }

    private void playAlarmSound(Context context, int hour) {
        try {
            // Retrieve the sound URI from SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE);
            String soundUriString = sharedPreferences.getString("notificationSoundUri", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            Uri soundUri = Uri.parse(soundUriString); // Convert the string back to a Uri

            // Send a notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "alarm_channel")
                    .setSmallIcon(R.mipmap.ic_launcher) // Change to your app icon
                    .setContentTitle("Hourly Chime")
                    .setContentText("It's " + hour + ":00!")
                    .setSound(soundUri) // Set the custom notification sound
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            Log.d(TAG, "NotificationManagerCompat initialized");

            notificationManager.notify(hour, builder.build());
            Log.d(TAG, "Notification posted for hour: " + hour);

            // Play the sound
            /*Ringtone ringtone = RingtoneManager.getRingtone(context, soundUri);
            if (ringtone != null) {
                ringtone.play();
                Log.d(TAG, "Playing ringtone: " + soundUriString);
            }*/
        } catch (Exception e) {
            Log.e(TAG, "Error in playAlarmSound: ", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Hourly Chime Channel";
            String description = "Channel for hourly chime notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("alarm_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            } else {
                Log.e(TAG, "NotificationManager is null, could not create notification channel");
            }
        }
    }
}
