package com.example.mitch.pmanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    String genericText;
    public static final int REQUEST_WRITE_STORAGE = 0;
    public static final int REQUEST_READ_STORAGE = 1;

    String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    String outPath = downloadsFolder + "/PManagerOut/";
    String inPath = downloadsFolder + "/PManagerIn/";

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("big mcthankies");
                } else {
                    Toast.makeText(this, "Please allow", Toast.LENGTH_LONG).show();
                    checkWritePermission();
                }
                return;
            }
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("big mcthankies");
                } else {
                    Toast.makeText(this, "Please allow", Toast.LENGTH_LONG).show();
                    checkReadPermission();
                }
            }
        }
    }

    public void checkReadPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
        }
    }

    public void checkWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        checkReadPermission();
        checkWritePermission();

    }

    public void deleteFile(View view) {
        EditText fn = findViewById(R.id.filenameField);
        String file = fn.getText().toString() + ".jpweds";
        File input = new File(getRoot(), file);
        if (input.delete()) {
            System.out.println("deleted");
            Toast.makeText(this, "File Deleted",
                    Toast.LENGTH_LONG).show();
        } else {
            System.out.println("nodelete");
            Toast.makeText(this, "Warning: File not Deleted!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void createDirs(View view) {
        File inDir = new File(inPath);
        File outDir = new File(outPath);
        try {
            if (outDir.mkdirs()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Directory is not created");
            }
            if (inDir.mkdirs()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Directory is not created");
            }
            Toast.makeText(this, "Folders Created",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Warning: Folders not Created!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void importFile(View view) {
        EditText fn = findViewById(R.id.filenameField);
        String file = fn.getText().toString() + ".jpweds";
        File input = new File(getRoot(), file);
        String sourcePath = inPath + file;
        String destinationPath = input.getAbsolutePath();

        File source = new File(sourcePath);
        File destination = new File(destinationPath);

        try (InputStream in = new FileInputStream(source)) {
            try (OutputStream out = new FileOutputStream(destination)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Toast.makeText(this, "File Imported",
                        Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Warning: File not Imported!",
                        Toast.LENGTH_LONG).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Warning: File not Imported!",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Warning: File not Imported!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void exportFile(View view) {
        EditText fn = findViewById(R.id.filenameField);
        String file = fn.getText().toString() + ".jpweds";
        File input = new File(getRoot(), file);
        String sourcePath = input.getAbsolutePath();
        String destinationPath = outPath + file;

        File source = new File(sourcePath);
        File destination = new File(destinationPath);

        try (InputStream in = new FileInputStream(source)) {
            try (OutputStream out = new FileOutputStream(destination)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Toast.makeText(this, "File Exported",
                        Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Warning: File not Exported!",
                        Toast.LENGTH_LONG).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Warning: File not Exported!",
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Warning: File not Exported!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void openGeneric(View view) {
        Intent intent = new Intent(this, EditGenericFileActivity.class);
        EditText fn = findViewById(R.id.filenameField);
        EditText pd = findViewById(R.id.passwordField);
        filename = fn.getText().toString() + ".jpweds";
        password = pd.getText().toString();
        out = new File(getRoot(), filename);
        boolean exists = out.exists();
        try {
            if (exists) {
                AES decrypt;
                StringBuilder decrypted;
                try {
                    decrypt = new AES(AES.pad(password));
                    decrypted = new StringBuilder();
                    String text = decrypt.decrypt(out);
                    splitFile = text.split(System.lineSeparator());
                    for (String s : splitFile) {
                        if (!s.equals(splitFile[0])) {
                            decrypted.append(s);
                            if (!s.equals(splitFile[splitFile.length - 1])) {
                                decrypted.append(System.lineSeparator());
                            }
                        }
                    }
                    genericText = decrypted.toString();
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                if (splitFile[0].equals(filename)) {
                    intent.putExtra("file", out);
                    intent.putExtra("filename", filename);
                    intent.putExtra("filenamenoextension", fn.getText().toString());
                    intent.putExtra("password", password);
                    intent.putExtra("filedata", genericText);
                    Toast.makeText(this, "Opened",
                            Toast.LENGTH_LONG).show();
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                AES newFile = new AES(AES.pad(password));
                newFile.encryptString(filename, out);
                Toast.makeText(this, "New File Created",
                        Toast.LENGTH_LONG).show();
                AES decrypt;
                StringBuilder decrypted;
                try {
                    decrypt = new AES(AES.pad(password));
                    decrypted = new StringBuilder(decrypt.decrypt(out));
                    splitFile = decrypted.toString().split(System.lineSeparator());
                    for (String s : splitFile) {
                        if (!s.equals(splitFile[0])) {
                            decrypted.append(s);
                        }
                    }
                    genericText = decrypted.toString();
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                if (splitFile[0].equals(filename)) {
                    intent.putExtra("file", out);
                    intent.putExtra("filename", filename);
                    intent.putExtra("password", password);
                    intent.putExtra("filedata", genericText);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e1) {
            Toast.makeText(this, "Wrong Password!",
                    Toast.LENGTH_LONG).show();
            e1.printStackTrace();
        }
    }

    @NonNull
    private File getRoot() {
        File root = new File(this.getFilesDir(), "PManager");
        if (!root.exists()) {
            if (root.mkdirs()) {
                System.out.println("yeet");
            }
        }
        return root;
    }

    public void openFile(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        EditText fn = findViewById(R.id.filenameField);
        EditText pd = findViewById(R.id.passwordField);
        filename = fn.getText().toString() + ".jpweds";
        password = pd.getText().toString();
        out = new File(getRoot(), filename);
        boolean exists = out.exists();
        try {
            if (exists) {
                LibraryFile f = new LibraryFile(out);
                AES decrypt;
                String decrypted;
                try {
                    decrypt = new AES(AES.pad(password));
                    decrypted = decrypt.decrypt(out);
                    splitFile = decrypted.split(System.lineSeparator());
                    fileData = f.read(password);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                if (splitFile[0].equals(filename)) {
                    intent.putExtra("file", out);
                    intent.putExtra("filename", filename);
                    intent.putExtra("password", password);
                    intent.putExtra("filedata", fileData);
                    Toast.makeText(this, "Opened",
                            Toast.LENGTH_LONG).show();
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                AES newFile = new AES(AES.pad(password));
                newFile.encryptString(filename, out);
                Toast.makeText(this, "New File Created",
                        Toast.LENGTH_LONG).show();
                LibraryFile f = new LibraryFile(out);
                AES decrypt;
                String decrypted;
                try {
                    decrypt = new AES(AES.pad(password));
                    decrypted = decrypt.decrypt(out);
                    splitFile = decrypted.split(System.lineSeparator());
                    fileData = f.read(password);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                if (splitFile[0].equals(filename)) {
                    intent.putExtra("file", out);
                    intent.putExtra("filename", filename);
                    intent.putExtra("password", password);
                    intent.putExtra("filedata", fileData);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Wrong Password!",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e1) {
            Toast.makeText(this, "Wrong Password!",
                    Toast.LENGTH_LONG).show();
            e1.printStackTrace();
        }
    }
}
