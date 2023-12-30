package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.util.AsyncUtil.diskIO;
import static com.example.mitch.pmanager.util.AsyncUtil.uiThread;
import static com.example.mitch.pmanager.util.Constants.Extensions.SHM;
import static com.example.mitch.pmanager.util.Constants.Extensions.V2;
import static com.example.mitch.pmanager.util.Constants.Extensions.V3;
import static com.example.mitch.pmanager.util.Constants.Extensions.V4;
import static com.example.mitch.pmanager.util.Constants.Extensions.WAL;
import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;
import static com.example.mitch.pmanager.util.Encryption.ENCRYPT_RETURN_CIPHERTEXT;
import static com.example.mitch.pmanager.util.Encryption.ENCRYPT_RETURN_IV;
import static com.example.mitch.pmanager.util.Encryption.SHA512;
import static com.example.mitch.pmanager.util.Encryption.decrypt;
import static com.example.mitch.pmanager.util.Encryption.encrypt;
import static com.example.mitch.pmanager.util.Encryption.exportTar;
import static com.example.mitch.pmanager.util.Encryption.generateSalt;
import static com.example.mitch.pmanager.util.Encryption.getAssociatedData;
import static com.example.mitch.pmanager.util.Encryption.importFile;
import static com.example.mitch.pmanager.util.Encryption.importTar;
import static com.example.mitch.pmanager.util.FileUtil.PMFileToBank;
import static com.example.mitch.pmanager.util.GsonUtil.gson;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.activities.LoginActivity;
import com.example.mitch.pmanager.background.AES;
import com.example.mitch.pmanager.database.database.FileDatabase;
import com.example.mitch.pmanager.database.database.FolderDatabase;
import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.dialogs.CustomDialog;
import com.example.mitch.pmanager.models.EncryptedPassword;
import com.example.mitch.pmanager.models.Folder;
import com.example.mitch.pmanager.objects.PMFile;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.PasswordBank;

import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

/**
 * FilesUtil utility class for upgrading old file versions, importing and exporting files
 *
 * @author mitch
 */
public class FilesUtil {
    /**
     * Util class, private constructor
     */
    private FilesUtil() {
    }

    public static int getFolderCount(Context context, FileEntity entity) {
        return FolderDatabase.singleton(context, entity.getFilename()).folderDAO().getFolders().size();
    }

    public static List<Folder> getFolders(List<FolderEntity> entities, EncryptedPassword password) throws Exception {
        List<Folder> folders = new ArrayList<>(entities.size());
        for (FolderEntity entity : entities) {
            folders.add(getFolder(entity, password));
        }
        return folders;
    }

    public static Folder getFolder(FolderEntity entity, EncryptedPassword password) throws Exception {
        SecretKeySpec key = password.getKey(entity.getSalt());
        byte[] data = decrypt(
                entity.getIv(),
                entity.getEncryptedJson(),
                getAssociatedData(entity.getId(), entity.getLastAccessed(), entity.getLabel()),
                key
        );

        Folder folder = gson().fromJson(new String(data, STRING_ENCODING), Folder.class);
        folder.setEntity(entity);
        return folder;
    }

    public static void updateOrInsertFolder(Folder folder, @Nullable FolderEntity entity, FolderDatabase database, EncryptedPassword password) {
        long time = System.nanoTime();

        byte[] salt = generateSalt();

        long id;
        if (entity == null) {
            entity = new FolderEntity(time, salt);
            id = database.folderDAO().insert(entity);
            entity.setId(id);
        } else {
            entity.setLastAccessed(time);
            entity.setSalt(salt);
            id = entity.getId();
        }
        if (id == -1) {
            throw new RuntimeException("Error inserting folderEntity");
            // TODO: maybe not the right error type?
        }

        byte[] labelHash = SHA512(folder.getLabel());
        entity.setLabel(labelHash);

        byte[] associatedData = getAssociatedData(id, time, labelHash);

        byte[] data = gson().toJson(folder).getBytes(STRING_ENCODING);
        SecretKeySpec key = password.getKey(salt);
        byte[][] encrypted = encrypt(data, associatedData, key);

        entity.setIv(encrypted[ENCRYPT_RETURN_IV]);
        entity.setEncryptedJson(encrypted[ENCRYPT_RETURN_CIPHERTEXT]);
        database.folderDAO().update(entity);
    }

