package com.example.mitch.pmanager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class FilterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
    }

    public void domainButton(View view) {
        filter("0");
    }

    public void usernameButton(View view) {
        filter("1");
    }

    public void passwordButton(View view) {
        filter("2");
    }

    private void filter(String operation) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText d = findViewById(R.id.filterField);
        String index = d.getText().toString();
        intent.putExtra("filter", index);
        intent.putExtra("operation", operation);
        setResult(RESULT_OK, intent);
        finish();
    }
}
