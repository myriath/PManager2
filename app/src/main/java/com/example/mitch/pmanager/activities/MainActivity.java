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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.exceptions.DecryptionException;
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

    String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    String outPath = downloadsFolder + "/PManagerOut/";
    String inPath = downloadsFolder + "/PManagerIn/";

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            toast("Please Allow", this);
            finish();
        } else {
            if (requestCode == REQUEST_READ_STORAGE) {
                Perm permB = new Perm(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_STORAGE);
                checkPermission(permB);
            }
        }
    }

    private void checkPermission(Perm permission) {
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
                    EditText pd = dialogLayout.findViewById(R.id.dialog_new_password);
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

    public void changePassword(View view) {
        String[] strs = setupOpenFile();
        final String filename = strs[0];
        final Context self = this;
        try {
            decryptFile(out, getPassword(), filename);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Change Password");
            @SuppressLint("InflateParams")
            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
            builder.setView(dialogLayout);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    EditText pd = dialogLayout.findViewById(R.id.dialog_new_password);
                    String pwd = pd.getText().toString();
                    try {
                        updatePassword(filename, pwd, out);
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

    public void updatePassword(String filename, String newPassword, File file) {
        ArrayList<String> dat = new ArrayList<>();
        dat.add(filename);
        for (PasswordEntry entry : fileData) {
            dat.add(entry.domain);
            dat.add(entry.username);
            dat.add(entry.password);
        }
        StringBuilder sb = new StringBuilder();
        for (String str : dat) {
            sb.append(str);
            sb.append('\0');
        }
        try {
            AES f = new AES(AES.pad(newPassword));
            f.encryptString(sb.toString(), file);
            if (!f.decrypt(file).split(System.lineSeparator())[0].equals(filename)) {
                updatePassword(filename, newPassword, file);
            }
            Toast.makeText(this, "Saved",
                    Toast.LENGTH_LONG).show();
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            Toast.makeText(this, "Warning: File not Saved!",
                    Toast.LENGTH_LONG).show();
            e1.printStackTrace();
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

        try {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);
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
    }

    public void exportFile(View view) {
        String file = getFilename();
        File input = new File(getRoot(), file);
        String sourcePath = input.getAbsolutePath();
        String destinationPath = outPath + file;

        File source = new File(sourcePath);
        File destination = new File(destinationPath);

        try {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);
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
    }

    public void openFile(View view) {
        String[] strs = setupOpenFile();
        String filename = strs[0];
        String password = strs[1];
        resetFields();
        try {
            String message;
            if (out.exists()) {
                decryptFile(out, password, filename);
                message = "Opened!";
            } else {
                AES newFile = new AES(AES.pad(password));
                newFile.encryptString(filename, out);
                decryptFile(out, password, filename);
                message = "New File Created!";
            }
            toast(message, this);
            fileData = read(password, out);

            Intent intent = new Intent(this, MainScreenActivity.class);
            intent.putExtra("file", out);
            intent.putExtra("filename", filename);
            intent.putExtra("password", password);
            intent.putExtra("filedata", fileData);

            startActivity(intent);
        } catch (Exception e1) {
            toast("Wrong Password!", this);
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

    public static ArrayList<PasswordEntry> read(String key, File out) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        ArrayList<PasswordEntry> entries = new ArrayList<>();
        AES aes = new AES(AES.pad(key));
        String decrypted = aes.decrypt(out);
        String[] dataList = decrypted.split(System.lineSeparator());
        String[] entry = new String[3];
        int i2 = 0;
        int check1 = (dataList.length-1) / 3;
        while(i2 < check1) {
            int check0 = i2 * 3 + 1;
            System.arraycopy(dataList, check0, entry, 0, 3);
            i2++;
            entries.add(new PasswordEntry(entry[0], entry[1], entry[2], i2));
        }
        return entries;
    }
}

