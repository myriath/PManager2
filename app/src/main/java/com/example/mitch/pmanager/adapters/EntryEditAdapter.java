package com.example.mitch.pmanager.adapters;

import static com.example.mitch.pmanager.util.WindowUtil.getFieldChars;

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
import com.example.mitch.pmanager.models.Entry;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Objects;

public class EntryEditAdapter extends BaseAdapter {

    private final ArrayList<Entry> entries;
    private final Context context;

    private static class ViewHolder {
        int id;
        TextInputLayout usernameField;
        TextInputLayout passwordField;
        ImageView deleteButton;
    }

    public EntryEditAdapter(ArrayList<Entry> entries, Context context) {
        this.entries = entries;
        this.context = context;
    }

    public void remove(int position) {
        entries.remove(position);
        notifyDataSetChanged();
    }

    public void add(Entry entry) {
        entries.add(entry);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int i) {
        return entries.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Entry entry = entries.get(position);
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.adapter_edit_entry, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.usernameField = convertView.findViewById(R.id.usernameField);
            viewHolder.passwordField = convertView.findViewById(R.id.passwordField);
            viewHolder.deleteButton = convertView.findViewById(R.id.deleteButton);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Objects.requireNonNull(viewHolder.usernameField.getEditText()).setText(CharBuffer.wrap(entry.getLabel()));
        Objects.requireNonNull(viewHolder.passwordField.getEditText()).setText(CharBuffer.wrap(entry.getSecret()));
        viewHolder.id = position;

        viewHolder.usernameField.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                final EditText usernameField = (EditText) view;
                try {
                    entries.get(viewHolder.id).setLabel(getFieldChars(usernameField, false));
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
        });

        viewHolder.passwordField.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                final EditText passwordField = (EditText) view;
                try {
                    entries.get(viewHolder.id).setSecret(getFieldChars(passwordField, false));
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
        });

        viewHolder.deleteButton.setOnClickListener(view -> remove(viewHolder.id));

        return convertView;
    }
}
