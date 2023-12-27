package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.util.Constants.CALLBACK_CODE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_FILE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_PWD;
import static com.example.mitch.pmanager.util.Constants.DP16;
import static com.example.mitch.pmanager.util.Constants.Extensions.V3;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILE;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILEDATA;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILENAME;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.PASSWORD;
import static com.example.mitch.pmanager.util.FileUtil.readFile;
import static com.example.mitch.pmanager.util.FileUtil.writeFile;
import static com.example.mitch.pmanager.util.FilesUtil.exportCallback;
import static com.example.mitch.pmanager.util.FilesUtil.importCallback;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldString;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.adapters.FilesAdapter;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.dialogs.CustomDialog;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.example.mitch.pmanager.util.Constants;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * TODO: Automatically sort files
 * TODO: Automatically sort domains
 * TODO: Eye should turn to /eye when showing passwords
 * TODO: Automatically open new files
 * TODO: move delete in edit to password line
 * TODO: edit domain name
 * TODO: Fix scrolling issues with keyboard (also toolbar goes off screen) editing
 * TODO: JavaDocs
 * TODO: Fix crash when deleting last domain (sometimes? maybe after deleting others?)
 * <p>
 * server : none : nu11-SQL
 */
public class LoginActivity extends AppCompatActivity implements CallbackListener {
    private static File FILES_DIR = null;
    private static File IMPORT_DIR = null;
    private static File DB_DIR = null;
    public static final int EXIT = 2;
    private FilesAdapter filesListAdapter;

    private ActivityResultLauncher<String> exportFileLauncher;
    private File exportSrcFile;
    private SecretKey exportKey;

    private ActivityResultLauncher<String[]> importFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FILES_DIR = getFilesDir();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

//        setStatusBarColors(getResources(), getWindow());

        setContentView(R.layout.activity_login);
        DP16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        filesListAdapter = new FilesAdapter(this, getImportDir(), this);
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

        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                exportCallback(this)
        );

        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                importCallback(this)
        );

        findViewById(R.id.newButton).setOnClickListener((view) -> {
            CustomDialog customDialog = new CustomDialog(
                    R.layout.dialog_create_file,
                    getString(R.string.create_file),
                    getString(R.string.create), getString(R.string.cancel),
                    (dialogInterface, i, dialogLayout) -> {
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
                            dialogInterface.cancel();
                            return;
                        }
                        File file = new File(getImportDir(), filename + V3.ext);
                        if (!writeFile(
                                new PasswordBank(),
                                file,
                                file.getName().getBytes(StandardCharsets.UTF_8),
                                getFieldChars(R.id.password, dialogLayout)
                        )) {
                            dialogInterface.cancel();
                            return;
                        }
                        filesListAdapter.add(file);
                        checkEmptyText(filesListAdapter);
                        dialogInterface.dismiss();
                    }, (dialogInterface, i, dialogLayout) -> dialogInterface.cancel()
            );
            customDialog.show(getSupportFragmentManager());
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
    public static File getImportDir() {
        if (IMPORT_DIR != null) return IMPORT_DIR;
        IMPORT_DIR = new File(FILES_DIR, "PManager");
        if (IMPORT_DIR.exists()) return IMPORT_DIR;
        if (!IMPORT_DIR.mkdirs()) return null;
        System.out.println("Created PManager");
        return IMPORT_DIR;
    }

    public static File getDBDir() {
        if (DB_DIR != null) return DB_DIR;
        DB_DIR = new File(FILES_DIR, "databases");
        if (DB_DIR.exists()) return DB_DIR;
        if (!DB_DIR.mkdirs()) return null;
        System.out.println("Created databases");
        return DB_DIR;
    }

    /**
     * Makes a toast on the given context
     *
     * @param stringId R string id to display
     * @param context  Context for the source app
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

            CustomDialog customDialog = new CustomDialog(
                    R.layout.dialog_password_only,
                    getString(R.string.open_file), file.getName(),
                    getString(R.string.open), getString(R.string.cancel),
                    (dialogInterface, i, dialogView) -> {
                        char[] pwd = getFieldChars(R.id.password, dialogView);
                        byte[] ad = file.getName().getBytes(StandardCharsets.UTF_8);

                        try {
                            Intent intent = new Intent(this, FileOpenActivity.class);
                            intent.putExtra(FILE.key, file);
                            intent.putExtra(FILENAME.key, ad);
                            intent.putExtra(PASSWORD.key, pwd);
                            intent.putExtra(FILEDATA.key, readFile(ad, pwd, file));
                            // TODO: maybe check if this is needed through some result code?
                            filesListAdapter.reset(getImportDir());
                            startActivity(intent);
                        } catch (Exception e) {
                            toast("Incorrect password", this);
                        }
                        dialogInterface.dismiss();
                    }, (dialogInterface, i, dialogView) -> dialogInterface.cancel()
            );
            customDialog.show(getSupportFragmentManager());
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
        File file = new File(getImportDir(), "test.jpweds");
        File file2 = new File(getImportDir(), "test.pm3");
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
