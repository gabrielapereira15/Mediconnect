package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.FormItemBinding;
import com.example.mediconnect_android.fragment.CheckinFormFragment;
import com.example.mediconnect_android.fragment.PreAppointmentFormFragment;
import com.example.mediconnect_android.model.FormItem;
import com.example.mediconnect_android.model.PreAppointmentForm;
import com.example.mediconnect_android.util.FragmentUtils;

import java.util.List;

public class FormAdapter extends RecyclerView.Adapter<FormAdapter.ViewHolder> {

    private final Context context;
    private final List<FormItem> forms;
    FormItemBinding formItemBindingbinding;

    // Constructor
    public FormAdapter(Context context, List<FormItem> forms) {
        super();
        this.context = context;
        this.forms = forms;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        formItemBindingbinding = FormItemBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(formItemBindingbinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((ViewHolder) holder).bindView(forms.get(position));
    }

    @Override
    public int getItemCount() {
        return forms.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        FormItemBinding recyclerItemBinding;

        public ViewHolder(FormItemBinding recyclerItemBinding) {
            super(recyclerItemBinding.getRoot());
            this.recyclerItemBinding = recyclerItemBinding;
        }

        public void bindView(FormItem form) {
            recyclerItemBinding.formTitle.setText(form.getTitle());
            recyclerItemBinding.formStatus.setText(form.getStatus());
            recyclerItemBinding.formDescription.setText(form.getDescription());

            recyclerItemBinding.editFormButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (form.getTitle().equals("Check-In PreAppointmentForm")) {
                        CheckinFormFragment checkinFormFragment = new CheckinFormFragment();
                        FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, checkinFormFragment);
                    } else if (form.getTitle().equals("Pre-Appointment PreAppointmentForm")) {
                        PreAppointmentFormFragment preAppointmentFormFragment = new PreAppointmentFormFragment();
                        FragmentUtils.loadFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.flFragment, preAppointmentFormFragment);
                    }
                }
            });
        }
    }
}
