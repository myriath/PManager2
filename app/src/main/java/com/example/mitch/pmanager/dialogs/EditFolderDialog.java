package com.example.mitch.pmanager.dialogs;

import static com.example.mitch.pmanager.models.Entry.Types.BASIC;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.adapters.EntryEditAdapter;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.models.Entry;
import com.example.mitch.pmanager.models.Folder;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Arrays;

public class EditFolderDialog extends DialogFragment {

    public static final String DIALOG_TAG = "PManager.EditDomainDialog";

    private final ViewHolder viewHolder;
    private final Folder folder;
    private final CallbackListener callbackListener;
    private ArrayList<Entry> tempEntries;

    public EditFolderDialog(Folder folder, CallbackListener callbackListener) {
        viewHolder = new ViewHolder();
        this.folder = folder;
        this.callbackListener = callbackListener;
    }

    private static class ViewHolder {
        MaterialToolbar toolbar;
        ListView entriesList;
    }

    public void show(@NonNull FragmentManager manager) {
        super.show(manager, DIALOG_TAG);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;

        Window window = dialog.getWindow();
        if (window == null) return;

        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setLayout(width, height);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_FullScreenDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_domain, container, false);

        viewHolder.toolbar = view.findViewById(R.id.toolbar);
        viewHolder.entriesList = view.findViewById(R.id.entryList);

        tempEntries = cloneEntries(folder.getEntries());
        if (tempEntries.size() == 0) tempEntries.add(new Entry(BASIC));
        viewHolder.entriesList.setAdapter(new EntryEditAdapter(tempEntries, requireContext()));

        return view;
    }

    public ArrayList<Entry> cloneEntries(ArrayList<Entry> entries) {
        ArrayList<Entry> cloned = new ArrayList<>();
        for (Entry entry : entries) {
            char[] label = Arrays.copyOf(entry.getLabel(), entry.getLabel().length);
            char[] secret = Arrays.copyOf(entry.getSecret(), entry.getSecret().length);
            cloned.add(new Entry(label, secret));
        }
        return cloned;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewHolder.toolbar.setNavigationOnClickListener(view1 -> dismiss());
        viewHolder.toolbar.inflateMenu(R.menu.menu_edit_folder_dialog);
        viewHolder.toolbar.setTitle(getString(R.string.editing_folder, folder.getLabel()));
        viewHolder.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.newEntry) {
                ((EntryEditAdapter) viewHolder.entriesList.getAdapter()).add(new Entry(BASIC));
            } else if (id == R.id.save) {
                // TODO: Entry character arrays are being filled with 0s for no fucking reason wtf
                folder.setEntries(tempEntries);
//                tempEntries = null;
                callbackListener.callback(null);
                dismiss();
            } else {
                return false;
            }
            return true;
        });
    }

    public void destroyEntries(ArrayList<Entry> entries) {
        for (Entry entry : entries) {
            switch (entry.getType()) {
                case BASIC: {
                    Arrays.fill(entry.getLabel(), (char) 0);
                    Arrays.fill(entry.getSecret(), (char) 0);
                    break;
                }
            }
        }
        entries.clear();
    }
}
