package com.amazonaws.demo.androidpubsubwebsocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ChangeParametersActivity extends Activity {

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changeparameters);

        intent = new Intent(ChangeParametersActivity.this, HomeScreenActivity.class);
    }

    public void changeParameters(View view) {
        EditText editText1 = (EditText) findViewById(R.id.editText1);
        EditText editText2 = (EditText) findViewById(R.id.editText2);
        EditText editText3 = (EditText) findViewById(R.id.editText3);
        EditText editText4 = (EditText) findViewById(R.id.editText4);
        EditText editText5 = (EditText) findViewById(R.id.editText5);

        intent.putExtra("pressure1", editText1.getText());
        intent.putExtra("pressure2", editText2.getText());
        intent.putExtra("pressure3", editText3.getText());
        intent.putExtra("pressure4", editText4.getText());
        intent.putExtra("pressure5", editText5.getText());

        final Activity activity = this;
        runOnUiThread(new Runnable() {
            public void run() {
            final Toast toast = Toast.makeText( activity,
                    "Parameters changed successfully!",
                    Toast.LENGTH_SHORT);
            toast.show();
            }
        });

        startActivity(intent);
    }

    public void back(View view) {
        startActivity(intent);
    }
}
