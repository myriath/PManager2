package com.example.mitch.pmanager.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.exceptions.DecryptionException;
import com.example.mitch.pmanager.exceptions.DirectoryException;
import com.example.mitch.pmanager.background.DecryptionObject;
import com.example.mitch.pmanager.objects.PasswordEntry;
import com.example.mitch.pmanager.objects.Perm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {

    File out;
    ArrayList<PasswordEntry> fileData;
    public static final int REQUEST_WRITE_STORAGE = 0;
    public static final int REQUEST_READ_STORAGE = 1;
    public static final int EXIT = 2;

    String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    String outPath = downloadsFolder + "/PManagerOut/";
    String inPath = downloadsFolder + "/PManagerIn/";

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                            toast("Please allow", this);
                            finish();
                        }
                return;
            }
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Perm permB = new Perm(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_STORAGE);
                    checkPermission(permB);
                } else {
                    toast("Please allow", this);
                    finish();
                }
            }
        }
    }

    public void checkPermission(Perm permission) {
        if (ContextCompat.checkSelfPermission(this, permission.permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission.permission}, permission.code);
        }
    }

    private void checkPerms() {
        Perm permA = new Perm(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_STORAGE);
        checkPermission(permA);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);
        checkPerms();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resetFields();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        resetFields();
    }

    public void deleteFile(View view) {
        String[] strs = setupOpenFile();
        final String filename = strs[0];
        final Context self = this;
        try {
            decryptFile(out, getPassword(), filename);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Are you sure?");
            builder.setMessage("Re-Enter Password");
            @SuppressLint("InflateParams")
            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_delete_file, null);
            builder.setView(dialogLayout);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    EditText pd = dialogLayout.findViewById(R.id.dialogPassword);
                    String pwd = pd.getText().toString();
                    try {
                        decryptFile(out, pwd, filename);
                        if (out.delete()) {
                            toast("File Deleted", self);
                        } else {
                            toast("Warning: File not Deleted!", self);
                        }
                    } catch (Exception e1) {
                        toast("Wrong Password!", self);
                    }
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    toast("Cancelled", self);
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            resetFields();
        } catch (Exception e1) {
            toast("Wrong Password!", this);
        }
    }

    public void createDirs(View view) {
        File inDir = new File(inPath);
        File outDir = new File(outPath);
        try {
            if (outDir.mkdirs()) {
                System.out.println("Directory created");
            } else {
                throw new DirectoryException("Warning: Folders not Created!");
            }
            if (inDir.mkdirs()) {
                System.out.println("Directory created");
            } else {
                throw new DirectoryException("Warning: In Folder not Created!");
            }
            toast("Folders Created", this);
        } catch (DirectoryException e) {
            e.printStackTrace();
            toast(e.getMessage(), this);
        }
    }

    public void deleteDirs(View view) {
        File inDir = new File(inPath);
        File outDir = new File(outPath);
        try {
            if (outDir.delete()) {
                System.out.println("Directory deleted");
            } else {
                throw new DirectoryException("Warning: Folders not Deleted!");
            }
            if (inDir.delete()) {
                System.out.println("Directory deleted");
            } else {
                throw new DirectoryException("Warning: In Folder not Deleted!");
            }
            toast("Folders Deleted", this);
        } catch (DirectoryException e) {
            e.printStackTrace();
            toast(e.getMessage(), this);
        }
    }

    public void importFile(View view) {
        String file = getFilename();
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
                toast("File imported", this);
            } catch (IOException e) {
                e.printStackTrace();
                toast("Warning: File not imported!", this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            toast("Warning: File not imported!", this);
        }
    }

    public void exportFile(View view) {
        String file = getFilename();
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
                toast("File Exported", this);
            } catch (IOException e) {
                e.printStackTrace();
                toast("Warning: File not Exported!", this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            toast("Warning: File not Exported!", this);
        }
    }

    public void openFile(View view) {
        Intent intent = new Intent(this, MainScreenActivity.class);
        String[] strs = setupOpenFile();
        String filename = strs[0];
        String password = strs[1];
        resetFields();
        boolean exists = out.exists();
        try {
            DecryptionObject decryptionObject;
            String message;
            if (exists) {
                decryptionObject = new DecryptionObject();
                decryptFile(out, password, filename);
                message = "Opened!";
            } else {
                AES newFile = new AES(AES.pad(password));
                newFile.encryptString(filename, out);
                decryptionObject = new DecryptionObject();
                decryptFile(out, password, filename);
                message = "New File Created!";
            }
            doActivity(intent, filename, password, decryptionObject, message);
        } catch (Exception e1) {
            toast("Wrong Password!", this);
        }
    }

    private void doActivity(Intent intent, String filename, String password, DecryptionObject decryptionObject, String message)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        fileData = decryptionObject.read(password, out);
        setupIntent(intent, message, filename, password);
        startActivity(intent);
    }

    private void setupIntent(Intent intent, @Nullable String message, String filename, String password) {
        intent.putExtra("file", out);
        intent.putExtra("filename", filename);
        intent.putExtra("password", password);
        intent.putExtra("filedata", fileData);
        if (message != null) {
            toast(message, this);
        }
    }

    @NonNull
    private String getFilename() {
        EditText fn = findViewById(R.id.filenameField);
        return fn.getText().toString() + ".jpweds";
    }

    @NonNull
    private String getPassword() {
        EditText pd = findViewById(R.id.passwordField);
        return pd.getText().toString();
    }

    @NonNull
    private File getRoot() {
        File root = new File(this.getFilesDir(), "PManager");
        if (!root.exists()) {
            if (root.mkdirs()) {
                System.out.println("done");
            }
        }
        return root;
    }

    private void decryptFile(File file, String pwd, String name) throws DecryptionException {
        String data;
        try {
            String[] splitFile;
            AES decrypt = new AES(AES.pad(pwd));
            data = decrypt.decrypt(file);
            splitFile = data.split(System.lineSeparator());

            if (!splitFile[0].equals(name)) {
                throw new DecryptionException("");
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DecryptionException("");
        }
    }

    private String[] setupOpenFile() {
        String filename = getFilename();
        String password = getPassword();
        out = new File(getRoot(), filename);
        return new String[]{filename, password};
    }

    private void resetFields() {
        EditText fn = findViewById(R.id.filenameField);
        EditText pd = findViewById(R.id.passwordField);
        fn.getText().clear();
        pd.getText().clear();
    }

    public static void toast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}
