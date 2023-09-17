package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.Constants.IntentKeys.FILEDATA;
import static com.example.mitch.pmanager.Constants.IntentKeys.FILENAME;
import static com.example.mitch.pmanager.Constants.IntentKeys.PASSWORD;
import static com.example.mitch.pmanager.Constants.Version.V2;
import static com.example.mitch.pmanager.Constants.Version.V3;
import static com.example.mitch.pmanager.Util.charsToBytes;
import static com.example.mitch.pmanager.Util.getFieldChars;
import static com.example.mitch.pmanager.Util.getFieldString;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
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

import com.example.mitch.pmanager.Constants;
import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.Util;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.exceptions.DirectoryException;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.Perm;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {

    File out;
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
            toast(getString(R.string.perm_please_allow), this);
            finish();
        } else {
            if (requestCode == REQUEST_READ_STORAGE) {
                Perm permB = new Perm(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_STORAGE);
                checkPermission(permB);
            }
        }
    }

    /**
     * Checks a given permission.
     * @param permission Permission to check for.
     */
    private void checkPermission(Perm permission) {
        if (ContextCompat.checkSelfPermission(this, permission.permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission.permission}, permission.code);
        }
    }

    /**
     * Checks we have permissions for the external and internal storage
     */
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

    /**
     * Code to delete a file. Double checks via password to make sure the user wants to delete.
     * Deletes both v2 and v3 files.
     * @param view For button onClick()
     */
    public void deleteFile(View view) {
        final Context self = this;
        try {
            char[][] strs = setupOpenFile();
            final byte[] filename = charsToBytes(strs[0]);

            PMFile.readFile(filename, strs[1], out);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.are_you_sure);
            builder.setMessage(R.string.re_enter_password);
            @SuppressLint("InflateParams")
            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_delete_file, null);
            builder.setView(dialogLayout);
            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                char[] pwd = getFieldChars(R.id.dialog_new_password, dialogLayout);
                try {
                    PMFile.readFile(filename, pwd, out);
                    if (out.delete()) {
                        String plainName = out.getName().split("\\.")[0];
                        new File(getRoot(), plainName + V2.ext).delete();
                        toast(getString(R.string.file_deleted), self);
                    } else {
                        toast(getString(R.string.warning_file_not_deleted), self);
                    }
                } catch (Exception e1) {
                    toast(getString(R.string.wrong_password), self);
                }
            });

            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> toast(getString(R.string.cancelled), self));

            AlertDialog dialog = builder.create();
            dialog.show();
            resetFields();
        } catch (Exception e1) {
            toast(R.string.wrong_password, this);
        }
    }

    /**
     * Code for changing the password of a specified file.
     * @param view For button onClick()
     */
    public void changePassword(View view) {
        final Context self = this;
        try {
            char[][] strs = setupOpenFile();
            final byte[] filename = charsToBytes(strs[0]);
            final PMFile pmFile = PMFile.readFile(filename, strs[1], out);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.change_password);
            @SuppressLint("InflateParams")
            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
            builder.setView(dialogLayout);
            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                char[] pwd = getFieldChars(R.id.dialog_new_password, dialogLayout);
                try {
                    pmFile.writeFile(filename, pwd);
                } catch (Exception e1) {
                    toast(R.string.wrong_password, self);
                }
            });

            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> toast(R.string.cancelled, self));

            AlertDialog dialog = builder.create();
            dialog.show();
            resetFields();
        } catch (Exception e1) {
            toast(R.string.wrong_password, this);
        }
    }

    /**
     * Creates the in/out folders
     * @param view For button onClick()
     */
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

    /**
     * Deletes the in/out folders
     * @param view For button onClick()
     */
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

    /**
     * Imports the file from the downloads in/out folder.
     * @param view For button onClick()
     */
    public void importFile(View view) {
        String filename = getFilename(V3);

        File source = new File(inPath + filename);
        if (!source.exists()) {
            filename = getFilename(V2);
            source = new File(inPath + filename);
        }

        File destination = new File(getRoot(), filename);

        if (Util.copyFile(source.toPath(), destination.toPath())) {
            toast("File imported", this);
        } else {
            toast("Warning: File not imported!", this);
        }
    }

    /**
     * Exports the file to the downloads in/out folder.
     * @param view For button onClick()
     */
    public void exportFile(View view) {
        String file = getFilename(V3);

        File source = new File(getRoot(), file);
        File destination = new File(outPath + file);

        if (Util.copyFile(source.toPath(), destination.toPath())) {
            toast("File Exported", this);
        } else {
            toast("Warning: File not Exported!", this);
        }
    }

    /**
     * Opens the file specified if valid credentials.
     * @param view For button onClick()
     */
    public void openFile(View view) {
        try {
            char[][] strs = setupOpenFile();
            byte[] filename = charsToBytes(strs[0]);
            char[] password = strs[1];
            resetFields();
            String message;
            if (out.exists()) {
                message = "Opened!";
            } else {
                new PMFile(V3, new ArrayList<>(), out).writeFile(filename, password);
                message = "New File Created!";
            }

            Intent intent = new Intent(this, MainScreenActivity.class);
            intent.putExtra(FILENAME.key, filename);
            intent.putExtra(PASSWORD.key, password);
            intent.putExtra(FILEDATA.key, PMFile.readFile(filename, password, out));

            toast(message, this);
            startActivity(intent);
        } catch (Exception e1) {
            toast("Wrong Password!", this);
            resetFields();
        }
    }

    /**
     * Gets the full filename stored on disk.
     * @param version File extension version to use.
     * @return Full filename used on disk.
     */
    @NonNull
    private String getFilename(Constants.Version version) {
        return getFieldString(R.id.filenameField, getActivityView()) + version.ext;
    }

    /**
     * Gets the root file directory for the app.
     * @return File pointing to the root files directory.
     */
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

    /**
     * Creates a File in the out variable. If the file is v2, automatically translates it to v3
     * @return array containing the filename and password as char[].
     * @throws Exception Exception from the decryption process.
     */
    private char[][] setupOpenFile() throws Exception {
        String filename = getFilename(V3);
        char[] password = getFieldChars(R.id.passwordField, getActivityView());
        out = new File(getRoot(), filename);
        if (!out.exists()) {
            File old = new File(getRoot(), getFilename(V2));
            if (old.exists()) {
                if (!PMFile.translateV2toV3(old, out, password)) {
                    toast("Error converting v2 file!", this);
                }
            }
        }

        return new char[][]{filename.toCharArray(), password};
    }

    /**
     * Resets the filename and password fields
     */
    private void resetFields() {
        EditText fn = findViewById(R.id.filenameField);
        EditText pd = findViewById(R.id.passwordField);
        fn.getText().clear();
        pd.getText().clear();
    }

    /**
     * Makes a toast on the given context
     * @param stringId R string id to display
     * @param context Context for the source app
     */
    public static void toast(int stringId, Context context) {
        Toast.makeText(context, stringId, Toast.LENGTH_LONG).show();
    }

    /**
     * Makes a toast on the given context
     * @param text Text to toast
     * @param context Context for the source app
     */
    public static void toast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    /**
     * TODO: Remove for release
     * Unused test function for creating an example v2 file to test conversion
     * @param view view for the onclick function from a button.
     */
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
        dat.add("TestEmpty");
        dat.add("");
        dat.add("");
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
            toast(R.string.saved, this);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            toast(R.string.warning_file_not_saved, this);
            e1.printStackTrace();
        }
    }

    /**
     * Gets this activity's content view
     * @return View for this activity
     */
    private View getActivityView() {
        return getWindow().getDecorView();
    }
}
