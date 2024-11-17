package com.example.mediconnect_android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.DoctorListItemBinding;
import com.example.mediconnect_android.databinding.SpecialtyItemBinding;
import com.example.mediconnect_android.model.Specialty;

import java.util.List;

public class GridViewAdapter extends BaseAdapter {
    private Context context;
    private List<Specialty> specialties;
    SpecialtyItemBinding binding;

    public GridViewAdapter(Context context, List<Specialty> specialties) {
        this.context = context;
        this.specialties = specialties;
    }

    @Override
    public int getCount() {
        return specialties.size();
    }

    @Override
    public Object getItem(int position) {
        return specialties.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            binding = SpecialtyItemBinding.inflate(LayoutInflater.from(context), parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (SpecialtyItemBinding) convertView.getTag();
        }

        Specialty specialty = specialties.get(position);

        binding.specialtyIcon.setImageResource(specialty.getImageResId());
        binding.specialty.setText(specialty.getName());

        return convertView;
    }
}
