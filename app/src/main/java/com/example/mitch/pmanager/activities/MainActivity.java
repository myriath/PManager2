package com.example.mitch.pmanager.activities;

import android.Manifest;
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
import com.example.mitch.pmanager.background.LibraryFile;
import com.example.mitch.pmanager.objects.DecryptionObject;
import com.example.mitch.pmanager.exceptions.DirectoryException;
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

    private boolean readPerm = false;
    private boolean writePerm = false;

    String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    String outPath = downloadsFolder + "/PManagerOut/";
    String inPath = downloadsFolder + "/PManagerIn/";

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // TODO: Make the app close if you say no
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writePerm = true;
                } else {
                    toast("Please allow", this);
                    writePerm = false;
                }
                return;
            }
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readPerm = true;
                } else {
                    toast("Please allow", this);
                    readPerm = false;
                }
            }
        }
    }

    public void checkPermission(Perm permission) {
        if (ContextCompat.checkSelfPermission(this, permission.permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission.permission}, permission.code);
        } else {
            switch (permission.permission) {
                case (Manifest.permission.READ_EXTERNAL_STORAGE): {
                    readPerm = true;
                    break;
                }
                case (Manifest.permission.WRITE_EXTERNAL_STORAGE): {
                    writePerm = true;
                    break;
                }
            }
        }
    }

    private void checkPerms() {
        Perm permA = new Perm(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_STORAGE);
        Perm permB = new Perm(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_STORAGE);
        checkPermission(permA);
        checkPermission(permB);
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
        checkPerms();
        String[] strs = setupOpenFile();
        final String filename = strs[0];
        final Context self = this;
        try {
            DecryptionObject decryptionObject = decryptFile(out, getPassword(), filename);
            if (decryptionObject.correctPassword) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Are you sure?");
                builder.setMessage("Re-Enter Password");
                final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_delete_file, null);
                builder.setView(dialogLayout);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText pd = dialogLayout.findViewById(R.id.dialogPassword);
                        String pwd = pd.getText().toString();
                        try {
                            DecryptionObject decryptionObject = decryptFile(out, pwd, filename);
                            if (decryptionObject.correctPassword) {
                                if (out.delete()) {
                                    toast("File Deleted", self);
                                } else {
                                    toast("Warning: File not Deleted!", self);
                                }
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
            }
        } catch (Exception e1) {
            toast("Wrong Password!", this);
        }
    }

    public void createDirs(View view) {
        checkPerms();
        if (readPerm && writePerm) {
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
    }

    public void deleteDirs(View view) {
        checkPerms();
        if (readPerm && writePerm) {
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
    }

    public void importFile(View view) {
        checkPerms();
        if (readPerm && writePerm) {
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
    }

    public void exportFile(View view) {
        checkPerms();
        if (readPerm && writePerm) {
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
    }

    public void openFile(View view) {
        checkPerms();
        if (readPerm && writePerm) {
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
                    decryptionObject = decryptFile(out, password, filename);
                    message = "Opened!";
                } else {
                    AES newFile = new AES(AES.pad(password));
                    newFile.encryptString(filename, out);
                    decryptionObject = decryptFile(out, password, filename);
                    message = "New File Created!";
                }
                doActivity(intent, filename, password, decryptionObject, message);
            } catch (Exception e1) {
                toast("Wrong Password!", this);
            }
        }
    }

    private void doActivity(Intent intent, String filename, String password, DecryptionObject decryptionObject, String message) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        if (decryptionObject.correctPassword) {
            LibraryFile f = new LibraryFile(out);
            fileData = f.read(password);
            setupIntent(intent, message, filename, password);
            startActivity(intent);
        }
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

    private DecryptionObject decryptFile(File file, String pwd, String name) {
        boolean correctPassword = false;
        String data = "";
        try {
            String[] splitFile;
            AES decrypt = new AES(AES.pad(pwd));
            data = decrypt.decrypt(file);
            splitFile = data.split(System.lineSeparator());

            if (splitFile[0].equals(name)) {
                correctPassword = true;
            } else {
                toast("Wrong Password!", this);
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            toast("Wrong Password!", this);
        }

        return new DecryptionObject(data, correctPassword, name);
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
