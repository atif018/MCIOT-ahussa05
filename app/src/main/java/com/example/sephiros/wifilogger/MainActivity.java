/* An app for collecting the Wifi Access Points found around an Android device and uploading a list of those
devices to Firebase. The GPS location of the device and the phone ID will also be uploaded. The number of APs
found will be shown on the main activity screen. I mainly used developer.android.com for source material, their
documentation to learn about how things work and come together, and example/sample code to get started. */

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
import android.widget.EditText;
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

    private WifiManager mWifiScanner; // Initialises WifiManager object
    private String mStopLog = "Stop Logging"; // Use this string object for setting text on the Logging button
    private String mStartLog = "Start Logging"; // Use this string object for setting text on the Logging button
    private IntentFilter mIntentFilter; // IntentFilter for use with getting Scan results
    private List<ScanResult> mListOfAps; // For storing the results of the Wifi scan
    private BroadcastReceiver mWifiScanReceiver; // Initialisation of a Receiver to receive system events
    int i = 0; // Will keep a count of the number of APs found
    int j = 1; // Will be used as a count variable for use in the Firebase database for access point numbers
    private FusedLocationProviderClient mFusedLocationClient; // Initialising the Location Service
    private String mPhoneId; // Where the Phone ID will be stored
    private Button mBeginLog; // Button for starting the logging process
    private String mGpsLocation; // Initialising a String object that will hold the GPS location of the device as a String
    private ArrayList <String> mStoreAps = new ArrayList<>();
    // Declaring an ArrayList to hold APs found and eliminate duplicates before sending to Firebase
    private TextView mApTextView; // A TextView that will show the number of APs
    Boolean mCompareApElement; // For use when comparing whether a Wifi AP has previously been uploaded to Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Copyright to gus27 who provided a solution in the link:
        https://stackoverflow.com/questions/39455722/android-wifi-scan-broadcastreceiver-for-scan-results-available-action-not-gett
        for a compact and easy way to request permissions from the user which has been used below and modified to incorporate extra permissions */

        String[] PERMS_INITIAL={ // Declare array of permissions
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.INTERNET,
        };
        ActivityCompat.requestPermissions(this, PERMS_INITIAL, 127); // Request permissions

        // For Button mBeginLog, used https://developer.android.com/reference/android/widget/Button as a guide and for example code

        mBeginLog = findViewById(R.id.startLogging_button); // Create Button object for initiating logging

        mApTextView = findViewById(R.id.ap_text_view); // Creates a TextView object for showing the number of APs found

        // The following is code to check for permissions
        // Used as a guide and some code from https://developer.android.com/training/permissions/requesting

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Please allow permission to access your location!", Toast.LENGTH_SHORT).show();
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(getApplicationContext(), "Please allow permission to access your location!", Toast.LENGTH_SHORT).show();
            } else { // I wanted to incorporate requesting permissions here but ran out of time
            }
        }
        else {
        }

        /* Both https://developer.android.com/reference/android/provider/Settings.Secure &
        https://medium.com/@ssaurel/how-to-retrieve-an-unique-id-to-identify-android-devices-6f99fd5369eb
        say to use ANDROID_ID for the purposes of extracting a unique ID of the phone/Android OS that you're using. the Google
        Dev website wasn't clear on how to explicitly use the method but the Medium website had a clear way to
        do so which I incorporated below. I tried using Settings.Secure.ANDROID_ID but it returned
        "ANDROID_ID" as the ID and that wasn't useful */

        // Get Android ID as a String and store in mPhoneID

        mPhoneId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        /* For mFusedLocationClient and the following, used https://developer.android.com/training/location/retrieve-current &
        https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient#requestLocationUpdates(com.google.android.gms.location.LocationRequest,%20com.google.android.gms.location.LocationCallback,%20android.os.Looper)
        & https://developer.android.com/training/location/receive-location-updates
        as a guide and used some code from there as well. The following is for the purposes of obtaining a GPS location for the device */

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this); // Create FusedLocationProviderClient object
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() { // Use the object to find Location
            @Override
            public void onSuccess(Location location) {

                if (location != null) { // Only if location isn't null will the following statements be carried out for GPS coordinates
                    // mFusedLocationClient.requestLocationUpdates(); Wanted to use this and the guide above to request constant updates but ran out of time
                    double gpsLatitude = location.getLatitude(); // Gets Latitude from Location object and stores as a double
                    double gpsLongitude = location.getLongitude(); // Gets Longitude from Location object and stores as a double

                    mGpsLocation = "" + gpsLatitude + ", " + gpsLongitude; // Stores Latitude and Longitude as a String
                }
                else {
                    // If location is equal to null, User is asked to check settings as one option
                    Toast.makeText(getApplicationContext(), "Please check Location Settings!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /* Used https://developer.android.com/guide/topics/connectivity/wifi-scan and
        https://developer.android.com/reference/android/net/wifi/WifiManager as a guide to incorporate
        the Wifi feature of the app as well as using parts of the example code. */

        // Creates a WifiManager object upon which methods can be used.

        mWifiScanner = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Used https://developer.android.com/reference/android/widget/Button as a guide and for example code for beginLog actions

        // A listener that waits for the Logging button to be clicked

        mBeginLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Uses the text stored in the mBeginLog object when the button is clicked, converts to a string and then stores in logButtonStatus

                String logButtonStatus = mBeginLog.getText().toString();

                /* Checks if the text in logButtonStatus equals "Start Logging" and, if it is, changes the text to "Stop Logging" for when the user
                wants to stop logging. If 'If' condition is true, changes Button text to stopLog variable text, starts Wifi and does a scan for Wifi APs */

                if (logButtonStatus.equals(mStartLog)) {
                    mBeginLog.setText(mStopLog);
                    mWifiScanner.setWifiEnabled(true);
                    mWifiScanner.startScan();

                }

                // If the text in logButtonStatus is not "Start Logging", then the text is changed to the string in startLog.

                else {
                    mBeginLog.setText(mStartLog);
                    // Here, I tried to unregister the Broadcast Receiver using unregister(<Name of receiver>) as a way of stopping the scan but the app kept crashing as a result
                }
            }
        });

        /* Used https://developer.android.com/reference/android/net/wifi/ScanResult as a guide as well as using
        some of the example code to get specific results from Scan results like the SSID, BSSID and others.

        Also used https://developer.android.com/guide/topics/connectivity/wifi-scan as a guide as well as some code */

        mWifiScanReceiver = new BroadcastReceiver() { // Registers new Broadcast Receiver
            @Override
            public void onReceive(Context c, Intent intent) {
                mListOfAps = mWifiScanner.getScanResults(); // Gets scan results from WifiManager object and stores in mListOfAps

                for(ScanResult element : mListOfAps) { // Checks each element in mListOfAps
                    mCompareApElement = false; // Boolean variable used to stop duplicate AP entries being sent to Firebase if true
                    String getSsid = element.SSID;  // Stores SSID from element as String
                    String getBssid = element.BSSID; // Stores BSSID from element as String
                    String getRssi = "" + element.level; // Stores RSSI from element as String
                    String getFrequency = "" + element.frequency; // Stores Frequency from element as String

                    // Sends multiple variables as argument to firebaseWrite method to check and, if relevant, send to Firebase

                    firebaseWrite(getSsid, getBssid, getRssi, getFrequency, mGpsLocation, mPhoneId);
                }
            }
        };

        // The code below was found at https://developer.android.com/guide/topics/connectivity/wifi-scan to be used with WifiManager above

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(mWifiScanReceiver, mIntentFilter);

    }

    /* A method that sends the User to their Firebase console to configure their settings and account.
    https://developer.android.com/guide/components/intents-common#ViewUrl was used as a guide and some code was used from there as well */

    public void fireConfig(View view) {
        Intent config = new Intent(Intent.ACTION_VIEW, Uri.parse("https://console.firebase.google.com/u/0"));
        if (config.resolveActivity(getPackageManager()) != null) {
            startActivity(config);
        }
    }

    /* The method below sets up Firebase and I used some example code from https://firebase.google.com/docs/android/setup
    & https://firebase.google.com/docs/database/android/start in the method below. */

    public void firebaseWrite(String getSsid, String getBssid, String getRssi, String getFrequency,
                              String mGpsLocation, String mPhoneId) {
        String message = "Access Point: " + j; // To print the number of the AP in Firebase and store as String
        String simpleApInfo = "SSID: " + getSsid + ", " + "BSSID: " + getBssid; // SSID & BSSID stored as String to compare later

        // All relevant information that needs to be sent to Firebase stored in this String

        String info = "SSID: " + getSsid + ", " + "BSSID: " + getBssid + ", " + "RSSI: " + getRssi + ", " +
                "Frequency: " + getFrequency + ", " + "Phone ID: " + mPhoneId + ", " + "GPS Location: " + mGpsLocation;

        // Used code from https://firebase.google.com/docs/database/android/start and as a guide to get started with Firebase

        FirebaseDatabase database = FirebaseDatabase.getInstance(); // Gets instance of database
        DatabaseReference wifiFireDb = database.getReference(message); // Location where information will be written to

        for (String element : mStoreAps) { // Checks each element in mStoreAps
            // If element is the same as simpleApInfo, then mCompareApElement returns true and the method returns. This is to stop duplicate entries in Firebase
            if (simpleApInfo.equals(element)) {
                mCompareApElement = true;
                return;
            }
        }

        /* Arrive here if mCompareApElement remains false. At this point, information is written to database using either If
         statement depending on length of mStoreAps.
          https://firebase.google.com/docs/database/android/read-and-write was used to learn about and implementing
          child nodes and setting values */

        if (mStoreAps.size() == 0) {
            mStoreAps.add(simpleApInfo); // Current AP is stored in ArrayList as a way of filtering out duplicate entries before writing to Firebase
            wifiFireDb.child("GPS Location: ").setValue(mGpsLocation); // Writes to Firebase database
            wifiFireDb.child("Phone ID: ").setValue(mPhoneId); // Writes to Firebase database
            wifiFireDb.child("Frequency: ").setValue(getFrequency); // Writes to Firebase database
            wifiFireDb.child("RSSI: ").setValue(getRssi); // Writes to Firebase database
            wifiFireDb.child("BSSID: ").setValue(getBssid); // Writes to Firebase database
            wifiFireDb.child("SSID: ").setValue(getSsid); // Writes to Firebase database
            i++; // Keeps a count of the number of Wifi APs available
            j++; // Increases Access Point Number
        }
        else {
            mStoreAps.add(simpleApInfo); // Current AP is stored in ArrayList as a way of filtering out duplicate entries before writing to Firebase
            wifiFireDb.child("GPS Location: ").setValue(mGpsLocation); // Writes to Firebase database
            wifiFireDb.child("Phone ID: ").setValue(mPhoneId); // Writes to Firebase database
            wifiFireDb.child("Frequency: ").setValue(getFrequency); // Writes to Firebase database
            wifiFireDb.child("RSSI: ").setValue(getRssi); // Writes to Firebase database
            wifiFireDb.child("BSSID: ").setValue(getBssid); // Writes to Firebase database
            wifiFireDb.child("SSID: ").setValue(getSsid); // Writes to Firebase database
            i++; // Keeps a count of the number of Wifi APs available
            j++; // Increases Access Point Number in Firebase
            mApTextView.setText("Number of APs available: " + i);
        }
    }

    /* The method below uses the EditText field in activity_main.xml file to extract the frequency of logging entered by the User
    and to use that to modify the frequency of the scan in the OnCreate method above. I used https://developer.android.com/reference/android/widget/EditText
     as a guide and for some */

    public void freqLog(View view) {
        EditText editConf = (EditText) findViewById(R.id.freq_log_button); // Ran out of time to incorporate frequency of logging
    }

}
