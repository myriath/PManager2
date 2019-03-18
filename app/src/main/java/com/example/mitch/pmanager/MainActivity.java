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
        // TODO Mitch, might be cleaner like this:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_WRITE_STORAGE: {
                    hasWritePerm = true;
                    return;
                }
                case REQUEST_READ_STORAGE: {
                    hasReadPerm = true;
                    return;
                }
            }
        }
    }

    // TODO this seems to only be called from this class, so it should be private
    public void checkReadPermission() {
        // TODO Mitch, put cursor on READ_EXTERNAL_STORAGE and press alt-return, choose "add static import" - it will import and shorten your code a bit
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
        // TODO also, it might be easier to understand like this:
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            hasReadPerm = true;
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
        }
    }

    // TODO this seems to only be called from this class, so it should be private
    public void checkWritePermission() {
        // TODO same comments here
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
        // TODO should make this if (!root.exists() && hasWritePerm)
        if (!root.exists()) {
            if (hasWritePerm) {
                root.mkdirs();
            }
        }
        out = new File(root, filename);
        boolean exists = out.exists();
        try {
            if (exists) {
                // TODO if you are always going to call checkReadPermission before doing if(hasReadPerm), maybe checkReadPermission should return boolean?
                checkReadPermission();
                if (hasReadPerm) {
                    LibraryFile f = new LibraryFile(out);
                    // TODO these 2 variables should be inside the try, and just assigned when defined, since they aren't used for anything else
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
                    // TODO what would you think about putting a PasswordEntry into the intent, instead of the separate fields?
                    intent.putExtra("file", out);
                    intent.putExtra("filename", filename);
                    intent.putExtra("password", password);
                    intent.putExtra("filedata", fileData);
                    startActivity(intent);
                } else {
                    fn.setText("Wrong Password!");
                }
            } else {
                // TODO same comment about these
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
