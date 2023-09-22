package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.Constants.CALLBACK_CODE;
import static com.example.mitch.pmanager.Constants.IntentKeys.FILE;
import static com.example.mitch.pmanager.Constants.IntentKeys.FILEDATA;
import static com.example.mitch.pmanager.Constants.IntentKeys.FILENAME;
import static com.example.mitch.pmanager.Constants.IntentKeys.PASSWORD;
import static com.example.mitch.pmanager.Constants.Version.V2;
import static com.example.mitch.pmanager.Constants.Version.V3;
import static com.example.mitch.pmanager.Util.copyFile;
import static com.example.mitch.pmanager.Util.getFieldChars;
import static com.example.mitch.pmanager.Util.getFieldString;
import static com.example.mitch.pmanager.Util.readFile;
import static com.example.mitch.pmanager.Util.setWindowInsets;
import static com.example.mitch.pmanager.Util.writeFile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.Constants;
import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.Util;
import com.example.mitch.pmanager.adapters.FilesAdapter;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.NoSuchPaddingException;

public class LoginActivity extends AppCompatActivity implements CallbackListener {

    public static File ROOT_DIR = null;
    public static final int EXIT = 2;

    private ActivityResultLauncher<String[]> importFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Util.setStatusBarColors(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_login);
        Util.DP16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        getRoot();

        ActivityResultLauncher<String> exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                result -> {
                    if (result == null) return;
                    DocumentFile df = DocumentFile.fromSingleUri(this, result);
                    String filename = Objects.requireNonNull(df).getName();
                    String ext = Objects.requireNonNull(filename).substring(filename.lastIndexOf('.'));
                    if (!(ext.equals(V2.ext) || ext.equals(V3.ext))) {
                        toast(getString(R.string.accepted_ext), this);
                        return;
                    }
                    File src = new File(ROOT_DIR, Objects.requireNonNull(filename));
                    if (!src.exists()) {
                        toast(getString(R.string.filename_was_changed), this);
                        return;
                    }
                    try (
                            InputStream in = Files.newInputStream(src.toPath());
                            OutputStream out = getContentResolver().openOutputStream(result)
                    ) {
                        copyFile(in, Objects.requireNonNull(out));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        FilesAdapter filesListAdapter = new FilesAdapter(this, ROOT_DIR, this, exportFileLauncher);
        RecyclerView filesList = findViewById(R.id.filesList);
        filesList.setItemAnimator(null); // TODO: Create animator
        filesList.setLayoutManager(new LinearLayoutManager(this));
        filesList.setAdapter(filesListAdapter);
        if (filesListAdapter.getItemCount() == 0) {
            findViewById(R.id.newButtonText).setVisibility(View.VISIBLE);
            findViewById(R.id.importButtonText).setVisibility(View.VISIBLE);
            findViewById(R.id.emptyText).setVisibility(View.VISIBLE);
        }

        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                result -> {
                    if (result == null) return;
                    DocumentFile df = DocumentFile.fromSingleUri(this, result);
                    String filename = Objects.requireNonNull(df).getName();
                    String ext = Objects.requireNonNull(filename).substring(filename.lastIndexOf('.'));
                    if (!(ext.equals(V2.ext) || ext.equals(V3.ext))) {
                        toast(getString(R.string.accepted_ext), this);
                        return;
                    }
                    File dest = new File(ROOT_DIR, Objects.requireNonNull(filename));
                    if (filesListAdapter.fileExists(filename) || dest.exists()) {
                        toast(R.string.error, this);
                        return;
                    }
                    try (
                            InputStream in = getContentResolver().openInputStream(result);
                            OutputStream out = Files.newOutputStream(dest.toPath())
                    ) {
                        copyFile(Objects.requireNonNull(in), out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );

        setWindowInsets(findViewById(R.id.buttonPanel), 0, 0, 0);

        findViewById(R.id.newButton).setOnClickListener((view) -> {
            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_create_file, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(LoginActivity.this)
                    .setView(dialogLayout)
                    .setTitle(R.string.create_file)
                    .setPositiveButton(R.string.create, null)
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view1) -> {
                String filename = getFieldString(R.id.filename, dialogLayout);
                if (filename.length() == 0) {
                    ((TextInputLayout) dialogLayout.findViewById(R.id.filename)).setError(getString(R.string.cannot_be_empty));
                    return;
                }
                ((TextInputLayout) dialogLayout.findViewById(R.id.filename)).setError("");
                if (getFieldChars(R.id.password, dialogLayout).length == 0) {
                    ((TextInputLayout) dialogLayout.findViewById(R.id.password)).setError(getString(R.string.cannot_be_empty));
                    return;
                }
                if (filesListAdapter.fileExists(filename)) {
                    dialog.cancel();
                    return;
                }
                File file = new File(ROOT_DIR, filename + V3.ext);
                if (!writeFile(
                        new PasswordBank(),
                        file,
                        file.getName().getBytes(StandardCharsets.UTF_8),
                        getFieldChars(R.id.password, dialogLayout)
                )) {
                    dialog.cancel();
                    return;
                }
                filesListAdapter.add(file);
                dialog.dismiss();
            });
        });

        findViewById(R.id.importButton).setOnClickListener((view) -> importFileLauncher.launch(new String[]{"application/octet-stream"}));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // TODO
    }

    /**
     * Gets the root file directory for the app.
     */
    private void getRoot() {
        ROOT_DIR = new File(getFilesDir(), "PManager");
        if (!ROOT_DIR.exists()) {
            if (ROOT_DIR.mkdirs()) {
                System.out.println("done");
            }
        }
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
        File file = new File(ROOT_DIR, "test.jpweds");
        File file2 = new File(ROOT_DIR, "test.pm3");
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
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Callback bundle key for the file
     */
    public static final String CALLBACK_FILE = "file";

    @Override
    public void callback(Bundle args) {
        Constants.CallbackCodes code = (Constants.CallbackCodes) args.getSerializable(CALLBACK_CODE);
        if (Objects.requireNonNull(code) == Constants.CallbackCodes.LOAD_FILE) {
            final File file = (File) args.getSerializable(CALLBACK_FILE);
            if (file == null || !file.exists()) {
                Log.i("Bruh", String.valueOf(Objects.requireNonNull(file).exists()));
                toast(R.string.error, this);
                return;
            }

            final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_password_only, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                    .setView(dialogLayout)
                    .setTitle(R.string.open_file)
                    .setMessage(file.getName())
                    .setPositiveButton(R.string.open, (dialogInterface, i) -> {
                        char[] pwd = getFieldChars(R.id.password, dialogLayout);
                        byte[] ad = file.getName().getBytes(StandardCharsets.UTF_8);

                        try {
                            Intent intent = new Intent(this, FileOpenActivity.class);
                            intent.putExtra(FILE.key, file);
                            intent.putExtra(FILENAME.key, ad);
                            intent.putExtra(PASSWORD.key, pwd);
                            intent.putExtra(FILEDATA.key, readFile(ad, pwd, file));
                            startActivity(intent);
                        } catch (Exception e) {
                            toast("Incorrect password", this);
                        }
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        }
    }
}
