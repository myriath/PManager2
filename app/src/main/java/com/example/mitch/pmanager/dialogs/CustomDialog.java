package com.example.mitch.pmanager.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomDialog extends DialogFragment {

    public static final String DIALOG_TAG = "PManager.CustomDialog";

    private View dialogView;
    private final int layoutResId;
    private final String title;
    private final String message;
    private final String positiveText;
    private final String negativeText;
    private final ButtonListener positiveListener;
    private final ButtonListener negativeListener;

    public CustomDialog(int layoutResId, String title, String positiveText, String negativeText, ButtonListener positiveListener, ButtonListener negativeListener) {
        this(layoutResId, title, null, positiveText, negativeText, positiveListener, negativeListener);
    }

    public CustomDialog(int layoutResId, String title, String message, String positiveText, String negativeText, ButtonListener positiveListener, ButtonListener negativeListener) {
        this.layoutResId = layoutResId;
        this.title = title;
        this.message = message;
        this.positiveText = positiveText;
        this.negativeText = negativeText;
        this.positiveListener = positiveListener;
        this.negativeListener = negativeListener;
    }

    public void show(@NonNull FragmentManager manager) {
        super.show(manager, DIALOG_TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialogView = onCreateView(LayoutInflater.from(requireContext()), null, savedInstanceState);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity())
                .setView(dialogView)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialogInterface, i) -> positiveListener.click(dialogInterface, i, dialogView))
                .setNegativeButton(negativeText, (dialogInterface, i) -> negativeListener.click(dialogInterface, i, dialogView));
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

    public interface ButtonListener {
        void click(DialogInterface dialogInterface, int i, View dialogView);
    }
}
