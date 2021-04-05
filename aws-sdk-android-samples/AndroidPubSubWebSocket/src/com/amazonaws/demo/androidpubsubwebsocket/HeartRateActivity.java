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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
    AWSIotMqttManager mqttManager;
    String clientId;
    CognitoCachingCredentialsProvider credentialsProvider;

    ArrayList<Integer> minHeartRate = new ArrayList<>();
    ArrayList<Integer> maxHeartRate = new ArrayList<>();

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
        JSONArray json = new JSONArray(payload);
        try {
            int averageHr = 0;
            int min = Integer.MAX_VALUE;
            int maxFromPastMinute = 0;
            for(int i = 0; i < json.length(); i++) {
                int hr = json.getJSONObject(i).getJSONObject("values").getJSONObject("ICvW4uBdSl_HRM").getInt("hrm");
                averageHr += hr;
                if (min > hr) {
                    min = hr;
                }
                if (maxFromPastMinute < hr) {
                    maxFromPastMinute = hr;
                }
            }

            averageHr = (int)(averageHr/json.length());
            Log.d(LOG_TAG, "Average heart rate: " + averageHr);
            minHeartRate.add(min);

            int minOfMin = Integer.MAX_VALUE;

            for (int i = 0; i < minHeartRate.size(); i++) {
                if (minOfMin > minHeartRate.get(i)) {
                    minOfMin = minHeartRate.get(i);
                }
            }

            //max from the past minute, min from the past 10 minutes
            int range = maxFromPastMinute - minOfMin;
            if (range >= 30) {
                alertUser("heart rate", range);
            }
            textView_hrReading.setText("" + averageHr);

            //likelihood of POTS attack (same screen as HR)
            //HR increase --> percent
            // 0 --> 0
            // map out the rest
            // above 60% = alert
            // 30 --> 70
            // 50 --> 100
            textView_percentage.setText("" + range * 2);

            if (minHeartRate.size() > 10) {
                minHeartRate.remove(0);
                maxHeartRate.remove(0);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(LOG_TAG, "deliveryComplete");
    }

    public void back(View view) {
        Intent intent = new Intent(HeartRateActivity.this, HomeScreenActivity.class);
        startActivity(intent);
    }
}
