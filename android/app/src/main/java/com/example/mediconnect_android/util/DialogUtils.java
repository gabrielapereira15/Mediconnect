package com.example.mediconnect_android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.mediconnect_android.R;

public class DialogUtils {

    public static void showMessageDialog(Context context, String message) {
        if (context == null || message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Context and message cannot be null or empty");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_message, null);

        TextView messageTextView = dialogView.findViewById(R.id.dialog_message_text);
        messageTextView.setText(message);

        builder.setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
