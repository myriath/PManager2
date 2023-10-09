package com.example.mitch.pmanager.adapters;

import static com.example.mitch.pmanager.activities.LoginActivity.ROOT_DIR;
import static com.example.mitch.pmanager.activities.LoginActivity.toast;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_CODE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_FILE;
import static com.example.mitch.pmanager.util.Constants.CALLBACK_PWD;
import static com.example.mitch.pmanager.util.Constants.CallbackCodes.DELETE_FILE;
import static com.example.mitch.pmanager.util.Constants.CallbackCodes.EXPORT_FILE;
import static com.example.mitch.pmanager.util.Constants.CallbackCodes.LOAD_FILE;
import static com.example.mitch.pmanager.util.Constants.Version.V3;
import static com.example.mitch.pmanager.util.FileUtil.readFile;
import static com.example.mitch.pmanager.util.FileUtil.writeFile;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;
import static com.example.mitch.pmanager.util.WindowUtil.getFieldString;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.util.ByteCharStringUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {

    /**
     * Context for the adapter
     */
    private final Context context;
    /**
     * List of files for the adapter
     */
    private final ArrayList<File> entries = new ArrayList<>();
    /**
     * Callback listener for opening files
     */
    private final CallbackListener callback;

    /**
     * Holds the index of the last expanded file
     */
    private int expanded = -1;

    /**
     * Constructor
     *
     * @param context  Context for the adapter
     * @param root     Root directory for the files list
     * @param callback Callback for opening a file
     */
    public FilesAdapter(LoginActivity context, File root, CallbackListener callback) {
        this.context = context;
        entries.addAll(getFiles(root));
        this.callback = callback;
    }

    /**
     * Gets a list of files in the given directory
     *
     * @param root Directory to search
     * @return List of files in the directory
     */
    public static List<File> getFiles(File root) {
        return Stream.of(root.listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toList());
    }

    /**
     * Checks if the given filename is in the list (as any extension)
     *
     * @param filename Filename to check
     * @return True if this file exists already
     */
    public boolean fileExists(String filename) {
        for (int i = 0; i < entries.size(); i++) {
            File file = entries.get(i);
            if (ByteCharStringUtil.removeExtension(file.getName()).equals(filename)) return true;
        }
        return false;
    }

    /**
     * Appends a file to the end of the list
     *
     * @param file File to append
     */
    public void add(File file) {
        entries.add(file);
//        notifyItemChanged(entries.size()-1);
        notifyItemInserted(entries.size() - 1);
    }

    /**
     * Removes the given file from the list
     *
     * @param position position of the file to remove
     */
    private void remove(int position) {
        entries.remove(position);
        if (expanded == position) expanded = -1;
        notifyItemRemoved(position);
    }

    /**
     * Replaces the given element in the file list
     *
     * @param position Position of the file to replace
     * @param newFile  File to be replaced with
     */
    private void replace(int position, File newFile) {
        entries.set(position, newFile);
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
        File file = entries.get(position);

        holder.titleBar.setOnClickListener((view) -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable(CALLBACK_CODE, LOAD_FILE);
            bundle.putSerializable(CALLBACK_FILE, file);
            callback.callback(bundle);
        });

        holder.nameView.setText(file.getName());

        holder.optionsDrawer.setVisibility(holder.getAdapterPosition() == expanded ? View.VISIBLE : View.GONE);

        holder.moreButton.setOnClickListener((view) -> {
            int old = expanded;
            expanded = holder.getAdapterPosition();
            if (old == expanded) expanded = -1;
            else notifyItemChanged(old);
            notifyItemChanged(holder.getAdapterPosition());
        });

        holder.exportButton.setOnClickListener((view) -> {
            final View dialogLayout = View.inflate(context, R.layout.dialog_password_only, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setView(dialogLayout)
                    .setTitle(R.string.export_file)
                    .setPositiveButton(R.string.export, (dialogInterface, i) -> {
                        char[] pwd = getFieldChars(R.id.password, dialogLayout);

                        Bundle bundle = new Bundle();
                        bundle.putSerializable(CALLBACK_CODE, EXPORT_FILE);
                        bundle.putCharArray(CALLBACK_PWD, pwd);
                        bundle.putSerializable(CALLBACK_FILE, file);
                        callback.callback(bundle);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        });

        holder.renameButton.setOnClickListener((view) -> {
            final View dialogLayout = View.inflate(context, R.layout.dialog_rename, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setView(dialogLayout)
                    .setTitle(R.string.rename_file)
                    .setMessage(file.getName())
                    .setPositiveButton(R.string.rename, null)
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                String oldFilename = file.getName();
                String newFilename = getFieldString(R.id.filename, dialogLayout) + V3.ext;
                if (newFilename.length() < 5) {
                    ((TextInputLayout) dialogLayout.findViewById(R.id.filename)).setError(context.getString(R.string.cannot_be_empty));
                    return;
                }
                File newFile = new File(ROOT_DIR, newFilename);
                byte[] oldAD = oldFilename.getBytes(StandardCharsets.UTF_8);
                byte[] newAD = newFilename.getBytes(StandardCharsets.UTF_8);
                char[] pwd = getFieldChars(R.id.password, dialogLayout);
                try {
                    if (!writeFile(readFile(oldAD, pwd, file), newFile, newAD, pwd))
                        throw new Exception();
                    file.delete();
                    replace(holder.getAdapterPosition(), newFile);
                } catch (Exception e) {
                    toast(R.string.wrong_password, context);
                }
                dialog.dismiss();
            });

        });

        holder.changePasswordButton.setOnClickListener((view) -> {
            final View dialogLayout = View.inflate(context, R.layout.dialog_change_password, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setView(dialogLayout)
                    .setTitle(R.string.change_password)
                    .setMessage(file.getName())
                    .setPositiveButton(R.string.update, null)
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view1) -> {
                char[] pwd = getFieldChars(R.id.password, dialogLayout);
                char[] newPwd = getFieldChars(R.id.newPassword, dialogLayout);
                if (newPwd.length == 0) {
                    ((TextInputLayout) dialogLayout.findViewById(R.id.newPassword)).setError(context.getString(R.string.cannot_be_empty));
                    return;
                }
                byte[] ad = file.getName().getBytes(StandardCharsets.UTF_8);
                try {
                    if (!writeFile(readFile(ad, pwd, file), file, ad, newPwd))
                        throw new Exception();
                } catch (Exception e) {
                    toast(R.string.wrong_password, context);
                }
                dialog.dismiss();
            });
        });

        holder.deleteButton.setOnClickListener((view) -> {
            final View dialogLayout = View.inflate(context, R.layout.dialog_password_only, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setView(dialogLayout)
                    .setTitle(((LoginActivity) callback).getString(R.string.delete_file, file.getName()))
                    .setMessage(R.string.are_you_sure_this_cannot_be_undone)
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                        char[] pwd = getFieldChars(R.id.password, dialogLayout);
                        byte[] ad = file.getName().getBytes(StandardCharsets.UTF_8);
                        try {
                            readFile(ad, pwd, file);
                            file.delete();
                            remove(holder.getAdapterPosition());
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(CALLBACK_CODE, DELETE_FILE);
                            callback.callback(bundle);
                        } catch (Exception e) {
                            toast(R.string.error, context);
                        }
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
            builder.create();
            builder.show();
        });
    }

    /**
     * Resets the adapter and re fetches file list
     *
     * @param root Filesystem root
     */
    @SuppressLint("NotifyDataSetChanged")
    public void reset(File root) {
        entries.clear();
        entries.addAll(getFiles(root));
        notifyDataSetChanged();
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
        return entries.size();
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
