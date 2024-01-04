package com.example.mitch.pmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Entry entry = entries.get(position);
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.adapter_edit_entry, parent, false);

            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Objects.requireNonNull(viewHolder.usernameField.getEditText()).setText(CharBuffer.wrap(entry.getLabel()));
        Objects.requireNonNull(viewHolder.passwordField.getEditText()).setText(CharBuffer.wrap(entry.getSecret()));
        viewHolder.id = position;

        viewHolder.deleteButton.setOnClickListener(view -> remove(viewHolder.id));

        return convertView;
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

    public static class ViewHolder {
        public int id;
        public TextInputLayout usernameField;
        public TextInputLayout passwordField;
        public ImageView deleteButton;

        public ViewHolder(View view) {
            usernameField = view.findViewById(R.id.usernameField);
            passwordField = view.findViewById(R.id.passwordField);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
}
