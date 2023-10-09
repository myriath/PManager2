package com.example.mitch.pmanager.dialogs;

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
import com.example.mitch.pmanager.adapters.UserEntryEditAdapter;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.UserEntry;
import com.google.android.material.appbar.MaterialToolbar;

public class EditDomainDialog extends DialogFragment {

    public static final String DIALOG_TAG = "PManager.EditDomainDialog";

    private final ViewHolder viewHolder;
    private final DomainEntry entry;
    private final CallbackListener callbackListener;
    private DomainEntry tempEntries;

    public EditDomainDialog(DomainEntry entry, CallbackListener callbackListener) {
        viewHolder = new ViewHolder();
        this.entry = entry;
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

        tempEntries = new DomainEntry(entry);
        if (tempEntries.getSize() == 0) tempEntries.add(new UserEntry());
        viewHolder.entriesList.setAdapter(new UserEntryEditAdapter(tempEntries, requireContext()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewHolder.toolbar.setNavigationOnClickListener(view1 -> dismiss());
        viewHolder.toolbar.inflateMenu(R.menu.menu_edit_domain_dialog);
        viewHolder.toolbar.setTitle(getString(R.string.editing_domain, entry.getDomain()));
        viewHolder.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.newEntry) {
                ((UserEntryEditAdapter) viewHolder.entriesList.getAdapter()).add(new UserEntry());
            } else if (id == R.id.save) {
                // TODO: save
                entry.destroy();
                entry.clone(tempEntries);
                tempEntries.destroy();
                tempEntries = null;
                callbackListener.callback(null);
                dismiss();
            } else {
                return false;
            }
            return true;
        });
    }

}
