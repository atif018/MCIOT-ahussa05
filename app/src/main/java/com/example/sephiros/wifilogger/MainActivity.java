package com.example.sephiros.wifilogger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WifiManager wifiScanner;
    private String stopLog = "Stop Logging"; // Use this string for setting text on the Logging button
    private String startLog = "Start Logging"; // Use this string for setting text on the Logging button
    TextView testView;
    IntentFilter intentFilter;
    List<ScanResult> listOfAps;
    BroadcastReceiver wifiScanReceiver;
    DatabaseReference myRef;

    // Used https://developer.android.com/guide/topics/connectivity/wifi-scan as a reference when
    // customising the onCreate method

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testView = (TextView) findViewById(R.id.scannedWifi);
        myRef = FirebaseDatabase.getInstance().getReference("message");


        wifiScanner = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //turnOffWifi();
        Button startButtonScan = (Button)findViewById(R.id.scanWifi_button);
        startButtonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiScanner.setWifiEnabled(true);
                wifiScanner.startScan();

            }
        });

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                listOfAps = wifiScanner.getScanResults();

                //for(int i = 0; i < listOfAps.size(); i++) {
                    String info = listOfAps.get(1).toString();
                    testView.setText(info);
                    myRef.push();
                    wLogger logger = new wLogger(info);
                    myRef.setValue(logger);

                //}
                /*if (listOfAps.size() > 0) {
                    Toast.makeText(getApplicationContext(), "We're populating the list", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "List is not being populated!!!!", Toast.LENGTH_SHORT).show();
                }
*/
                }
            };


        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);
        firebaseWrite();
    }

    /*private void turnOffWifi() {
        wifiScanner.setWifiEnabled(false);
    }

    // This method will be used when the start/stop logging button is pressed.
    // Used https://developer.android.com/reference/android/widget/Button as a reference


    // This method will be used when the Settings button is pressed.

    public void checkSettings(View view) {

        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);

    }*/

    public void firebaseWrite() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        myRef.setValue("Test run to database");
    }
}
