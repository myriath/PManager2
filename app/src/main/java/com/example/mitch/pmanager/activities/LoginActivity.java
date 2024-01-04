package com.example.mitch.pmanager.activities;

import static com.example.mitch.pmanager.util.AsyncUtil.diskIO;
import static com.example.mitch.pmanager.util.AsyncUtil.uiThread;
import static com.example.mitch.pmanager.util.Constants.BundleKeys.BUNDLE_FILE;
import static com.example.mitch.pmanager.util.Constants.BundleKeys.BUNDLE_KEY;
import static com.example.mitch.pmanager.util.Constants.BundleKeys.BUNDLE_OP;
import static com.example.mitch.pmanager.util.Constants.DP16;
import static com.example.mitch.pmanager.util.Constants.Encryption.AES_GCM_NOPADDING;
import static com.example.mitch.pmanager.util.Constants.Encryption.GCM_IV_LENGTH_OLD;
import static com.example.mitch.pmanager.util.Constants.Encryption.GCM_TAG_LENGTH;
import static com.example.mitch.pmanager.util.Constants.Encryption.RANDOM;
import static com.example.mitch.pmanager.util.Constants.Extensions.V4;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.FILE;
import static com.example.mitch.pmanager.util.Constants.IntentKeys.KEY;
import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;
import static com.example.mitch.pmanager.util.FilesUtil.exportCallback;
import static com.example.mitch.pmanager.util.FilesUtil.getFolderCount;
import static com.example.mitch.pmanager.util.FilesUtil.importCallback;
import static com.example.mitch.pmanager.util.FilesUtil.updateOrInsertMetadata;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldString;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.mitch.pmanager.database.database.FileDatabase;
import com.example.mitch.pmanager.database.database.FolderDatabase;
import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.database.entity.MetadataEntity;
import com.example.mitch.pmanager.deprecated.AES;
import com.example.mitch.pmanager.dialogs.CustomDialog;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.models.FileKey;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.PasswordEntry;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.PasswordBank;
import com.example.mitch.pmanager.objects.storage.UserEntry;
import com.example.mitch.pmanager.util.Constants;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/**
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
    public static final int EXIT = 2;
    private FilesAdapter filesListAdapter;

    private ActivityResultLauncher<String> exportFileLauncher;
    private ActivityResultLauncher<String[]> importFileLauncher;
    private ActivityResultLauncher<String> createTestV2Launcher;
    private ActivityResultLauncher<String> createTestPMFileLauncher;
    private ActivityResultLauncher<String> createTestPM3Launcher;

    private FileKey exportFileKey;
    private FileEntity exportSrc;

    private double progressDiff;
    private double currentProgress;
    private ProgressBar progressBar;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_login);
        DP16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        diskIO().execute(() -> {
            List<FileEntity> entities = FileDatabase.singleton(this).fileDAO().getFiles();
            for (FileEntity entity : entities) {
                entity.setSize(getFolderCount(this, entity));
                try {
                    entity.setMetadata(FolderDatabase.singleton(this, entity.getFilename()).metadataDAO().getMetadata());
                } catch (IndexOutOfBoundsException e) {
                    entity.setCorrupt(true);
                }
            }
            filesListAdapter = new FilesAdapter(this, entities);
            uiThread().execute(() -> {
                RecyclerView filesList = findViewById(R.id.filesList);
                ViewCompat.setOnApplyWindowInsetsListener(filesList, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom + DP16 * 5);
                    return WindowInsetsCompat.CONSUMED;
                });
                filesList.setItemAnimator(null); // TODO: Create animator
                filesList.setLayoutManager(new LinearLayoutManager(this));
                filesList.setAdapter(filesListAdapter);
                checkEmptyText();
            });
        });

        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                exportCallback(this)
        );

        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                importCallback(this)
        );

        // TODO: Delete for prod
        createTestV2Launcher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                this::createTestV2
        );

        // TODO: Delete for prod
        createTestPMFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                this::createTestPMFile
        );

        // TODO: Delete for prod
        createTestPM3Launcher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                this::createTestPM3
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
                        char[] password = getFieldChars(R.id.password, dialogLayout, true);
                        if (password.length == 0) {
                            ((TextInputLayout) dialogLayout.findViewById(R.id.password)).setError(getString(R.string.cannot_be_empty));
                            return;
                        }

                        final String finalFilename = filename;
                        diskIO().execute(() -> {
                            FileKey key = new FileKey(password);

                            FileDatabase fileDB = FileDatabase.singleton(this);
                            FileEntity entity = new FileEntity(finalFilename);
                            entity.setSize(0);
                            entity.setDuplicateNumber(getDuplicateFileCount(entity));
                            entity.setId(FileDatabase.singleton(this).fileDAO().insert(entity));
                            fileDB.fileDAO().update(entity);
                            if (entity.getId() == -1) {
                                uiThread().execute(() -> toast(R.string.error, this));
                                return;
                            }
                            FolderDatabase folderDB = FolderDatabase.singleton(this, entity.getFilename());
                            MetadataEntity metadata = updateOrInsertMetadata(null, folderDB, 0, key);

                            entity.setMetadata(metadata);
                            uiThread().execute(() -> {
                                filesListAdapter.add(entity);
                                checkEmptyText();
                            });
                        });
                        dialogInterface.dismiss();
                    }, (dialogInterface, i, dialogLayout) -> dialogInterface.cancel()
            );
            customDialog.show(getSupportFragmentManager());
        });

        findViewById(R.id.importButton).setOnClickListener((view) -> importFileLauncher.launch(new String[]{"application/octet-stream"}));

        findViewById(R.id.testButton).setOnClickListener(view -> createTestPMFileLauncher.launch("generatedtest.pm3"));
    }

    /**
     * Checks if the filesListAdapter is empty and properly displays texts accordingly
     */
    public void checkEmptyText() {
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
        Constants.CallbackCodes code = args.getSerializable(BUNDLE_OP, Constants.CallbackCodes.class);

        switch (Objects.requireNonNull(code)) {
            case LOAD_FILE: {
                final FileEntity file = args.getParcelable(BUNDLE_FILE, FileEntity.class);
                if (file == null || file.getDisplayName() == null) {
                    toast(R.string.error, this);
                    return;
                }
                CustomDialog customDialog = new CustomDialog(
                        R.layout.dialog_password_only,
                        getString(R.string.open_file), file.getDisplayName(),
                        getString(R.string.open), getString(R.string.cancel),
                        (dialogInterface, i, dialogView) -> {
                            startProgressBar(R.string.working, -1);
                            char[] password = getFieldChars(R.id.password, dialogView, true);
                            diskIO().execute(() -> {
                                FileKey fileKey = new FileKey(password, file.getMetadata().getSalt());
                                try {
                                    if (!file.getMetadata().check(fileKey)) throw new Exception();
                                    Intent intent = new Intent(this, FileOpenActivity.class);
                                    intent.putExtra(FILE, file);
                                    intent.putExtra(KEY, fileKey);
                                    // TODO: maybe check if this is needed through some result code?
//                            filesListAdapter.reset(getImportDir());
                                    endProgressBar();
                                    startActivity(intent);
                                } catch (Exception e) {
                                    uiThread().execute(() -> toast("Incorrect password", this));
                                }
                            });
                            dialogInterface.dismiss();
                        }, (dialogInterface, i, dialogView) -> dialogInterface.cancel()
                );
                showDialog(customDialog);
                break;
            }
            case EXPORT_FILE: {
                exportSrc = args.getParcelable(BUNDLE_FILE, FileEntity.class);
                exportFileKey = args.getParcelable(BUNDLE_KEY, FileKey.class);
                exportFileLauncher.launch(exportSrc.getDisplayName() + V4);
                break;
            }
        }
    }

    /**
     * Shows a given dialog using the support fragment manager.
     *
     * @param dialog Dialog to show
     */
    public void showDialog(CustomDialog dialog) {
        dialog.show(getSupportFragmentManager());
    }

    /**
     * Returns the export source file entity
     *
     * @return FileEntity for exporting
     */
    public FileEntity getExportSrc() {
        return exportSrc;
    }

    /**
     * Returns the encrypted password to be used when exporting
     *
     * @return EncryptedPassword to use for exporting
     */
    public FileKey getExportKey() {
        return exportFileKey;
    }

    /**
     * Clears the export source and password
     */
    public void clearExport() {
        exportSrc = null;
        exportFileKey = null;
    }

    /**
     * Adds a given file entity to the files list adapter
     *
     * @param entity Entity to add
     */
    public void addFile(FileEntity entity) {
        filesListAdapter.add(entity);
        checkEmptyText();
    }

    /**
     * Deletes a given file entity from the file list adapter
     *
     * @param entity Entity to delete
     */
    public void deleteFile(FileEntity entity) {
        filesListAdapter.remove(entity);
        checkEmptyText();
    }

    /**
     * Gets the number of duplicates for a given file entity
     *
     * @param entity Entity to get duplicate count for
     * @return Number of entities sharing the given entity's displayName
     */
    public long getDuplicateFileCount(FileEntity entity) {
        return filesListAdapter.getDuplicates(entity.getDisplayName());
    }

    public void startProgressBar(int rString, int totalCount) {
        progressDiff = 100.0 / totalCount;
        uiThread().execute(() -> {
            progressText.setText(rString);
            if (totalCount == -1) {
                progressBar.setVisibility(View.INVISIBLE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
            }
            progressBar.setProgress(0);
            findViewById(R.id.progressLayout).setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        });
    }

    /**
     * Increments the import bar accordingly to how many items are being imported
     */
    public void incrementProgressBar() {
        uiThread().execute(() -> {
            currentProgress += progressDiff;
            progressBar.setProgress((int) Math.round(currentProgress));
        });
    }

    /**
     * Re-enables interaction and hides the importLayout
     */
    public void endProgressBar() {
        uiThread().execute(() -> {
            findViewById(R.id.progressLayout).setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        });
    }

    /**
     * TODO: Remove for release
     * Test function for creating an example pm3 file for conversion testing
     *
     * @param result result from the file chooser
     */
    public void createTestPM3(Uri result) {
        // TODO: test, should be similar to pmfile
        ArrayList<DomainEntry> entries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ArrayList<UserEntry> entry = new ArrayList<>();
            entry.add(new UserEntry(
                    ("Username #" + i).toCharArray(),
                    ("Password #" + i).toCharArray()
            ));
            entries.add(new DomainEntry(
                    entry, "Domain #" + i
            ));
        }
        PasswordBank bank = new PasswordBank(entries);

        try (
                OutputStream out = getContentResolver().openOutputStream(result);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {
            if (out == null) throw new Exception("out null");

            oos.writeObject(bank);
            oos.flush();

            String name = Objects.requireNonNull(
                    Objects.requireNonNull(DocumentFile.fromSingleUri(this, result)).getName()
            );
            byte[] salt = new byte[]{1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6};
            char[] password = "test".toCharArray();
            FileKey key = new FileKey(password, salt);

            byte[] iv = new byte[GCM_IV_LENGTH_OLD];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(
                    Cipher.ENCRYPT_MODE, key.getKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            );
            cipher.updateAAD(name.getBytes(STRING_ENCODING));
            byte[] ciphertext = cipher.doFinal(bos.toByteArray());

            out.write(key.getSalt());
            out.write(iv);
            out.write(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove for release
     * Test function for creating an example pmfile file for conversion testing
     *
     * @param result result from the file chooser
     */
    public void createTestPMFile(Uri result) {
        ArrayList<PasswordEntry> entries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entries.add(new PasswordEntry(
                    ("Domain #" + i).toCharArray(),
                    ("Username #" + i).toCharArray(),
                    ("Password #" + i).toCharArray(),
                    i
            ));
        }
        PMFile pmFile = new PMFile(Constants.Version.V3, entries, null);

        try (
                OutputStream out = getContentResolver().openOutputStream(result);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {
            if (out == null) throw new Exception("out null");

            oos.writeObject(pmFile);
            oos.flush();

            String name = Objects.requireNonNull(
                    Objects.requireNonNull(DocumentFile.fromSingleUri(this, result)).getName()
            );
            byte[] salt = new byte[]{1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6};
            char[] password = "test".toCharArray();
            FileKey key = new FileKey(password, salt);

            byte[] iv = new byte[GCM_IV_LENGTH_OLD];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(
                    Cipher.ENCRYPT_MODE, key.getKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            );
            cipher.updateAAD(name.getBytes(STRING_ENCODING));
            byte[] ciphertext = cipher.doFinal(bos.toByteArray());

            out.write(key.getSalt());
            out.write(iv);
            out.write(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove for release
     * Unused test function for creating an example v2 file to test conversion
     *
     * @param result result from the file chooser
     */
    public void createTestV2(Uri result) {
        ArrayList<String> dat = new ArrayList<>();
        dat.add("test.jpweds");
        for (int i = 0; i < 10; i++) {
            dat.add(String.format(Locale.ENGLISH, "Domain%d", i));
            dat.add(String.format(Locale.ENGLISH, "Username%d", i));
            dat.add(String.format(Locale.ENGLISH, "Password%d", i));
        }
        dat.add("TestEmpty");
        dat.add("");
        dat.add("lol");
        StringBuilder sb = new StringBuilder();
        for (String str : dat) {
            sb.append(str);
            sb.append('\0');
        }
        try (OutputStream out = getContentResolver().openOutputStream(result)){
            if (out == null) throw new Exception("out null");
            AES f = new AES(AES.pad("test"));
            byte[] value = f.encryptString(sb.toString());
            try (ByteArrayInputStream bais = new ByteArrayInputStream(value)) {
                if (!f.decrypt(bais).split(System.lineSeparator())[0].equals("test.jpweds")) {
                    createTestV2(result);
                }
            }
            out.write(value);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
