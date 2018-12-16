package com.example.sephiros.wifilogger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    String stopLog = "Stop Logging"; // Use this string for setting text on the Logging button
    String startLog = "Start Logging"; // Use this string for setting text on the Logging button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
}
