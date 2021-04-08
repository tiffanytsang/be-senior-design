package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

public class PressureActivity extends Activity {
    TextView textView_pReading1, textView_pReading2, textView_pReading3, textView_pReading4, textView_pReading5;
    AWSIotMqttManager mqttManager;
    static final String LOG_TAG = PressureActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2tud3mf4dwlbh-ats.iot.us-east-1.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:3e5129b9-aca3-440a-a0ba-ca0a31177599";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;

    String clientId;
    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        textView_pReading1 = (TextView) findViewById(R.id.textView_pReading1);
        textView_pReading2 = (TextView) findViewById(R.id.textView_pReading2);
        textView_pReading3 = (TextView) findViewById(R.id.textView_pReading3);
        textView_pReading4 = (TextView) findViewById(R.id.textView_pReading4);
        textView_pReading5 = (TextView) findViewById(R.id.textView_pReading5);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(final AWSIotMqttClientStatus status,
                                        final Throwable throwable) {
                Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status == AWSIotMqttClientStatus.Connecting) {
                            Log.d(LOG_TAG, "Connecting... (AWS)");
                        } else if (status == AWSIotMqttClientStatus.Connected) {
                            Log.d(LOG_TAG, "Connected (AWS)");
                            final String topic = "home/helloworld";

                            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                                    new AWSIotMqttNewMessageCallback() {
                                        @Override
                                        public void onMessageArrived(final String topic, final byte[] data) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        String message = new String(data, "UTF-8");
                                                        ArrayList<Integer> calibratedPressureReadings = calibrateRawPressureReadings(message);
                                                        for (int i = 0; i < calibratedPressureReadings.size(); i++) {
                                                            if (calibratedPressureReadings.get(i) >= 35) {
                                                                alertUser("pressure", 1);
                                                            }
                                                            if (calibratedPressureReadings.get(i) <= 15) {
                                                                alertUser("pressure", 0);
                                                            }
                                                        }
                                                        Log.d(LOG_TAG, " Calibrated Pressures: " + calibratedPressureReadings);
                                                        textView_pReading1.setText(""+calibratedPressureReadings.get(0));
                                                        textView_pReading2.setText(""+calibratedPressureReadings.get(1));
                                                        textView_pReading3.setText(""+calibratedPressureReadings.get(2));
                                                        textView_pReading4.setText(""+calibratedPressureReadings.get(3));
                                                        textView_pReading5.setText(""+calibratedPressureReadings.get(4));
                                                    } catch (UnsupportedEncodingException e) {
                                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                                    }
                                                }
                                            });
                                        }
                                    });
                        } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error. (AWS)", throwable);
                            }
                            Log.d(LOG_TAG, "Reconnecting (AWS)");
                        } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error. (AWS)", throwable);
                                throwable.printStackTrace();
                            }
                            Log.d(LOG_TAG, "Disconnected (AWS)");
                        } else {
                            Log.d(LOG_TAG, "Disconnected (AWS)");
                        }
                    }
                });
            }
        });
    }

    public ArrayList<Integer> calibrateRawPressureReadings(String rawReading) {
        String [] array = rawReading.split(",");
        double pad_value1 = Double.parseDouble(array[0]);
        double pad_value2 = Double.parseDouble(array[1]);
        double pad_value3 = Double.parseDouble(array[2]);
        double pad_value4 = Double.parseDouble(array[3]);
        double pad_value5 = Double.parseDouble(array[4]);

        Bundle bundle = getIntent().getExtras();

        double circumference1 = bundle.getDouble("circumference1", 5.1);
        double circumference2 = bundle.getDouble("circumference2", 5.5);
        double circumference3 = bundle.getDouble("circumference3", 5.7);
        double circumference4 = bundle.getDouble("circumference4", 5.4);
        double circumference5 = bundle.getDouble("circumference5", 5.2);

        double radius1 = circumference1 / (2*Math.PI);
        double radius2 = circumference2 / (2*Math.PI);
        double radius3 = circumference3 / (2*Math.PI);
        double radius4 = circumference4 / (2*Math.PI);
        double radius5 = circumference5 / (2*Math.PI);

        double thickness = .002;
        double epsilon = 1.74*(10^6); // units: N/m^2
        double baseRes1 = 2*(10^3); // units: Ohms
        double baseRes2 = 2*(10^3);
        double baseRes3 = 2*(10^3);
        double baseRes4 = 2*(10^3);
        double baseRes5 = 2*(10^3);

        //formula
        double strain1 = pad_value1 * (10^3) / (baseRes1 * 1023);
        double strain2 = pad_value2 * (10^3) / (baseRes2 * 1023);
        double strain3 = pad_value3 * (10^3) / (baseRes3 * 1023);
        double strain4 = pad_value4 * (10^3) / (baseRes4 * 1023);
        double strain5 = pad_value5 * (10^3) / (baseRes5 * 1023);

        //for validation: convert to csv file
        ArrayList<Integer> calibratedPressureReadings = new ArrayList<>();
        calibratedPressureReadings.add((int)(thickness / radius1 * strain1 / epsilon));
        calibratedPressureReadings.add((int)(thickness / radius2 * strain2 / epsilon));
        calibratedPressureReadings.add((int)(thickness / radius3 * strain3 / epsilon));
        calibratedPressureReadings.add((int)(thickness / radius4 * strain4 / epsilon));
        calibratedPressureReadings.add((int)(thickness / radius5 * strain5 / epsilon));
        return calibratedPressureReadings;
    }

    public void alertUser(String reason, final int range) {
        final Activity activity = this;
        if (reason.equals("pressure") && range == 1) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText( activity,
                            "Your pressure is above your threshold! Decrease your compression.",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
        if (reason.equals("pressure") && range == -1) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText( activity,
                            "Your pressure is below your threshold!",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

    public void back(View view) {
        Intent intent = new Intent(PressureActivity.this, HomeScreenActivity.class);
        startActivity(intent);
    }
}
