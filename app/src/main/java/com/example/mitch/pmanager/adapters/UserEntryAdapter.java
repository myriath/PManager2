package com.example.mitch.pmanager.adapters;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.UserEntry;

import java.nio.CharBuffer;

public class UserEntryAdapter extends RecyclerView.Adapter<UserEntryAdapter.ViewHolder> {

    private static final String CLIPBOARD_LABEL = "PManagerCopy";

    private final Context context;
    private final DomainEntry entries;

    public UserEntryAdapter(Context context, DomainEntry entries) {
        this.context = context;
        this.entries = entries;
    }

    /**
     * Appends a new UserEntry to the list
     *
     * @param entry Entry to append
     */
    public void add(UserEntry entry) {
        entries.add(entry);
        notifyItemInserted(entries.getEntries().size());
    }

    /**
     * Removes the given entry from the list
     *
     * @param position position of the entry to remove
     */
    public void remove(int position) {
        entries.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Replaces the given element in the entry list
     *
     * @param position Position of the entry to replace
     * @param newEntry entry to be replaced with
     */
    public void replace(int position, UserEntry newEntry) {
        entries.replace(position, newEntry);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public UserEntryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_password_entry, parent, false);
        return new UserEntryAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserEntryAdapter.ViewHolder holder, int position) {
        UserEntry entry = entries.getEntries().get(position);

        holder.usernameView.setText(entry.getUsername(), 0, entry.getUsername().length);
        holder.passwordView.setText(entry.getPassword(), 0, entry.getPassword().length);
        holder.passwordView.setInputType(
                TYPE_CLASS_TEXT | (entries.getShown(holder.getAdapterPosition()) ? 0 : TYPE_TEXT_VARIATION_PASSWORD)
        );

        holder.showPasswordButton.setOnClickListener(view -> {
            int pos = holder.getAdapterPosition();
            entries.setShown(pos, !entries.getShown(pos));
            notifyItemChanged(pos);
        });

        holder.copyButton.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, CharBuffer.wrap(entry.getPassword())));
        });
    }

    @Override
    public int getItemCount() {
        return entries.getEntries().size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView usernameView;
        private final TextView passwordView;
        private final ImageView showPasswordButton;
        private final ImageView copyButton;

        public ViewHolder(@NonNull View view) {
            super(view);
            usernameView = view.findViewById(R.id.usernameView);
            passwordView = view.findViewById(R.id.passwordView);
            showPasswordButton = view.findViewById(R.id.showPasswordButton);
            copyButton = view.findViewById(R.id.copyButton);
        }

    }

}
