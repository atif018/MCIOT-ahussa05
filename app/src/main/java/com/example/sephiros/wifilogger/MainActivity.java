/* An app for collecting the Wifi Access Points found around an Android device and uploading a list of those
devices to Firebase. The GPS location of the device and the phone ID will also be uploaded. The number of APs
found will be shown on the main activity screen.  */

package com.example.sephiros.wifilogger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiScanner; // Initialises WifiManager object
    private String stopLog = "Stop Logging"; // Use this string object for setting text on the Logging button
    private String startLog = "Start Logging"; // Use this string object for setting text on the Logging button
    private IntentFilter intentFilter; // IntentFilter for use with getting Scan results
    private List<ScanResult> listOfAps; // For storing the results of the Wifi scan
    private BroadcastReceiver wifiScanReceiver; // Initialisation of a Receiver to receive system events
    int i = 0; // Will keep a count of the number of APs found
    int j = 1; // Will be used as a count variable for use in the Firebase database for access point numbers
    private FusedLocationProviderClient mFusedLocationClient; // Initialising the Location Service
    private String phoneId; // Where the Phone ID will be stored
    private Button beginLog; // Button for starting the logging process
    private String gpsLocation; // Initialising a String object that will hold the GPS location of the device as a String
    private ArrayList <String> storeAps = new ArrayList<>();
    // Declaring an ArrayList to hold APs found and eliminate duplicates before sending to Firebase
    private TextView apTextView; // A TextView that will show the number of APs
    private Boolean compareApElement; // For use when comparing whether a Wifi AP has previously been uploaded to Firebase

    // https://developer.android.com/guide/topics/location/strategies

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beginLog = findViewById(R.id.startLogging_button); // Create Button object using the logging_button state attributes
        beginLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String logButtonStatus = beginLog.getText().toString(); // Returns what text the logButton object has when the button is clicked, converts to a string and then stores in logButtonStatus
                if (logButtonStatus.equals(startLog)) { // Checks if the text in logButtonStatus equals "Start Logging" and, if it is, changes the text to "Stop Logging" for when the user wants to stop
                    beginLog.setText(stopLog);
                    wifiScanner.setWifiEnabled(true);
                    wifiScanner.startScan();
                } else { // If the text in logButtonStatus is not "Start Logging", then the text is changed to the string in startLog.
                    beginLog.setText(startLog);
                }
            }
        });

        apTextView = findViewById(R.id.ap_text_view);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

        /* Both https://developer.android.com/reference/android/provider/Settings.Secure &
        https://medium.com/@ssaurel/how-to-retrieve-an-unique-id-to-identify-android-devices-6f99fd5369eb
        say to use ANDROID_ID for the purposes of extracting a unique ID of the phone/Android OS that you're using. the Google
        Dev website wasn't clear on how to explicitly use the method but the Medium website had a clear way to
        do so which I incorporated below. I tried using Settings.Secure.ANDROID_ID but it returned
        "ANDROID_ID" as the ID and that wasn't useful. */

        phoneId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        // Get Android ID as a String and store in phoneID

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {

                            double gpsLatitude = location.getLatitude();
                            double gpsLongitude = location.getLongitude();

                            gpsLocation = "" + gpsLatitude + ", " + gpsLongitude;
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "Please turn on your GPS in Location Settings!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        /* Used https://developer.android.com/guide/topics/connectivity/wifi-scan and
        https://developer.android.com/reference/android/net/wifi/WifiManager as a guide to incorporate
        the Wifi feature of the app as well as using parts of the example code. */

        wifiScanner = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // Creates a WifiManager object upon which methods can be used.

        //https://developer.android.com/reference/android/net/wifi/ScanResult

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                listOfAps = wifiScanner.getScanResults();

                for(ScanResult element : listOfAps) {
                    compareApElement = false;
                    String getSsid = element.SSID;
                    String getBssid = element.BSSID;
                    String getRssi = "" + element.level;
                    String getFrequency = "" + element.frequency;

                    firebaseWrite(getSsid, getBssid, getRssi, getFrequency, gpsLocation, phoneId);
                    // Sends variables as argument to firebaseWrite method

                }
                }
            };


        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

    }

    // Used https://developer.android.com/reference/android/widget/Button as a reference

   public void fireConfig(View view) {
       Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://console.firebase.google.com/u/0"));
           if (intent.resolveActivity(getPackageManager()) != null) {
               startActivity(intent);
           }
    }

    /* Set up Firebase and used some example code from https://firebase.google.com/docs/android/setup
    & https://firebase.google.com/docs/database/android/start in the method below. */

    public void firebaseWrite(String getSsid, String getBssid, String getRssi, String getFrequency,
                              String gpsLocation, String phoneId) {
        String message = "Access Point: " + j;
        String simpleApInfo = "SSID: " + getSsid + ", " + "BSSID: " + getBssid;
        String info = "SSID: " + getSsid + ", " + "BSSID: " + getBssid + ", " + "RSSI: " + getRssi + ", " +
                "Frequency: " + getFrequency + ", " + "Phone ID: " + phoneId + ", " + "GPS Location: " + gpsLocation;
        // A string that contains all the information that will be written to database
        FirebaseDatabase database = FirebaseDatabase.getInstance(); // Gets instance of database
        DatabaseReference wifiFireDb = database.getReference(message); // Location where information will be written to

        for (String element : storeAps) {
            if (simpleApInfo.equals(element)) {
                compareApElement = true;
                return;
            }
        }
        if (storeAps.size() == 0) {
            storeAps.add(simpleApInfo);
            wifiFireDb.child("GPS Location: ").setValue(gpsLocation); // Writes to Firebase database
            wifiFireDb.child("Phone ID: ").setValue(phoneId); // Writes to Firebase database
            wifiFireDb.child("Frequency: ").setValue(getFrequency); // Writes to Firebase database
            wifiFireDb.child("RSSI: ").setValue(getRssi); // Writes to Firebase database
            wifiFireDb.child("BSSID: ").setValue(getBssid); // Writes to Firebase database
            wifiFireDb.child("SSID: ").setValue(getSsid); // Writes to Firebase database
            i++; // Keeps a count of the number of Wifi APs available
            j++; // Increases Access Point Number
        }
        else {
            storeAps.add(simpleApInfo);
            wifiFireDb.child("GPS Location: ").setValue(gpsLocation); // Writes to Firebase database
            wifiFireDb.child("Phone ID: ").setValue(phoneId); // Writes to Firebase database
            wifiFireDb.child("Frequency: ").setValue(getFrequency); // Writes to Firebase database
            wifiFireDb.child("RSSI: ").setValue(getRssi); // Writes to Firebase database
            wifiFireDb.child("BSSID: ").setValue(getBssid); // Writes to Firebase database
            wifiFireDb.child("SSID: ").setValue(getSsid); // Writes to Firebase database
            i++; // Keeps a count of the number of Wifi APs available
            j++; // Increases Access Point Number in Firebase
            apTextView.setText("Number of APs available: " + i);
        }
    }

    public void freqLog(View view) {

    }

}
