package com.amazonaws.demo.androidpubsubwebsocket;
//package paho.mqtt.java.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.io.UnsupportedEncodingException;
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
//import org.eclipse.paho.client.mqttv3.*;
//import org.eclipse.paho.android.service.MqttAndroidClient;


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
//        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
//        mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
//            @Override
//            public void onStatusChanged(final AWSIotMqttClientStatus status,
//                                        final Throwable throwable) {
//                Log.d(LOG_TAG, "Status = " + String.valueOf(status));
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (status == AWSIotMqttClientStatus.Connecting) {
//                            Log.d(LOG_TAG, "Connecting...");
////                                    tvStatus.setText("Connecting...");
//
//                        } else if (status == AWSIotMqttClientStatus.Connected) {
//                            Log.d(LOG_TAG, "Connected");
//
//                        } else if (status == AWSIotMqttClientStatus.Reconnecting) {
//                            if (throwable != null) {
//                                Log.e(LOG_TAG, "Connection error.", throwable);
//                            }
//                            Log.d(LOG_TAG, "Reconnecting");
////                                    tvStatus.setText("Reconnecting");
//                        } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
//                            if (throwable != null) {
//                                Log.e(LOG_TAG, "Connection error.", throwable);
//                                throwable.printStackTrace();
//                            }
//                            Log.d(LOG_TAG, "Disconnected");
////                                    tvStatus.setText("Disconnected");
//                        } else {
//                            Log.d(LOG_TAG, "Disconnected");
////                                    tvStatus.setText("Disconnected");
//                        }
//                    }
//                });
//            }
//        });
//
//        final String topic = "home/helloworld";
//        Log.d(LOG_TAG, "topic = " + topic);
//
//        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
//                new AWSIotMqttNewMessageCallback() {
//                    @Override
//                    public void onMessageArrived(final String topic, final byte[] data) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    String message = new String(data, "UTF-8");
//                                    Log.d(LOG_TAG, "Message arrived:");
//                                    Log.d(LOG_TAG, "   Topic: " + topic);
//                                    Log.d(LOG_TAG, " Message: " + message);
//
//                                    Log.d(LOG_TAG, "topic = " + topic);
//
//                                    textView_pReading.setText(message);
//
//                                } catch (UnsupportedEncodingException e) {
//                                    Log.e(LOG_TAG, "Message encoding error.", e);
//                                }
//                            }
//                        });
//                    }
//                });

    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(LOG_TAG, "connectionLost " + cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        JSONArray json = new JSONArray(payload);
        Log.d(LOG_TAG, "got a message");
        try {
            int hrm = json.getJSONObject(0).getJSONObject("values").getJSONObject("ICvW4uBdSl_HRM").getInt("hrm");
            Log.d(LOG_TAG, "" + hrm);
            textView_hrReading.setText("" + hrm);
        } catch (Exception e) {

        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(LOG_TAG, "deliveryComplete");

    }
}
