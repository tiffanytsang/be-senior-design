package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ExplainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explain);
    }

    public void back(View view) {
        Intent intent = new Intent(ExplainActivity.this, HeartRateActivity.class);
        startActivity(intent);
    }
}
