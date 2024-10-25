package com.neolit.womensos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    Button send;
    FusedLocationProviderClient fusedLocationClient;
    String currentLocationString;
    String TAG = "SOS";
    dbHelper databaseHelper;
    FloatingActionButton mAbout;

    @Override
    @SuppressWarnings("deprecation") //TODO: Remove along with SmsManager.getDefault() when minsdk is raised.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        send=findViewById(R.id.button);
        mAbout=findViewById(R.id.about);
        databaseHelper = new dbHelper(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getSMS();
        getLocation();

        if(currentLocationString==null){
            Toast.makeText(getApplicationContext(), "Please enable location first!", Toast.LENGTH_LONG).show();
        }

        send.setOnClickListener(view -> {
            Log.i(TAG, "Location: " + currentLocationString);
            try {
                SmsManager smsManager;

                // For API 31+ (Android 12+), use getSystemService to get SmsManager instance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = getSystemService(SmsManager.class);
                } else {
                    // For API 29-30, fall back to SmsManager.getDefault()
                    smsManager = SmsManager.getDefault();
                }

                if(currentLocationString==null){
                    Toast.makeText(getApplicationContext(), "Please enable location first!", Toast.LENGTH_LONG).show();
                }

                if(!currentLocationString.contains("SOS ALERT")){
                    currentLocationString+=" SOS ALERT - HELP NEEDED ASAP";
                }

                Cursor cursor = databaseHelper.getAllContacts();

                if (cursor != null) {
                    int numberIndex = cursor.getColumnIndex("number");

                    if (numberIndex >= 0) {
                        while (cursor.moveToNext()) {
                            String number = cursor.getString(numberIndex);
                            Log.i(TAG, "number: " + number);
                            smsManager.sendTextMessage(number, null, currentLocationString, null, null);
                        }
                    }
                    cursor.close();
                }
                Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "SMS failed", e);
                Toast.makeText(getApplicationContext(), "Failed to sent message", Toast.LENGTH_LONG).show();
            }
        });

        Button mAdd = findViewById(R.id.add);
        mAdd.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, add.class);
            startActivity(intent);
        });

        mAbout.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
            builder.setMessage("It is a simple Android app by Sarthak Roy to send SOS Calls to your emergency contacts.");
            builder.setTitle("About the App");
            builder.setNegativeButton("Done", (dialog, which) -> dialog.cancel());
            builder.show();
        });

    }

    private void getSMS(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1000);
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            MaterialAlertDialogBuilder builder = getMaterialAlertDialogBuilder();
            builder.show();
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocationString = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
                    }
                });
    }

    private @NonNull MaterialAlertDialogBuilder getMaterialAlertDialogBuilder() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
        builder.setMessage("Location Services are disabled. You want to enable?");
        builder.setTitle("Warning!");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });
        builder.setNegativeButton("No", (dialog, which) -> finish());
        return builder;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            }
        }
    }
}