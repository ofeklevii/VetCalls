package com.example.vetcalls.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.example.vetcalls.R;
import com.example.vetcalls.activities.HomeActivity;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "alarm working", Toast.LENGTH_LONG).show();
        // Retrieve reminder data
        Bundle bundle = intent.getExtras();
        String title = "Reminder";
        String message = "You have an appointment!";

        if (bundle != null) {
            title = bundle.getString("title", "Reminder");
            message = bundle.getString("message", "You have an appointment!");
        }

        // Display a toast message
        Toast.makeText(context, "Reminder: " + title, Toast.LENGTH_LONG).show();

        // Create notification intent
        Intent activityIntent = new Intent(context, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.calendar)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }
}
