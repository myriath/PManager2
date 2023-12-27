package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.activities.LoginActivity.getDBDir;
import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.util.AsyncUtil.uiThread;
import static com.example.mitch.pmanager.util.Constants.Extensions.V2;
import static com.example.mitch.pmanager.util.Constants.Extensions.V3;
import static com.example.mitch.pmanager.util.Constants.Extensions.V4;
import static com.example.mitch.pmanager.util.Encryption.decrypt;
import static com.example.mitch.pmanager.util.Encryption.encrypt;
import static com.example.mitch.pmanager.util.Encryption.generateKey;
import static com.example.mitch.pmanager.util.Encryption.importFile;
import static com.example.mitch.pmanager.util.FileUtil.PMFileToBank;
import static com.example.mitch.pmanager.util.FileUtil.readFile;
import static com.example.mitch.pmanager.util.FileUtil.writeExternal;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.documentfile.provider.DocumentFile;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.activities.LoginActivity;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.database.database.FileDatabase;
import com.example.mitch.pmanager.database.database.FolderDatabase;
import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.dialogs.CustomDialog;
import com.example.mitch.pmanager.models.Folder;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.PasswordBank;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import javax.crypto.SecretKey;

/**
 * FilesUtil utility class for upgrading old file versions, importing and exporting files
 *
 * @author mitch
 */
public class FilesUtil {
    /**
     * Util class, private constructor
     */
    private FilesUtil() {}

    public static ActivityResultCallback<Uri> exportCallback(LoginActivity activity) {
        return result -> {
            if (result == null) return;
            DocumentFile df = DocumentFile.fromSingleUri(activity, result);
            String chosenFilename = Objects.requireNonNull(df).getName();
            String ext = Objects.requireNonNull(chosenFilename).substring(chosenFilename.lastIndexOf('.'));
            if (!(ext.equals(V4))) {
                toast(activity.getString(R.string.accepted_ext), activity);
                return;
            }

            byte[] ad = chosenFilename.getBytes(StandardCharsets.UTF_8);

            try (OutputStream out = activity.getContentResolver().openOutputStream(result)) {
                Encryption.ExportedFile exported = new Encryption.ExportedFile(
                        encrypt(Files.readAllBytes(exportSrcFile.toPath()), ad, exportKey), );
                Encryption.exportFile(exported, out);
            } catch (IOException e) {

            }

            try (OutputStream out = activity.getContentResolver().openOutputStream(result)) {
                if (!writeExternal(readFile(oldAD, exportPwd, exportSrcFile), out, newAD, exportPwd)) {
                    throw new Exception("Couldn't write file");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                toast(R.string.wrong_password, activity);
            }

            exportSrcFile = null;
            Arrays.fill(exportPwd, (char) 0);
            exportPwd = null;
        };
    }

    public static ActivityResultCallback<Uri> importCallback(LoginActivity activity) {
        return result -> {
            if (result == null) return;

            DocumentFile df = DocumentFile.fromSingleUri(activity, result);
            String filename = Objects.requireNonNull(Objects.requireNonNull(df).getName());
            int splitIndex = filename.lastIndexOf('.');
            String displayName = filename.substring(0, splitIndex - 1);
            String ext = filename.substring(splitIndex);
            byte[] ad = filename.getBytes(StandardCharsets.UTF_8);

            CustomDialog dialog = new CustomDialog(
                    R.layout.dialog_password_only,
                    activity.getString(R.string.open_file),
                    activity.getString(R.string.open), activity.getString(R.string.cancel),
                    (dialogInterface, i, dialogView) -> {
                        char[] password = getFieldChars(R.id.passwordField, dialogView);
                        HashMap<String, Folder> folders = new HashMap<>();

                        FileDatabase fileDB = FileDatabase.singleton(activity);
                        FileEntity fileEntity = new FileEntity(displayName);
                        long fileID = fileDB.fileDAO().insert(fileEntity);
                        if (fileID == -1) return;
                        fileEntity.setId(fileID);

                        if (ext.equals(V2)) {
                            upgradeV2(activity, result, folders, fileEntity, fileDB, filename, password);
                            return;
                        }

                        Encryption.ExportedFile file;
                        try (InputStream in = activity.getContentResolver().openInputStream(result)) {
                            if (in == null) throw new IOException("in is null");
                            file = importFile(in);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        SecretKey key = generateKey(password, file.getSalt());

                        byte[] data;
                        try {
                            data = decrypt(file.getIv(), file.getCiphertext(), ad, key);
                        } catch (Exception e) {
                            uiThread().execute(() -> toast(R.string.wrong_password, activity));
                            return;
                        }

                        if (ext.equals(V3)) {
                            upgradeV3(activity, folders, fileEntity, fileDB, data);
                            return;
                        }

                        // Copy file to new place
                        File dest = new File(getDBDir(), fileID + "_db");
                        try(OutputStream out = Files.newOutputStream(dest.toPath())) {
                            out.write(data);
                        } catch (IOException e) {
                            uiThread().execute(() -> toast(R.string.error_failed_to_save, activity));
                            fileDB.fileDAO().delete(fileEntity);
                        }
                    }, (dialogInterface, i, dialogView) -> dialogInterface.cancel()
            );
            dialog.show(activity.getSupportFragmentManager());

            filesListAdapter.add(dest);
            checkEmptyText(filesListAdapter);
        };
    }

    /**
     * Takes a given folders hashmap and writes it to the prospective database
     * @param folders folders to write
     * @param folderDB database to write folder to
     */
    private static void writeFolders(HashMap<String, Folder> folders, FolderDatabase folderDB) {
        for (String key : folders.keySet()) {
            Folder folder = folders.get(key);

            long time = System.nanoTime();
            byte[][]
            FolderEntity folderEntity = new FolderEntity(time, )
        }
    }

    /**
     * Upgrades the file given by result to version 4 from version 2
     * @noinspection deprecation Used for backwards compatibility
     * @param context Context for the activity
     * @param importUri URI for loading from import
     * @param folders Folders hashmap for parsing to
     * @param fileEntity Entity for the file created in fileDB, deletes in case of failure
     * @param fileDB File database for storing the new file
     * @param filename Filename of the imported file
     * @param password Password of the imported file
     */
    private static void upgradeV2(
            Context context,
            Uri importUri,
            HashMap<String, Folder> folders,
            FileEntity fileEntity, FileDatabase fileDB,
            String filename, char[] password
    ) {
        String data;
        try (InputStream in = context.getContentResolver().openInputStream(importUri)) {
            data = AES.parse(in, filename, password);
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }
        Arrays.fill(password, (char) 0);

        AES.modernize(data, folders);
        writeFolders(folders);
    }

    /**
     * Upgrades a version 3 file to version 4
     * @param context Activity context
     * @param folders V4 data structure
     * @param fileEntity File entity from fileDB, deleted if conversion fails
     * @param fileDB File database for handling stored files in the database
     * @param data Decrypted data to upgrade
     */
    private static void upgradeV3(
            Context context,
            HashMap<String, Folder> folders,
            FileEntity fileEntity, FileDatabase fileDB,
            byte[] data
    ) {
        PasswordBank bank = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object read = ois.readObject();
            if (read.getClass() == PMFile.class) {
                bank = PMFileToBank((PMFile) read);
            } else if (read.getClass() == PasswordBank.class) {
                bank = (PasswordBank) read;
            }
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }
        if (bank == null) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }
        for (DomainEntry domain : bank.getEntries()) {
            folders.put(domain.getDomain(), new Folder(domain));
        }
        writeFolders(folders);
    }
}
