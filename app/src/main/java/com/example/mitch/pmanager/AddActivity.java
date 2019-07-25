package com.example.mitch.pmanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
    }

    public void addEntry(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText dm = findViewById(R.id.domainField);
        EditText us = findViewById(R.id.usernameField);
        EditText pw = findViewById(R.id.passwordField);
        String domain = dm.getText().toString();
        String username = us.getText().toString();
        String pass = pw.getText().toString();
        intent.putExtra("domain", domain);
        intent.putExtra("username", username);
        intent.putExtra("password", pass);
        setResult(RESULT_OK, intent);
        Toast.makeText(this, "Added Entry",
                Toast.LENGTH_LONG).show();
        finish();
    }
}
