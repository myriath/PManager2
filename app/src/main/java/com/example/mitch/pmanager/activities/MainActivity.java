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
import com.example.mitch.pmanager.objects.PasswordEntry;

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


    String filename; // todo I don't think these need to be class level variables. You always seem to
    // todo call setupOpenFile before you use them, so they can just be stored as local variables in
    // todo those methods
    String password;
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
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
// todo It looks like in both cases the if part is the same. I suggest pulling the if part outside of
// todo the switch and do it once, inside the if you can return since you'll be done. That will simplify the switch.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("thanks");
                } else {
                    toast("Please allow", this);
                    checkWritePermission();
                }
                return;
            }
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("thanks");
                } else {
                    toast("Please allow", this);
                    checkReadPermission();
                }
            }
        }
    }

    // todo Without too much effort, I think you could simplify these two methods into one, passing in
// todo the request and the permission requested.
// todo Also, I think this could/should be private.
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);
        checkReadPermission();
        checkWritePermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resetFields();
    }

    @Override
    protected void onUserLeaveHint() {
        // todo When does this get called? I'm not sure you tested it, because it looks like the super call throws an exception.
        super.onUserLeaveHint();
        resetFields();
    }

    // todo Do these methods have to be public, or could they be private?
    public void deleteFile(View view) {
        setupOpenFile();
        DecryptionObject decryptionObject;
        // todo It's kind of funny how you're trying to change Java to be like Python. :-)
        final Context self = this;
        try {
            decryptionObject = decryptFile(out, getPassword(), filename);
            if (decryptionObject.correctPassword) { // todo It looks like if you decrypt this and it
                // todo doesn't have the right password, it won't toast "Wrong password".
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

                        DecryptionObject decryptionObject; // todo I think this could be inside the try and combined with assignment.
                        try {
                            decryptionObject = decryptFile(out, pwd, filename);
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
            toast("Folders Created", this);
        } catch (Exception e) {
            e.printStackTrace();
            toast("Warning: Folders not Created!", this);
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
        setupOpenFile();
        resetFields();
        boolean exists = out.exists();
        DecryptionObject decryptionObject; // todo only used inside the try
        try {
            if (exists) {
                decryptionObject = decryptFile(out, password, filename);
                if (decryptionObject.correctPassword) {
                    LibraryFile f = new LibraryFile(out);
                    fileData = f.read(password);
                    setupIntent(intent, "Opened!");
                    startActivity(intent);
                }
            } else {
                AES newFile = new AES(AES.pad(password));
                // todo newFile isn't used after you set it. Does it do anything? How does the new
                // todo filename get in there? If it isn't used, since filename is decrypted in both
                // todo the if and the else, you probably don't need to do the decrypt in both, you
                // todo could do it once before the if.
                newFile.encryptString(filename, out);
                decryptionObject = decryptFile(out, password, filename);
                if (decryptionObject.correctPassword) { // todo Also, this if is the same in both the outer if and else.
                    LibraryFile f = new LibraryFile(out);
                    fileData = f.read(password);
                    setupIntent(intent, "New File Created!");
                    startActivity(intent);
                }
            }
        } catch (Exception e1) {
            toast("Wrong Password!", this);
        }
    }

    private void setupIntent(Intent intent, @Nullable String message) {
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

    // todo I'm not sure name is needed as an argument here. It looks like you start the delete and open methods by
    // todo calling setupOpenFile, which gets the filename and puts it into out. But then whenever
    // todo you call this method, you pass in out and filename into this, so won't name here always
    // todo be the same as the filename inside file?
    private DecryptionObject decryptFile(File file, String pwd, String name) {
        String[] splitFile = {};
        boolean correctPassword = false;
        boolean error = false;
        String data = "";
        AES decrypt; // todo This isn't used outside of the try, so it should be defined inside it
        // todo and combined with the assignment.
        try {
            decrypt = new AES(AES.pad(pwd));
            data = decrypt.decrypt(file);
            splitFile = data.split(System.lineSeparator());
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            toast("Wrong Password!", this);
            error = true;
        }

        if (!error) { // todo I think the stuff in this if can be moved to the end of the try section,
            // todo since if there's an exception, it stops at that line and goes into the catch.
            if (splitFile[0].equals(name)) {
                correctPassword = true;
            } else {
                toast("Wrong Password!", this);
            }
        }

        return new DecryptionObject(data, correctPassword, filename);
    }

    private void setupOpenFile() {
        filename = getFilename();
        password = getPassword();
        out = new File(getRoot(), filename);
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
