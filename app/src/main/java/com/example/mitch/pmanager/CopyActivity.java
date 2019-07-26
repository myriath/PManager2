package com.example.mitch.pmanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CopyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy);
    }

    public void copyUsernameButton(View view) {
        copyAndToast("0", "Copied Username #");
    }

    public void copyPasswordButton(View view) {
        copyAndToast("1", "Copied Password #");
    }

    private void copyAndToast(String operation, String message) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText d = findViewById(R.id.copyIndexField);
        String index = d.getText().toString();
        intent.putExtra("copy", index);
        intent.putExtra("operation", operation);
        setResult(RESULT_OK, intent);
        Toast.makeText(this, message + index,
                Toast.LENGTH_LONG).show();
        finish();
    }
}
