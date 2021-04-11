package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONArray;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class HeartRateActivity extends Activity implements MqttCallback {
    static final String LOG_TAG = HeartRateActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2tud3mf4dwlbh-ats.iot.us-east-1.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:3e5129b9-aca3-440a-a0ba-ca0a31177599";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;

    TextView textView_hrReading;
    TextView textView_percentage;
    String clientId;
    CognitoCachingCredentialsProvider credentialsProvider;

    ArrayList<Date> timestampTesting = new ArrayList<>();
    ArrayList<Integer> averageHeartRateTesting = new ArrayList<>();
    ArrayList<Integer> likelihoodTesting = new ArrayList<>();
    ArrayList<Integer> minHeartRate = new ArrayList<>();
    ArrayList<Integer> maxFromPastMinute = new ArrayList<>();
    ArrayList<Integer> likelihoodArray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heartrate);

        textView_hrReading = (TextView) findViewById(R.id.textView_hrReading);
        textView_percentage = (TextView) findViewById(R.id.textView_percentage);

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

        //HR
        try {
            MqttClient client = new MqttClient("ssl://broker.emqx.io:8883", "", new MemoryPersistence());
            client.setCallback(this);
            client.connect();
            String topic = "pots/topic/hrm";
            client.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void alertUser(String reason, final int range) {
        final Activity activity = this;
        if (reason.equals("heart rate")) {
            runOnUiThread(new Runnable() {
                public void run() {
                final Toast toast = Toast.makeText( activity,
                        "We have detected a potential POTS attack. Your HR has increased by "+ range +"!! Increase your compression to 30 mmHg.",
                        Toast.LENGTH_LONG);
                toast.show();
                }
            });
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(LOG_TAG, "connectionLost " + cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        JSONArray json = new JSONArray(payload); // data for 6 seconds
        //  Log.d(LOG_TAG, "Heart rate json: " + json);
        try {
            int averageHr = 0; // average for the last 6 seconds
            int minFromPast6Seconds = Integer.MAX_VALUE;
            int maxFromPast6Seconds = 0;

            for(int i = 0; i < json.length(); i++) {
                int hr = json.getJSONObject(i).getJSONObject("values").getJSONObject("ICvW4uBdSl_HRM").getInt("hrm");
                averageHr += hr;
                if (minFromPast6Seconds > hr) {
                    minFromPast6Seconds = hr;
                }
                if (maxFromPast6Seconds < hr) {
                    maxFromPast6Seconds = hr;
                }
            }

            Timestamp ts=new Timestamp(Long.parseLong(json.getJSONObject(0).getString("ts")));
            Date date=new Date(ts.getTime());
            timestampTesting.add(date);
            averageHr = (int)(averageHr/json.length()); // average for the last 6 seconds
            averageHeartRateTesting.add(averageHr);
            //  Log.d(LOG_TAG, "Average heart rate: " + averageHr);
            minHeartRate.add(minFromPast6Seconds); // at its max will have 100 values, 10 values for minute
            maxFromPastMinute.add(maxFromPast6Seconds);

            int minOfMin = Integer.MAX_VALUE; // min from the past 10 minutes
            for (int i = 0; i < minHeartRate.size(); i++) {
                if (minOfMin > minHeartRate.get(i)) {
                    minOfMin = minHeartRate.get(i);
                }
            }

            int maxOfMax = 0; // max from the past minute
            for (int i = 0; i < maxFromPastMinute.size(); i++) {
                if (maxOfMax < maxFromPastMinute.get(i)) {
                    maxOfMax = maxFromPastMinute.get(i);
                }
            }

            // Max from the past minute, min from the past 10 minutes
            int range = maxOfMax - minOfMin;
            if (range >= 30) {
                alertUser("heart rate", range);
            }
            textView_hrReading.setText("" + averageHr);

            // Likelihood of POTS attack
            int likelihood = range * 2 - 10;
            if (likelihood < 0) {
                likelihood = 0;
            }
            if (likelihood > 100) {
                likelihood = 100;
            }
            textView_percentage.setText("" + likelihood);
            likelihoodArray.add(likelihood); // values from the past 10 minutes
            likelihoodTesting.add(likelihood);

            if (minHeartRate.size() > 10*10) {
                for (int i = 0; i < 10; i++) {
                    minHeartRate.remove(0);
                    likelihoodArray.remove(0);
                }
            }

            if (maxFromPastMinute.size() > 10) {
                maxFromPastMinute.remove(0); // remove last 6 seconds
            }

            StringBuilder sb = new StringBuilder();
            sb.append(date.toString());
            sb.append(',');
            sb.append(averageHr);
            sb.append(',');
            sb.append(likelihood);
            sb.append('\n');
            Log.d(LOG_TAG, sb.toString());

            if (likelihoodTesting.size() > 200) { // 200
                convertToCSV();
                timestampTesting = new ArrayList<>();
                averageHeartRateTesting = new ArrayList<>();
                likelihoodTesting = new ArrayList<>();
            }

            //average HR (every 6 seconds), min (every 6 seconds), likelihood (every 6 seconds)
            //10 minutes overall?

            // timestamp, average HR, likelihood, recorded for 20 minutes
            // recent on the bottom
            // excel

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public void convertToCSV() {
        Log.d(LOG_TAG, "Creating CSV now.");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("timestamp");
            sb.append(',');
            sb.append("average hr");
            sb.append(',');
            sb.append("likelihood");
            sb.append('\n');


            for (int i = 0; i < averageHeartRateTesting.size(); i++) {
                sb.append(timestampTesting.get(i).toString());
                sb.append(',');
                sb.append(averageHeartRateTesting.get(i).toString());
                sb.append(',');
                sb.append(likelihoodTesting.get(i).toString());
                sb.append('\n');
            }
            Log.d(LOG_TAG, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(LOG_TAG, "deliveryComplete");
    }

    public void back(View view) {
        Intent intent = new Intent(HeartRateActivity.this, HomeScreenActivity.class);
        startActivity(intent);
    }

    public void explain(View view) {
        Intent intent = new Intent(HeartRateActivity.this, ExplainActivity.class);
        startActivity(intent);
    }
}
