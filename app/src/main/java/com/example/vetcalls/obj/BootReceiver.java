package com.example.vetcalls.obj;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Device rebooted, rescheduling reminders...");
            String userId = null;
            try {
                userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            } catch (Exception e) {
                Log.e("BootReceiver", "Failed to get userId after boot", e);
            }
            if (userId == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users").document(userId)
                .collection("Reminders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    long now = System.currentTimeMillis();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long time = doc.getTimestamp("time") != null ? doc.getTimestamp("time").toDate().getTime() : null;
                        if (time != null && time > now) {
                            String title = doc.getString("title");
                            String description = doc.getString("description");
                            notificationHelper.scheduleNotification(context, title, description, time);
                        }
                    }
                });
        }
    }
} 