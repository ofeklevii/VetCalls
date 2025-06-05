package com.example.vetcalls.obj;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Helper class for managing notification scheduling and cancellation.
 * Handles notification channel creation, exact alarm scheduling with proper permission handling,
 * and provides functionality for both scheduling and canceling notifications.
 *
 * @author Ofek Levi
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "appointment_channel";
    private static final String CHANNEL_NAME = "תזכורות תורים";
    private final Context context;

    /**
     * Constructor that initializes the NotificationHelper and creates the notification channel.
     *
     * @param context The application context
     */
    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    /**
     * Creates a notification channel for Android 8.0 (API level 26) and above.
     * Required for displaying notifications on newer Android versions.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("תזכורות עבור תורים ומטלות");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Schedules a notification to be displayed at a specific time.
     * Handles permission checks for exact alarms on Android 12+ and creates unique request codes.
     *
     * @param context The application context
     * @param title The notification title
     * @param message The notification message content
     * @param timeInMillis The time in milliseconds when the notification should be displayed
     */
    @SuppressLint("ScheduleExactAlarm")
    public void scheduleNotification(Context context, String title, String message, long timeInMillis) {
        if (timeInMillis <= System.currentTimeMillis()) {
            Log.w("NotificationHelper", "Cannot schedule notification in the past");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        int requestCode = (int) (timeInMillis % Integer.MAX_VALUE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    openExactAlarmSettings(context);
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }

            Log.i("NotificationHelper", "Notification scheduled for: " + timeInMillis);
        }
    }

    /**
     * Opens the system settings for exact alarm permissions on Android 12+.
     * Displays a toast message to guide the user through the permission process.
     *
     * @param context The application context
     */
    private void openExactAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);

            Toast.makeText(context, "נא לאשר התראות מדויקות בהגדרות", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Cancels a previously scheduled notification.
     * Removes both the scheduled alarm and any currently displayed notification.
     *
     * @param context The application context
     * @param notificationId The unique identifier of the notification to cancel
     */
    public void cancelNotification(Context context, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }
}