package com.example.mitch.pmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mitch.pmanager.R;
import com.example.mitch.pmanager.activities.FileOpenActivity;
import com.example.mitch.pmanager.interfaces.CallbackListener;
import com.example.mitch.pmanager.models.Folder;

import java.util.ArrayList;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Folder> entries;
    private final CallbackListener callbackListener;
    private int expanded = -1;

    public FolderAdapter(Context context, ArrayList<Folder> entries, CallbackListener callbackListener) {
        this.context = context;
        this.entries = entries;
        this.callbackListener = callbackListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_domain_entry, parent, false);
        return new FolderAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Folder entry = entries.get(position);

        if (entries.get(holder.getAdapterPosition()).getEntries().size() == 0) {
            holder.moreButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.moreButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.passwordList.setVisibility(expanded == holder.getAdapterPosition() ? View.VISIBLE : View.GONE);
        holder.domainView.setText(entry.getLabel());

        holder.editButton.setOnClickListener(view -> FileOpenActivity.startEditDialog((FileOpenActivity) context, position, this));

        holder.deleteButton.setOnClickListener(view -> {
            entries.remove(position);
            notifyItemRemoved(position);
            callbackListener.callback(null);
        });

        holder.title.setOnClickListener(view -> {
            int old = expanded;
            expanded = holder.getAdapterPosition();
            if (old == expanded) expanded = -1;
            else notifyItemChanged(old);
            notifyItemChanged(holder.getAdapterPosition());
        });

        if (holder.passwordList.getVisibility() == View.VISIBLE) {
            EntryAdapter adapter = new EntryAdapter(context, entry);
            holder.passwordList.setItemAnimator(null); // TODO: create animator
            holder.passwordList.setLayoutManager(new LinearLayoutManager(context));
            holder.passwordList.setAdapter(adapter);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final RelativeLayout title;
        private final TextView domainView;
        private final ImageView editButton;
        private final ImageView moreButton;
        private final ImageView deleteButton;
        private final RecyclerView passwordList;

        public ViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.title);
            domainView = view.findViewById(R.id.domainView);
            editButton = view.findViewById(R.id.editButton);
            moreButton = view.findViewById(R.id.moreButton);
            deleteButton = view.findViewById(R.id.deleteButton);
            passwordList = view.findViewById(R.id.passwordList);
        }

    }

}
