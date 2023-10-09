package com.example.mitch.pmanager.adapters;

import static com.example.mitch.pmanager.util.ByteCharStringUtil.getFieldChars;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.UserEntry;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.CharBuffer;
import java.util.Objects;

public class UserEntryEditAdapter extends BaseAdapter {

    private final DomainEntry domainEntry;
    private final Context context;

    private static class ViewHolder {
        int id;
        TextInputLayout usernameField;
        TextInputLayout passwordField;
        ImageView deleteButton;
    }

    public UserEntryEditAdapter(DomainEntry domainEntry, Context context) {
        this.domainEntry = domainEntry;
        this.context = context;
    }

    public void remove(int position) {
        domainEntry.remove(position);
        notifyDataSetChanged();
    }

    public void add(UserEntry entry) {
        domainEntry.add(entry);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return domainEntry.getSize();
    }

    @Override
    public Object getItem(int i) {
        return domainEntry.getEntry(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        UserEntry entry = domainEntry.getEntry(position);
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.adapter_edit_entry, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.usernameField = (TextInputLayout) convertView.findViewById(R.id.usernameField);
            viewHolder.passwordField = (TextInputLayout) convertView.findViewById(R.id.passwordField);
            viewHolder.deleteButton = (ImageView) convertView.findViewById(R.id.deleteButton);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Objects.requireNonNull(viewHolder.usernameField.getEditText()).setText(CharBuffer.wrap(entry.getUsername()));
        Objects.requireNonNull(viewHolder.passwordField.getEditText()).setText(CharBuffer.wrap(entry.getPassword()));
        viewHolder.id = position;

        viewHolder.usernameField.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                final EditText usernameField = (EditText) view;
                domainEntry.getEntry(viewHolder.id).setUsername(getFieldChars(usernameField));
            }
        });

        viewHolder.passwordField.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                final EditText passwordField = (EditText) view;
                domainEntry.getEntry(viewHolder.id).setPassword(getFieldChars(passwordField));
            }
        });

        viewHolder.deleteButton.setOnClickListener(view -> remove(viewHolder.id));

        return convertView;
    }
}
