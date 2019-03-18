package com.example.mitch.pmanager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class DeleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);
    }

    public void deleteEntry(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText d = findViewById(R.id.deleteField);
        String index = d.getText().toString();
        intent.putExtra("id", index);
        setResult(RESULT_OK, intent);
        finish();
    }
}
