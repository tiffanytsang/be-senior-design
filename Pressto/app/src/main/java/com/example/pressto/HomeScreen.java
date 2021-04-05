package com.example.pressto;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.SensorsClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class HomeScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
    }

    public void googleFit(View v) {
        GoogleSignInOptionsExtension fitnessOptions =
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        SensorsClient sensorsClient = Fitness.getSensorsClient(this, googleSignInAccount);

        if (!GoogleSignIn.hasPermissions(googleSignInAccount, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                1, // e.g. 1
                googleSignInAccount,
                fitnessOptions);
            Log.i("home screen", "permissions requested");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String [] {Manifest.permission.BODY_SENSORS},
                    1);
            Log.i("home screen", "permissions not granted before");
        }
        Log.i("home screen", googleSignInAccount.getEmail());

        sensorsClient
            .findDataSources(
                new DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build())
                .addOnSuccessListener(dataSources -> {
                    Log.i("find data source", "success");
                    for(int i = 0; i < dataSources.size(); i++) {
                        Log.i("data sources", dataSources.get(i).toString());
                    }
                } )
                .addOnFailureListener( e -> Log.e("data sources error", "Find data sources request failed", e));

        OnDataPointListener mListener = dataPoint -> {
            for (Field field : dataPoint.getDataType().getFields()) {
                Log.i("device", dataPoint.getDataSource().getDevice().toString());
                Value val = dataPoint.getValue(field);
//                    Value(TotalSteps);
//                     TotalSteps=val+TotalSteps;
                Log.i("main activity", "Detected DataPoint field: " + field.getName());
                Log.i("main activity", "Detected DataPoint value: " + val);
            }
        };

        sensorsClient
            .add(
                new SensorRequest.Builder()
                    .setDataType(DataType.TYPE_HEART_RATE_BPM) // TYPE_STEP_COUNT_DELTA
                    .setSamplingRate(1, TimeUnit.SECONDS)  // sample once per minute
                    .build(), mListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            if (1 == requestCode) {
//                createDriveFile();
                Log.i("permissions", "granted");
            }
        }
    }

    public void onLogoutButtonClick(View v) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut();
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}