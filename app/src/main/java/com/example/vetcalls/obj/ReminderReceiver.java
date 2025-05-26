package com.example.vetcalls.obj;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.vetcalls.R;
import com.example.vetcalls.activities.HomeActivity;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "appointment_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderReceiver", "Received reminder broadcast");

        // קבלת פרטי ההתראה
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e("ReminderReceiver", "No extras found in intent");
            return;
        }

        String title = extras.getString("title", "תזכורת לתור");
        String message = extras.getString("message", "יש לך תור קרוב!");

        // יצירת Intent לפתיחת האפליקציה בלחיצה על ההתראה
        Intent notificationIntent = new Intent(context, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // בניית ההתראה
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.calendar)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // הצגת ההתראה
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
            Log.d("ReminderReceiver", "Notification displayed with ID: " + notificationId);
        } else {
            Log.e("ReminderReceiver", "NotificationManager is null");
        }
    }
}