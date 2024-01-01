package com.example.mitch.pmanager.adapters;

import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.models.FileKey.generateSalt;
import static com.example.mitch.pmanager.util.AsyncUtil.diskIO;
import static com.example.mitch.pmanager.util.AsyncUtil.uiThread;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_CODE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_FILE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_FILEKEY;
import static com.example.mitch.pmanager.util.Constants.CallbackCodes.EXPORT_FILE;
import static com.example.mitch.pmanager.util.Constants.CallbackCodes.LOAD_FILE;
import static com.example.mitch.pmanager.util.HashUtil.SHA512;
import static com.example.mitch.pmanager.util.HashUtil.compareHashes;
import static com.example.mitch.pmanager.util.FilesUtil.getFolder;
import static com.example.mitch.pmanager.util.FilesUtil.updateOrInsertFolder;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldString;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.activities.LoginActivity;
import com.example.mitch.pmanager.database.dao.FolderDAO;
import com.example.mitch.pmanager.database.database.FileDatabase;
import com.example.mitch.pmanager.database.database.FolderDatabase;
import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.models.FileKey;
import com.example.mitch.pmanager.models.Folder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {

    /**
     * Host activity
     */
    private final LoginActivity activity;
    /**
     * List of files for the adapter
     */
    private final List<FileEntity> files;

    /**
     * Holds the index of the last expanded file
     */
    private int expanded = -1;

    /**
     * Constructor
     *
     * @param activity Context, callback for the adapter
     * @param files    List of files from the files database
     */
    public FilesAdapter(LoginActivity activity, List<FileEntity> files) {
        this.activity = activity;
        this.files = files;
    }

    /**
     * Checks if the given filename is in the list (as any extension)
     *
     * @param filename Filename to check
     * @return Largest duplicate value. 0 if no duplicates
     */
    public long getDuplicates(String filename) {
        long duplicates = 1;
        for (FileEntity entity : files) {
            if (entity.getDisplayName().equals(filename)) {
                duplicates++;
            }
        }
        return duplicates;
    }

    /**
     * Appends a file to the end of the list
     *
     * @param file File to append
     */
    public void add(FileEntity file) {
        files.add(file);
//        notifyItemChanged(entries.size()-1);
        notifyItemInserted(files.size() - 1);
    }

    /**
     * Removes the given file from the list
     *
     * @param position position of the file to remove
     */
    public void remove(int position) {
        files.remove(position);
        if (expanded == position) expanded = -1;
        notifyItemRemoved(position);
    }

    public void remove(FileEntity entity) {
        int position = files.indexOf(entity);
        remove(position);
    }

    /**
     * Replaces the given element in the file list
     *
     * @param position Position of the file to replace
     * @param newFile  File to be replaced with
     */
    private void replace(int position, FileEntity newFile) {
        files.set(position, newFile);
        if (expanded == position) expanded = -1;
        notifyItemChanged(position);
    }

    /**
     * Creates a ViewHolder's view
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return ViewHolder with an inflated view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_file_entry, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds a view to a given holder. Used when updating views
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileEntity file = files.get(position);

        holder.titleBar.setOnClickListener((view) -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable(CALLBACK_CODE, LOAD_FILE);
            bundle.putParcelable(CALLBACK_FILE, file);
            activity.callback(bundle);
        });

        String displayName = file.getDisplayName();
        long duplicate = file.getDuplicateNumber();
        if (duplicate > 1) {
            displayName += String.format(Locale.ENGLISH, " (%d)", duplicate);
        }
        // TODO: Delete this, not needed anymore with metadata
        if (file.getSize() == 0) {
            displayName += " (Empty)";
        }
        holder.nameView.setText(displayName);

        holder.optionsDrawer.setVisibility(holder.getAdapterPosition() == expanded ? View.VISIBLE : View.GONE);

        holder.moreButton.setOnClickListener((view) -> {
            int old = expanded;
            expanded = holder.getAdapterPosition();
            if (old == expanded) expanded = -1;
            else notifyItemChanged(old);
            notifyItemChanged(holder.getAdapterPosition());
        });

        holder.exportButton.setOnClickListener((view) -> {
            // TODO: UPdate with metadata
            final View dialogLayout = View.inflate(activity, R.layout.dialog_password_only, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                    .setView(dialogLayout)
                    .setTitle(R.string.export_file)
                    .setMessage(file.getDisplayName())
                    .setPositiveButton(R.string.export, (dialogInterface, i) -> {
                        Bundle bundle = new Bundle();
                        char[] password = getFieldChars(R.id.password, dialogLayout);
                        char[] pwclone = Arrays.copyOf(password, password.length);
                        FileKey keyToEncrypt = new FileKey(password, generateSalt());
                        FileKey metaKey = new FileKey(pwclone, file.getMetadata().getSalt());
                        if (!file.getMetadata().check(metaKey)) {
                            uiThread().execute(() -> toast(R.string.wrong_password, activity));
                            return;
                        }
                        bundle.putSerializable(CALLBACK_CODE, EXPORT_FILE);
                        bundle.putParcelable(CALLBACK_FILEKEY, keyToEncrypt);
                        bundle.putParcelable(CALLBACK_FILE, file);
                        activity.callback(bundle);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        });

        holder.renameButton.setOnClickListener((view) -> {
            final View dialogLayout = View.inflate(activity, R.layout.dialog_rename, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                    .setView(dialogLayout)
                    .setTitle(R.string.rename_file)
                    .setMessage(file.getDisplayName())
                    .setPositiveButton(R.string.rename, null)
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                String newFilename = getFieldString(R.id.filename, dialogLayout);
                file.setDuplicateNumber(activity.getDuplicateFileCount(file));
                file.setDisplayName(newFilename);
                diskIO().execute(() -> {
                    FileDatabase.singleton(activity).fileDAO().update(file);
                    uiThread().execute(() -> notifyItemChanged(holder.getAdapterPosition()));
                });
                dialog.dismiss();
            });

        });

        holder.changePasswordButton.setOnClickListener((view) -> {
            // TODO Update with metadata
            final View dialogLayout = View.inflate(activity, R.layout.dialog_change_password, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                    .setView(dialogLayout)
                    .setTitle(R.string.change_password)
                    .setMessage(file.getDisplayName())
                    .setPositiveButton(R.string.update, null)
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view1) -> {
                FileKey oldKey = new FileKey(getFieldChars(R.id.password, dialogLayout), file.getMetadata().getSalt());
                FileKey newKey = new FileKey(getFieldChars(R.id.newPassword, dialogLayout), file.getMetadata().getSalt());
                diskIO().execute(() -> {
                    FolderDatabase database = FolderDatabase.singleton(activity, file.getFilename());
                    FolderDAO dao = database.folderDAO();
                    List<FolderEntity> folders = dao.getFolders();
                    try {
                        for (FolderEntity entity : folders) {
                            updateOrInsertFolder(getFolder(entity, oldKey), entity, database, newKey);
                        }
                    } catch (Exception e) {
                        uiThread().execute(() -> toast(R.string.wrong_password, activity));
                    }
                });
                dialog.dismiss();
            });
        });

        holder.deleteButton.setOnClickListener((view) -> {
            // TODO Update with metadata
            final View dialogLayout = View.inflate(activity, R.layout.dialog_password_only, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                    .setView(dialogLayout)
                    .setTitle(activity.getString(R.string.delete_file, file.getDisplayName()))
                    .setMessage(R.string.are_you_sure_this_cannot_be_undone)
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                        FileKey key = new FileKey(getFieldChars(R.id.password, dialogLayout), file.getMetadata().getSalt());
                        diskIO().execute(() -> {
                            try {
                                FolderDatabase database = FolderDatabase.singleton(activity, file.getFilename());
                                FolderDAO dao = database.folderDAO();
                                for (FolderEntity entity : dao.getFolders()) {
                                    Folder folder = getFolder(entity, key);
                                    if (!compareHashes(entity.getLabel(), SHA512(folder.getLabel())))
                                        throw new Exception();
                                }
                                file.getFile(activity).delete();
                                FileDatabase fileDatabase = FileDatabase.singleton(activity);
                                fileDatabase.fileDAO().delete(file);
                                uiThread().execute(() -> activity.deleteFile(file));
                            } catch (Exception e) {
                                uiThread().execute(() -> toast(R.string.wrong_password, activity));
                            }
                        });
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        });
    }

    /**
     * Gets the item id
     *
     * @param i Adapter position to query
     * @return i
     */
    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * Gets the number of elements
     *
     * @return number of elements
     */
    @Override
    public int getItemCount() {
        return files.size();
    }

    /**
     * ViewHolder holds the view of a given entry in the recycler
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout titleBar;
        private final TextView nameView;
        private final RelativeLayout exportButton;
        private final RelativeLayout renameButton;
        private final RelativeLayout changePasswordButton;
        private final RelativeLayout deleteButton;
        private final LinearLayout optionsDrawer;
        private final ImageView moreButton;

        public ViewHolder(View view) {
            super(view);
            titleBar = view.findViewById(R.id.title);
            nameView = view.findViewById(R.id.name);
            exportButton = view.findViewById(R.id.export);
            renameButton = view.findViewById(R.id.rename);
            changePasswordButton = view.findViewById(R.id.changePassword);
            deleteButton = view.findViewById(R.id.delete);
            moreButton = view.findViewById(R.id.moreButton);
            optionsDrawer = view.findViewById(R.id.optionsDrawer);
        }
    }
}
