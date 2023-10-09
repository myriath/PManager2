package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.util.ByteCharStringUtil.getFieldChars;
import static com.example.mitch.pmanager.util.ByteCharStringUtil.getFieldString;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_CODE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_FILE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_PWD;
import static com.example.mitch.pmanager.util.Constants.DP16;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILE;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILEDATA;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILENAME;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.PASSWORD;
import static com.example.mitch.pmanager.util.Constants.Version.V2;
import static com.example.mitch.pmanager.util.Constants.Version.V3;
import static com.example.mitch.pmanager.util.FileUtil.copyFile;
import static com.example.mitch.pmanager.util.FileUtil.readFile;
import static com.example.mitch.pmanager.util.FileUtil.writeExternal;
import static com.example.mitch.pmanager.util.FileUtil.writeFile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.adapters.FilesAdapter;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.example.mitch.pmanager.util.Constants;
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
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.NoSuchPaddingException;

/**
 * TODO: Automatically sort files
 * TODO: Automatically sort domains
 * TODO: Eye should turn to /eye when showing passwords
 * TODO: Automatically open new files
 * TODO: move delete in edit to password line
 * TODO: edit domain name
 * TODO: Fix scrolling issues with keyboard (also toolbar goes off screen) editing
 * TODO: JavaDocs
 * <p>
 * server : none : nu11-SQL
 */
public class LoginActivity extends AppCompatActivity implements CallbackListener {

    public static File ROOT_DIR = null;
    public static final int EXIT = 2;
    private FilesAdapter filesListAdapter;

    private ActivityResultLauncher<String> exportFileLauncher;
    private File exportSrcFile;
    private char[] exportPwd;

    private ActivityResultLauncher<String[]> importFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_login);
        DP16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        getRoot();

        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                result -> {
                    if (result == null) return;
                    DocumentFile df = DocumentFile.fromSingleUri(this, result);
                    String chosenFilename = Objects.requireNonNull(df).getName();
                    String ext = Objects.requireNonNull(chosenFilename).substring(chosenFilename.lastIndexOf('.'));
                    if (!(ext.equals(V2.ext) || ext.equals(V3.ext))) {
                        toast(getString(R.string.accepted_ext), this);
                        return;
                    }

                    byte[] oldAD = exportSrcFile.getName().getBytes(StandardCharsets.UTF_8);
                    byte[] newAD = chosenFilename.getBytes(StandardCharsets.UTF_8);

                    try (
                            OutputStream out = this.getContentResolver().openOutputStream(result)
                    ) {
                        if (!writeExternal(readFile(oldAD, exportPwd, exportSrcFile), out, newAD, exportPwd)) {
                            throw new Exception("Couldn't write file");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        toast(R.string.wrong_password, this);
                    }

                    exportSrcFile = null;
                    Arrays.fill(exportPwd, (char) 0);
                    exportPwd = null;
                }
        );

        filesListAdapter = new FilesAdapter(this, ROOT_DIR, this);
        RecyclerView filesList = findViewById(R.id.filesList);
        ViewCompat.setOnApplyWindowInsetsListener(filesList, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom + DP16 * 5);
            return WindowInsetsCompat.CONSUMED;
        });
        filesList.setItemAnimator(null); // TODO: Create animator
        filesList.setLayoutManager(new LinearLayoutManager(this));
        filesList.setAdapter(filesListAdapter);
        checkEmptyText(filesListAdapter);

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
                    filesListAdapter.add(dest);
                    checkEmptyText(filesListAdapter);
                }
        );

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
                checkEmptyText(filesListAdapter);
                dialog.dismiss();
            });
        });

        findViewById(R.id.importButton).setOnClickListener((view) -> importFileLauncher.launch(new String[]{"application/octet-stream"}));

        findViewById(R.id.testButton).setOnClickListener((this::createTestOldFile));
    }

    /**
     * Checks if the filesListAdapter is empty and properly displays texts accordingly
     *
     * @param filesListAdapter Adapter to check
     */
    private void checkEmptyText(FilesAdapter filesListAdapter) {
        if (filesListAdapter.getItemCount() == 0) {
            findViewById(R.id.newButtonText).setVisibility(View.VISIBLE);
            findViewById(R.id.importButtonText).setVisibility(View.VISIBLE);
            findViewById(R.id.emptyText).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.newButtonText).setVisibility(View.GONE);
            findViewById(R.id.importButtonText).setVisibility(View.GONE);
            findViewById(R.id.emptyText).setVisibility(View.GONE);
        }
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
     *
     * @param text    Text to toast
     * @param context Context for the source app
     */
    public static void toast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void callback(Bundle args) {
        Constants.CallbackCodes code = (Constants.CallbackCodes) args.getSerializable(CALLBACK_CODE);
        if (Objects.requireNonNull(code) == Constants.CallbackCodes.LOAD_FILE) {
            final File file = (File) args.getSerializable(CALLBACK_FILE);
            if (file == null || !file.exists()) {
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
                            // TODO: maybe check if this is needed through some result code?
                            filesListAdapter.reset(ROOT_DIR);
                            startActivity(intent);
                        } catch (Exception e) {
                            toast("Incorrect password", this);
                        }
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        } else if (Objects.requireNonNull(code) == Constants.CallbackCodes.DELETE_FILE) {
            checkEmptyText(filesListAdapter);
        } else if (Objects.requireNonNull(code) == Constants.CallbackCodes.EXPORT_FILE) {
            exportSrcFile = (File) args.getSerializable(CALLBACK_FILE);
            exportPwd = args.getCharArray(CALLBACK_PWD);
            exportFileLauncher.launch(exportSrcFile.getName());
        }
    }

    /**
     * TODO: Remove for release
     * Unused test function for creating an example v2 file to test conversion
     *
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
        dat.add("lol");
        StringBuilder sb = new StringBuilder();
        for (String str : dat) {
            sb.append(str);
            sb.append('\0');
        }
        try {
            AES f = new AES(AES.pad("test"));
            f.encryptString(sb.toString(), file);
            if (!f.decrypt(file).split(System.lineSeparator())[0].equals("test.jpweds")) {
                createTestOldFile(view);
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
