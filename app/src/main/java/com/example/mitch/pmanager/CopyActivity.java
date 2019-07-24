package com.example.mitch.pmanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class CopyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy);
    }

    public void copyUsernameButton(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText d = findViewById(R.id.copyIndexField);
        String index = d.getText().toString();
        intent.putExtra("copy", index);
        intent.putExtra("operation", "0");
        setResult(RESULT_OK, intent);
        finish();
    }

    public void copyPasswordButton(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText d = findViewById(R.id.copyIndexField);
        String index = d.getText().toString();
        intent.putExtra("copy", index);
        intent.putExtra("operation", "1");
        setResult(RESULT_OK, intent);
        finish();
    }
}
