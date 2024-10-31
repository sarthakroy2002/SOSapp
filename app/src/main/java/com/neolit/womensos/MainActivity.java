package com.neolit.womensos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Button send;
    FusedLocationProviderClient fusedLocationClient;
    String currentLocationString;
    String TAG = "SOS";
    dbHelper databaseHelper;
    FloatingActionButton mAbout;
    private static final int LOCATION_RETRY_INTERVAL = 5000; // Retry every 5 seconds
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private Timer locationRetryTimer;

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

        send = findViewById(R.id.button);
        mAbout = findViewById(R.id.about);
        databaseHelper = new dbHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkAndRequestPermissions();
        initializeLocationRetry();

        send.setOnClickListener(view -> sendSOSMessage());

        Button mAdd = findViewById(R.id.add);
        mAdd.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, add.class);
            startActivity(intent);
        });

        mAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private void initializeLocationRetry() {
        fetchLocation();
        locationRetryTimer = new Timer();
        locationRetryTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentLocationString == null) {
                    fetchLocation();
                } else {
                    locationRetryTimer.cancel();
                }
            }
        }, LOCATION_RETRY_INTERVAL, LOCATION_RETRY_INTERVAL);
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                getMaterialAlertDialogBuilder().show();
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocationString = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
                    Log.i(TAG, "Location acquired: " + currentLocationString);
                } else {
                    Log.w(TAG, "Failed to acquire location, retrying...");
                }
            });
        }
    }

    private void sendSOSMessage() {
        if (currentLocationString == null) {
            showToast("Please enable location first!", Toast.LENGTH_LONG);
            return;
        }

        try {
            SmsManager smsManager = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? getSystemService(SmsManager.class) : SmsManager.getDefault();

            if (!currentLocationString.contains("SOS ALERT")) {
                currentLocationString += " SOS ALERT - HELP NEEDED ASAP";
            }

            Cursor cursor = databaseHelper.getAllContacts();
            if (cursor != null) {
                int numberIndex = cursor.getColumnIndex("number");
                if (numberIndex >= 0) {
                    while (cursor.moveToNext()) {
                        String number = cursor.getString(numberIndex);
                        Log.i(TAG, "Sending SOS to: " + number);
                        smsManager.sendTextMessage(number, null, currentLocationString, null, null);
                    }
                }
                cursor.close();
            }
            showToast("SOS Message Sent Successfully!", Toast.LENGTH_SHORT);
        } catch (Exception e) {
            Log.e(TAG, "SMS failed", e);
            showToast("Failed to send message", Toast.LENGTH_LONG);
        }
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("This is an SOS App by Sarthak Roy to send alerts to your emergency contacts.")
                .setTitle("About the App")
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showToast(String message, int duration) {
        Toast.makeText(getApplicationContext(), message, duration).show();
    }

    private @NonNull MaterialAlertDialogBuilder getMaterialAlertDialogBuilder() {
        return new MaterialAlertDialogBuilder(this)
                .setMessage("Location Services are disabled. Would you like to enable them?")
                .setTitle("Warning!")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("No", (dialog, which) -> finish());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                showToast("Permissions are required for the app to function", Toast.LENGTH_LONG);
            }
        }
    }
}