    public static void checkpoint(Context context) {
        SupportSQLiteDatabase db = FileDatabase.singleton(context).getOpenHelper().getWritableDatabase();
        db.query("PRAGMA wal_checkpoint(FULL);");
        db.query("PRAGMA wal_checkpoint(TRUNCATE);");
    }

    public static ActivityResultCallback<Uri> exportCallback(LoginActivity activity) {
        return result -> {
            if (result == null) return;
            diskIO().execute(() -> {
                DocumentFile df = DocumentFile.fromSingleUri(activity, result);
                String chosenFilename = Objects.requireNonNull(df).getName();
                String ext = Objects.requireNonNull(chosenFilename).substring(chosenFilename.lastIndexOf('.'));
                if (!(ext.equals(V4))) {
                    uiThread().execute(() -> toast(activity.getString(R.string.accepted_ext), activity));
                    return;
                }
                byte[] ad = chosenFilename.getBytes(STRING_ENCODING);

                File db = activity.getExportSrc().getFile(activity);
                File wal = new File(db.getPath() + WAL);
                File shm = new File(db.getPath() + SHM);
                byte[] salt = generateSalt();
                SecretKeySpec key = activity.getExportPwd().getKey(salt);
                try (OutputStream out = activity.getContentResolver().openOutputStream(result)) {
                    if (out == null) throw new RuntimeException("output stream null");
                    checkpoint(activity);
                    exportTar(
                            new Encryption.ExportedFile(encrypt(Files.readAllBytes(db.toPath()), ad, key), salt),
                            new Encryption.ExportedFile(encrypt(Files.readAllBytes(wal.toPath()), ad, key), salt),
                            new Encryption.ExportedFile(encrypt(Files.readAllBytes(shm.toPath()), ad, key), salt),
                            out
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                activity.clearExport();
            });
        };
    }

    public static ActivityResultCallback<Uri> importCallback(LoginActivity activity) {
        return result -> {
            // TODO: Test this shit :(
            if (result == null) return;

            DocumentFile df = DocumentFile.fromSingleUri(activity, result);
            String filename = Objects.requireNonNull(Objects.requireNonNull(df).getName());
            int splitIndex = filename.lastIndexOf('.');
            String displayName = filename.substring(0, splitIndex);
            String ext = filename.substring(splitIndex);

            CustomDialog dialog = new CustomDialog(
                    R.layout.dialog_password_only,
                    activity.getString(R.string.open_file),
                    activity.getString(R.string.open), activity.getString(R.string.cancel),
                    (dialogInterface, i, dialogView) -> {
                        EncryptedPassword password = new EncryptedPassword(getFieldChars(R.id.password, dialogView));
                        diskIO().execute(() -> {
                            HashMap<String, Folder> folders = new HashMap<>();

                            FileDatabase fileDB = FileDatabase.singleton(activity);
                            FileEntity fileEntity = new FileEntity(displayName);
                            long fileID = fileDB.fileDAO().insert(fileEntity);
                            if (fileID == -1) return;
                            fileEntity.setId(fileID);
                            fileEntity.setDuplicateNumber(activity.getDuplicateFileCount(fileEntity));
                            fileEntity.setSize(getFolderCount(activity, fileEntity));
                            fileDB.fileDAO().update(fileEntity);
                            FolderDatabase folderDB = FolderDatabase.singleton(activity, fileEntity.getFilename());

                            switch (ext) {
                                case V2:
                                    upgradeV2(activity, result, folders, fileEntity, fileDB, filename, password);
                                    break;
                                case V3:
                                    upgradeV3(activity, result, folders, fileEntity, fileDB, filename, password);
                                    break;
                                default:
                                    File db = fileEntity.getFile(activity);
                                    File wal = new File(db.getPath() + WAL);
                                    File shm = new File(db.getPath() + SHM);
                                    byte[] ad = filename.getBytes(STRING_ENCODING);
                                    byte[] data;
                                    try (InputStream in = activity.getContentResolver().openInputStream(result)) {
                                        if (in == null) throw new IOException("in is null");
                                        Encryption.ExportedFile[] files = importTar(in);
                                        SecretKeySpec key = password.getKey(files[0].getSalt());
                                        data = decrypt(files[0].getIv(), files[0].getCiphertext(), ad, key);
                                        IOUtils.copy(new ByteArrayInputStream(data), Files.newOutputStream(db.toPath()));
                                        data = decrypt(files[1].getIv(), files[1].getCiphertext(), ad, key);
                                        IOUtils.copy(new ByteArrayInputStream(data), Files.newOutputStream(wal.toPath()));
                                        data = decrypt(files[2].getIv(), files[2].getCiphertext(), ad, key);
                                        IOUtils.copy(new ByteArrayInputStream(data), Files.newOutputStream(shm.toPath()));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    } catch (Exception e) {
                                        uiThread().execute(() -> toast(R.string.wrong_password, activity));
                                        fileDB.fileDAO().delete(fileEntity);
                                        return;
                                    }
                                    checkpoint(activity);
                                    updateFileList(activity, fileEntity, folders);
                                    return;
                            }
                            finishUpgrade(activity, fileEntity, folders, folderDB, password);
                        });
                    }, (dialogInterface, i, dialogView) -> dialogInterface.cancel()
            );
            dialog.show(activity.getSupportFragmentManager());
        };
    }

    public static void finishUpgrade(LoginActivity activity, FileEntity fileEntity, HashMap<String, Folder> folders, FolderDatabase folderDB, EncryptedPassword password) {
        writeFolders(activity, folders, folderDB, password);
        updateFileList(activity, fileEntity, folders);
    }

    public static void updateFileList(LoginActivity activity, FileEntity fileEntity, HashMap<String, Folder> folders) {
        fileEntity.setSize(folders.size());
        uiThread().execute(() -> activity.addFile(fileEntity));
    }

    /**
     * Takes a given folders hashmap and writes it to the prospective database
     *
     * @param folders  folders to write
     * @param folderDB database to write folder to
     * @param password EncryptedPassword to hash with
     */
    private static void writeFolders(LoginActivity activity, HashMap<String, Folder> folders, FolderDatabase folderDB, EncryptedPassword password) {
        int i = 0;
        activity.startImportBar(folders.size());
        for (String label : folders.keySet()) {
            Log.i("Writing", "File #" + ++i + " / " + folders.size());
            activity.incrementImportBar();
            // TODO: Progress bar
            updateOrInsertFolder(folders.get(label), null, folderDB, password);
        }
        activity.endImportBar();
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
            String filename, EncryptedPassword password
    ) {
        String data;
        try (InputStream in = context.getContentResolver().openInputStream(importUri)) {
            data = AES.parse(in, filename, password.getPassword());
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.error, context));
            fileDB.fileDAO().delete(fileEntity);
            return;
        }

        AES.modernize(data, folders);
    }

    /**
     * Upgrades a version 3 file to version 4
     * @param context Activity context
     * @param result URI from user choice of input file
     * @param folders V4 data structure
     * @param fileEntity File entity from fileDB, deleted if conversion fails
     * @param fileDB File database for handling stored files in the database
     * @param filename Filename for the input file. Used as associated data
     * @param password Encrypted password for the file.
     */
    private static void upgradeV3(
            Context context,
            Uri result,
            HashMap<String, Folder> folders,
            FileEntity fileEntity, FileDatabase fileDB,
            String filename, EncryptedPassword password
    ) {
        byte[] ad = filename.getBytes(STRING_ENCODING);

        Encryption.ExportedFile file;
        try (InputStream in = context.getContentResolver().openInputStream(result)) {
            if (in == null) throw new IOException("in is null");
            file = importFile(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] data;
        try {
            SecretKeySpec key = password.getKey(file.getSalt());
            data = decrypt(file.getIv(), file.getCiphertext(), ad, key);
        } catch (DestroyFailedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            uiThread().execute(() -> toast(R.string.wrong_password, context));
            return;
        }

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
    }
}
