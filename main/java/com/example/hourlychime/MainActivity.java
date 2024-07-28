package com.example.hourlychime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PICK_SOUND = 1;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 100;
    private Uri notificationSoundUri;
    private AlarmManager alarmManager;
    private SharedPreferences sharedPreferences;
    private static final int REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        sharedPreferences = getSharedPreferences("AlarmPreferences", MODE_PRIVATE);

        Button btnTurnAllOn = findViewById(R.id.btnTurnAllOn);
        Button btnTurnAllOff = findViewById(R.id.btnTurnAllOff);

        btnTurnAllOn.setOnClickListener(v -> toggleAllSwitches(true));
        btnTurnAllOff.setOnClickListener(v -> toggleAllSwitches(false));

        // Set a default notification sound URI
        notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Button testButton = findViewById(R.id.test1); // Adjust the ID to your actual test button ID
        testButton.setOnClickListener(v -> setTestAlarm(this));

        Button btnSound = findViewById(R.id.btnSound);
        btnSound.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, notificationSoundUri);
            startActivityForResult(intent, REQUEST_CODE_PICK_SOUND);
        });

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (int i = 1; i <= 24; i++) {
            int resID = getResources().getIdentifier("swt" + i, "id", getPackageName());
            Switch switchButton = findViewById(resID);
            int hour = i - 1; // Adjusting for 0-23 hours

            // Restore the saved state of the switch
            boolean isChecked = sharedPreferences.getBoolean("swt" + i, false);
            switchButton.setChecked(isChecked);

            switchButton.setOnCheckedChangeListener((buttonView, isCheckedState) -> {
                // Save the state of the switch
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("swt" + (hour + 1), isCheckedState);
                editor.apply();

                if (isCheckedState) {
                    setAlarm(this, hour);
                } else {
                    cancelAlarm(this, hour);
                }
            });
        }

        askForBatteryOptimization();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATION_PERMISSION);
            }
        }
    }
    private void toggleAllSwitches(boolean turnOn) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (int i = 1; i <= 24; i++) {
            int resID = getResources().getIdentifier("swt" + i, "id", getPackageName());
            Switch switchButton = findViewById(resID);
            switchButton.setChecked(turnOn);

            // Save the state of the switch
            editor.putBoolean("swt" + i, turnOn);

            int hour = i - 1; // Adjusting for 0-23 hours
            if (turnOn) {
                setAlarm(this, hour);
            } else {
                cancelAlarm(this, hour);
            }
        }

        editor.apply();
    }

    private void setTestAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("hour", 0); // Just passing 0 for hour, since we won't use it for the test

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Set the alarm to trigger in 30 seconds
        long triggerAtMillis = System.currentTimeMillis() + 30000; // 30 seconds from now
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent); // Use setExact for precision

        Toast.makeText(context, "Test alarm set for 30 seconds!", Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Hourly Chime Channel";
            String description = "Channel for hourly chime notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH; // Change as needed
            NotificationChannel channel = new NotificationChannel("alarm_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_SOUND && resultCode == RESULT_OK) {
            notificationSoundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (notificationSoundUri == null) {
                notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("notificationSoundUri", notificationSoundUri.toString());
            editor.apply();
            Toast.makeText(this, "Sound selected: " + notificationSoundUri.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void askForBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS);
            } else {
                Toast.makeText(this, "Battery optimization is already disabled for this app.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Battery optimization is not applicable on this version.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can send notifications
                Toast.makeText(this, "Chime Notifications Allowed", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Chime Notifications Not Allowed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void setAlarm(Context context, int hour) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        SharedPreferences sharedPreferences = context.getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE);
        String soundUriString = sharedPreferences.getString("notificationSoundUri", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
        intent.putExtra("soundUri", soundUriString);
        intent.putExtra("hour", hour);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, hour, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, hour, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.d("MainActivity", "Alarm set for hour: " + hour);
        Toast.makeText(context, "Alarm set for hour: " + hour, Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, int hour) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, hour, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, hour, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Toast.makeText(context, "Alarm canceled for hour: " + hour, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Battery optimization is still enabled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Battery optimization is disabled for this app.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }
}
