package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.Constants.EXTENSION_V2;
import static com.example.mitch.pmanager.Constants.EXTENSION_V3;
import static com.example.mitch.pmanager.Constants.STATE_FILE;
import static com.example.mitch.pmanager.Constants.STATE_FILEDATA;
import static com.example.mitch.pmanager.Constants.STATE_FILENAME;
import static com.example.mitch.pmanager.Constants.STATE_PASSWORD;
import static com.example.mitch.pmanager.Util.toBytes;

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
import com.example.mitch.pmanager.Util;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.background.Encryptor;
import com.example.mitch.pmanager.exceptions.DecryptionException;
import com.example.mitch.pmanager.exceptions.DirectoryException;
import com.example.mitch.pmanager.objects.PasswordEntry;
import com.example.mitch.pmanager.objects.Perm;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
        char[][] strs;
        final Context self = this;
        try {
            strs = setupOpenFile();
            final byte[] filename = new String(strs[0]).getBytes(StandardCharsets.UTF_8);

            Encryptor.EncryptedData encrypted = Encryptor.readFromFile(out);
            Encryptor.decrypt(encrypted, filename, strs[1]);

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
                    char[] pwd = new char[pd.length()];
                    pd.getText().getChars(0, pd.length(), pwd, 0);
                    try {
                        Encryptor.EncryptedData encrypted = Encryptor.readFromFile(out);
                        Encryptor.decrypt(encrypted, filename, pwd);
                        if (out.delete()) {
                            String plainName = out.getName().split("\\.")[0];
                            new File(getRoot(), plainName + EXTENSION_V2).delete();
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
        final Context self = this;
        try {
            char[][] strs = setupOpenFile();
            final String filename = new String(strs[0]);

            Encryptor.EncryptedData encrypted = Encryptor.readFromFile(out);
            Encryptor.decrypt(encrypted, filename.getBytes(StandardCharsets.UTF_8), strs[1]);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Change Password");
            @SuppressLint("InflateParams")
            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
            builder.setView(dialogLayout);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    EditText pd = dialogLayout.findViewById(R.id.dialog_new_password);
                    int pwdLength = pd.length();
                    char[] pwd = new char[pwdLength];
                    pd.getText().getChars(0, pwdLength, pwd, 0);
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

    public void updatePassword(String filename, char[] newPassword, File file) {
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
            sb.append(System.lineSeparator());
        }
        try {
            byte[] plaintext = sb.toString().getBytes(StandardCharsets.UTF_8);
            byte[] ad = filename.getBytes(StandardCharsets.UTF_8);
            Encryptor.EncryptedData encrypted = Encryptor.encrypt(plaintext, ad, newPassword);
            if (!new String(Encryptor.decrypt(encrypted, ad, newPassword)).split(System.lineSeparator())[0].equals(filename)) {
                updatePassword(filename, newPassword, file);
                return;
            }
            Encryptor.writeEncrypted(encrypted, file);

            Toast.makeText(this, "Saved",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e1) {
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
        String filename = getFilename();

        File source = new File(inPath + filename);
        if (!source.exists()) {
            filename = getOldFilename();
            source = new File(inPath + filename);
        }

        File destination = new File(getRoot(), filename);

        if (Util.copyFile(source.toPath(), destination.toPath())) {
            toast("File imported", this);
        } else {
            toast("Warning: File not imported!", this);
        }
    }

    public void exportFile(View view) {
        String file = getFilename();

        File source = new File(getRoot(), file);
        File destination = new File(outPath + file);

        if (Util.copyFile(source.toPath(), destination.toPath())) {
            toast("File Exported", this);
        } else {
            toast("Warning: File not Exported!", this);
        }
    }

    public void openFile(View view) {
        try {
            char[][] strs = setupOpenFile();
            byte[] filename = toBytes(strs[0]);
            char[] password = strs[1];
            resetFields();
            String message;
            byte[] decrypted;
            if (out.exists()) {
                Encryptor.EncryptedData encrypted = Encryptor.readFromFile(out);
                decrypted = Encryptor.decrypt(encrypted, filename, strs[1]);
                message = "Opened!";
            } else {
                Encryptor.EncryptedData data = Encryptor.encrypt(filename, filename, password);
                Encryptor.writeEncrypted(data, out);
                message = "New File Created!";
                decrypted = filename;
            }
            toast(message, this);
            fileData = parseData(decrypted);

            Intent intent = new Intent(this, MainScreenActivity.class);
            intent.putExtra(STATE_FILE, out);
            intent.putExtra(STATE_FILENAME, String.valueOf(strs[0]));
            intent.putExtra(STATE_PASSWORD, password);
            intent.putExtra(STATE_FILEDATA, fileData);

            startActivity(intent);
        } catch (Exception e1) {
            toast("Wrong Password!", this);
            resetFields();
        }
    }

    @NonNull
    private String getFilename() {
        EditText fn = findViewById(R.id.filenameField);
        return fn.getText().toString() + EXTENSION_V3;
    }

    @NonNull
    private String getOldFilename() {
        EditText fn = findViewById(R.id.filenameField);
        return fn.getText().toString() + EXTENSION_V2;
    }

    @NonNull
    private char[] getPassword() {
        EditText pd = findViewById(R.id.passwordField);
        int length = pd.length();
        char[] ret = new char[length];
        pd.getText().getChars(0, length, ret, 0);
        return ret;
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

    private char[][] setupOpenFile() throws Exception {
        String filename = getFilename();
        char[] password = getPassword();
        out = new File(getRoot(), filename);
        if (!out.exists()) {
            File old = new File(getRoot(), getOldFilename());
            if (old.exists()) {
                Util.translateV2toV3(old, out, password, getOldFilename(), filename);
            }
        }

        return new char[][]{filename.toCharArray(), password};
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

    public static ArrayList<PasswordEntry> parseData(byte[] data) {
        ArrayList<PasswordEntry> entries = new ArrayList<>();
        String decrypted = new String(data);

        String[] dataList = decrypted.split(System.lineSeparator());
        String[] entry = new String[3];
        int i = 0;
        int check1 = (dataList.length-1) / 3;
        while(i < check1) {
            int check0 = i * 3 + 1;
            System.arraycopy(dataList, check0, entry, 0, 3);
            i++;
            entries.add(new PasswordEntry(entry[0], entry[1], entry[2], i));
        }
        return entries;
    }

    public void createTestOldFile(View view) {
        File file = new File(getRoot(), "test.jpweds");
        File file2 = new File(getRoot(), "test.pm3");
        file.delete();
        file2.delete();

        ArrayList<String> dat = new ArrayList<>();
        dat.add("test.jpweds");
        for (int i = 0; i < 10; i++) {
            dat.add(String.format("Domain%d", i));
            dat.add(String.format("Username%d", i));
            dat.add(String.format("Password%d", i));
        }
        StringBuilder sb = new StringBuilder();
        for (String str : dat) {
            sb.append(str);
            sb.append('\0');
        }
        try {
            AES f = new AES(AES.pad("testpass"));
            f.encryptString(sb.toString(), file);
            if (!f.decrypt(file).split(System.lineSeparator())[0].equals("test.jpweds")) {
                createTestOldFile(view);
            }
            Toast.makeText(this, "Saved",
                    Toast.LENGTH_LONG).show();
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            Toast.makeText(this, "Warning: File not Saved!",
                    Toast.LENGTH_LONG).show();
            e1.printStackTrace();
        }
    }
}

