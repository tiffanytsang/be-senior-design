package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

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

import org.json.*;


public class HomeScreen extends Activity implements MqttCallback {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2tud3mf4dwlbh-ats.iot.us-east-1.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-1:3e5129b9-aca3-440a-a0ba-ca0a31177599";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;

    TextView textView_pReading;
    TextView textView_hrReading;
    AWSIotMqttManager mqttManager;
    String clientId;
    CognitoCachingCredentialsProvider credentialsProvider;

    ArrayList<Integer> minHeartRate = new ArrayList<>();
    ArrayList<Integer> maxHeartRate = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        textView_pReading = (TextView) findViewById(R.id.textView_pReading);
        textView_hrReading = (TextView) findViewById(R.id.textView_hrReading);

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
                                                    String calibratedReading = new String("" + calibrateRawPressureReading(message));
                                                    Log.d(LOG_TAG, " Calibrated Pressure: " + calibratedReading);
                                                    textView_pReading.setText(calibratedReading);
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

    public int calibrateRawPressureReading(String rawReading) {
        int pad_value = Integer.parseInt(rawReading);
        double v_diff = 3.3;

        //TODO: accept user input
        double radius = 1.59;
        double thickness = .002;
        double epsilon = .2;

        double strain = pad_value/v_diff;
        return (int)(thickness / radius * strain / epsilon);
    }

    public void alertUser(String reason, final int range) {
        final Activity activity = this;
        if (reason.equals("heart rate")) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText( activity,
                            "Your HR has changed by "+ range +"!! Adjust your compression :))))",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
        if (reason.equals("pressure") && range == 1) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText( activity,
                            "Your pressure is above your threshold!! Decrease your compression :))))",
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
        if (reason.equals("pressure") && range == -1) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText( activity,
                            "Your pressure is below your threshold!! Increase your compression :))))",
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
            int max = 0;
            for(int i = 0; i < json.length(); i++) {
                int hr = json.getJSONObject(i).getJSONObject("values").getJSONObject("ICvW4uBdSl_HRM").getInt("hrm");
                averageHr += hr;
                if (min > hr) {
                    min = hr;
                }
                if (max < hr) {
                    max = hr;
                }
            }

            averageHr = (int)(averageHr/json.length());
            Log.d(LOG_TAG, "Average heart rate: " + averageHr);
            minHeartRate.add(min);
            maxHeartRate.add(max);

            int minOfMin = Integer.MAX_VALUE;
            int maxOfMax = 0;

            for (int i = 0; i < minHeartRate.size(); i++) {
                if (minOfMin > minHeartRate.get(i)) {
                    minOfMin = minHeartRate.get(i);
                }
                if (maxOfMax < minHeartRate.get(i)) {
                    maxOfMax = minHeartRate.get(i);
                }
            }

            int range = maxOfMax - minOfMin;
            if (range >= 30) {
                alertUser("heart rate", range);
            }

            if (minHeartRate.size() > 3) {
                minHeartRate.remove(0);
                maxHeartRate.remove(0);
            }
            textView_hrReading.setText("" + averageHr);

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(LOG_TAG, "deliveryComplete");
    }
}
