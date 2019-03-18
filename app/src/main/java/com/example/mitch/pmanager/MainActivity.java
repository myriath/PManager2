package com.example.mitch.pmanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {

    String filename;
    String password;
    File out;
    ArrayList<PasswordEntry> fileData;
    String[] splitFile;
    public static final int REQUEST_WRITE_STORAGE = 0;
    public static final int REQUEST_READ_STORAGE = 1;
    private boolean hasWritePerm = false;
    private boolean hasReadPerm = false;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasWritePerm = true;
                }
                return;
            }
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasReadPerm = true;
                }
                return;
            }
        }
    }

    public void checkReadPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
        } else {
            hasReadPerm = true;
        }
    }

    public void checkWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            }
        } else {
            hasWritePerm = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
    }

    public void openFile(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText fn = findViewById(R.id.filenameField);
        EditText pd = findViewById(R.id.passwordField);
        filename = fn.getText().toString() + ".jpweds";
        password = pd.getText().toString();
        checkWritePermission();
        File root = new File(this.getFilesDir(), "PManager");
        if (!root.exists()) {
            if (hasWritePerm) {
                root.mkdirs();
            }
        }
        out = new File(root, filename);
        boolean exists = out.exists();
        try {
            if (exists) {
                checkReadPermission();
                if (hasReadPerm) {
                    LibraryFile f = new LibraryFile(out);
                    AES decrypt;
                    String decrypted;
                    try {
                        decrypt = new AES(AES.pad(password));
                        decrypted = decrypt.decrypt(out);
                        splitFile = decrypted.split(System.lineSeparator());
                        fileData = f.read(password);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                        e.printStackTrace();
                    }
                }
                if (splitFile[0].equals(filename)) {
                    intent.putExtra("file", out);
                    intent.putExtra("filename", filename);
                    intent.putExtra("password", password);
                    intent.putExtra("filedata", fileData);
                    startActivity(intent);
                } else {
                    fn.setText("Wrong Password!");
                }
            } else {
                checkWritePermission();
                if (hasWritePerm) {
                    AES newFile = new AES(AES.pad(password));
                    newFile.encryptString(filename, out);
                }
                fn.setText("Re-type filename");
            }
        } catch (Exception e1) {
            fn.setText("Wrong Password!");
            e1.printStackTrace();
        }
    }
}
