package com.example.mitch.pmanager.adapters;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
import static com.example.mitch.pmanager.models.Entry.Types.BASIC;

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
import com.example.mitch.pmanager.models.Entry;
import com.example.mitch.pmanager.models.Folder;

import java.nio.CharBuffer;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.ViewHolder> {

    private static final String CLIPBOARD_LABEL = "PManagerCopy";

    private final Context context;
    private final Folder folder;

    private int shown = -1;

    public EntryAdapter(Context context, Folder folder) {
        this.context = context;
        this.folder = folder;
    }

    /**
     * Appends a new UserEntry to the list
     *
     * @param entry Entry to append
     */
    public void add(Entry entry) {
        folder.getEntries().add(entry);
        notifyItemInserted(folder.getEntries().size());
    }

    @NonNull
    @Override
    public EntryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_password_entry, parent, false);
        return new EntryAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryAdapter.ViewHolder holder, int position) {
        Entry entry = folder.getEntries().get(position);

        switch (entry.getType()) {
            case BASIC: {
                char[] label = entry.getLabel();
                char[] secret = entry.getSecret();
                holder.usernameView.setText(label, 0, label.length);
                holder.passwordView.setText(secret, 0, secret.length);
                holder.passwordView.setInputType(
                        TYPE_CLASS_TEXT | (holder.getAdapterPosition() == shown ? 0 : TYPE_TEXT_VARIATION_PASSWORD)
                );

                holder.showPasswordButton.setOnClickListener(view -> {
                    // TODO: toggle this image
                    int pos = holder.getAdapterPosition();
                    if (shown == pos) shown = -1;
                    else shown = pos;
                    notifyItemChanged(pos);
                });

                holder.copyButton.setOnClickListener(view -> {
                    ClipboardManager clipboard = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, CharBuffer.wrap(entry.getSecret())));
                });
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return folder.getEntries().size();
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
