package com.example.sephiros.wifilogger;

import android.content.Intent;
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

    // This method will be used when the start/stop logging button is pressed.
    // Used https://developer.android.com/reference/android/widget/Button as a reference

    public void startLog(View view) {

        final Button logButton = findViewById(R.id.logging_button); // Create Button object using the logging_button state attributes
        String logButtonStatus = logButton.getText().toString(); // Returns what text the logButton object has when the button is clicked, converts to a string and then stores in logButtonStatus
        if (logButtonStatus.equals(startLog)) { // Checks if the text in logButtonStatus equals "Start Logging" and, if it is, changes the text to "Stop Logging" for when the user wants to stop
            logButton.setText(stopLog);
        } else { // If the text in logButtonStatus is not "Start Logging", then the text is changed to the string in startLog.
            logButton.setText(startLog);
        }

    }

    // This method will be used when the Settings button is pressed.

    public void checkSettings(View view) {

        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);

    }
    
}
