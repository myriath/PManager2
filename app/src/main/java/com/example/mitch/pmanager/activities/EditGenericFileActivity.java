package com.example.mitch.pmanager.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.background.AES;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class EditGenericFileActivity extends AppCompatActivity {

    private static final String STATE_FILEDATA = "filedata";
    private static final String STATE_FILENAME = "filename";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_FILENAMENO = "filenamenoextension";
    private static final String STATE_FILE = "file";
    private File file;
    private String filename, password, filedata, name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_edit_generic_file);
        Bundle bd = getIntent().getExtras();
        if (bd != null) {
            file = (File) bd.get(STATE_FILE);
            filename = (String) bd.get(STATE_FILENAME);
            password = (String) bd.get(STATE_PASSWORD);
            filedata = (String) bd.get(STATE_FILEDATA);
            name = (String) bd.get(STATE_FILENAMENO);
        }
        TextView tv = findViewById(R.id.filenameView);
        tv.setText(name);

        EditText et = findViewById(R.id.textBody);
        et.setText(filedata);
    }

    public void saveFile(View view) {
        ArrayList<String> dat = new ArrayList<>();
        dat.add(filename);
        EditText et = findViewById(R.id.textBody);
        String text = et.getText().toString();
        dat.add(text);
        StringBuilder sb = new StringBuilder();
        for (String str : dat) {
            sb.append(str);
            sb.append('\0');
        }
        AES f;
        try {
            f = new AES(AES.pad(password));
            f.encryptString(sb.toString(), file);
            if (!f.decrypt(file).split(System.lineSeparator())[0].equals(filename)) {
                saveFile(view);
            }
            Toast.makeText(this, "Saved",
                    Toast.LENGTH_LONG).show();
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            Toast.makeText(this, "Warning: File not Saved!",
                    Toast.LENGTH_LONG).show();
            e1.printStackTrace();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        setResult(MainActivity.EXIT);
        finish();
    }
}
